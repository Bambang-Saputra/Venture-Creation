<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use App\Services\RingkasanSisa;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

/**
 * Catat sisa dan laporan mingguan (F-12, F-13): M06 hitung per item,
 * M19 timbang, M07 laporan minggu ini.
 *
 * Nilai sisa memakai HPP (products.cost_rupiah) dan disalin ke
 * waste_log_items saat dicatat, supaya laporan lama tidak berubah saat
 * HPP naik (kamus data). Produk tanpa HPP memakai harga jual.
 */
class CatatSisaController extends Controller
{
    use AksesToko;

    /** Catatan boleh diubah sampai akhir hari berikutnya, lalu terkunci. */
    private const HARI_BOLEH_UBAH = 1;

    public function __construct(private readonly RingkasanSisa $ringkasan) {}

    /** GET /api/partner/stores/{store}/waste-logs?date=Y-m-d */
    public function tampil(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store, pemilikSaja: false);
        $tanggal = $this->tanggal($request);

        $log = DB::table('waste_logs')->where('store_id', $store)->where('log_date', $tanggal->toDateString())->first();
        $semua = $log === null ? collect() : DB::table('waste_log_items')->where('waste_log_id', $log->id)->orderBy('id')->get();
        $item = $semua->whereNotNull('product_id')->keyBy('product_id');

        // Semua produk aktif ditampilkan, termasuk yang belum tercatat (qty 0), supaya M06 cukup ketuk +.
        $produk = DB::table('products')->where('store_id', $store)->where('is_active', true)->orderBy('name')
            ->get(['id', 'name', 'unit', 'price_rupiah', 'cost_rupiah', 'weight_gram'])
            ->map(fn ($p) => [
                'product_id' => $p->id,
                'name' => $p->name,
                'unit' => $p->unit,
                'price_rupiah' => $p->price_rupiah,
                'unit_value_rupiah' => $this->nilaiSatuan($p),
                'qty' => $item->get($p->id)?->qty ?? 0,
                'weight_gram' => $item->get($p->id)?->weight_gram,
                'disposition' => $item->get($p->id)?->disposition ?? 'discarded',
            ]);

        $pekanLalu = DB::table('waste_logs')->where('store_id', $store)
            ->where('log_date', $tanggal->copy()->subWeek()->toDateString())->value('total_value_rupiah');

        return response()->json(['data' => [
            'log_date' => $tanggal->toDateString(),
            'is_recorded' => $log !== null,
            'is_locked' => $this->terkunci($tanggal),
            'method' => $log?->method ?? 'per_item',
            'note' => $log?->note,
            'total_value_rupiah' => $log?->total_value_rupiah ?? 0,
            'total_weight_gram' => $log?->total_weight_gram ?? 0,
            'total_items' => $log?->total_items ?? 0,
            // "naik 12% dari Kamis lalu" di M06. null kalau hari yang sama pekan lalu tidak dicatat.
            'change_vs_last_week_percent' => $log === null ? null : $this->persen($log->total_value_rupiah, $pekanLalu),
            'products' => $produk,
            'other_items' => $semua->whereNull('product_id')->values()
                ->map(fn ($i) => ['label' => $i->label, 'qty' => $i->qty, 'weight_gram' => $i->weight_gram, 'value_rupiah' => $i->value_rupiah]),
        ]]);
    }

    /**
     * POST /api/partner/stores/{store}/waste-logs
     * Satu catatan per toko per hari: mengirim lagi mengganti isi hari itu.
     */
    public function simpan(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store, pemilikSaja: false);
        $timbang = $request->input('method') === 'weight';

        $data = $request->validate([
            'log_date' => ['sometimes', 'date_format:Y-m-d'],
            'method' => ['required', Rule::in(['per_item', 'weight'])],
            'note' => ['nullable', 'string', 'max:500'],
            'items' => ['present', 'array', 'max:100'],
            'items.*.product_id' => ['nullable', 'integer', 'distinct', Rule::exists('products', 'id')->where('store_id', $store)],
            'items.*.label' => ['required_without:items.*.product_id', 'nullable', 'string', 'max:140'],
            'items.*.qty' => [$timbang ? 'nullable' : 'required', 'integer', 'min:0', 'max:10000'],
            'items.*.weight_gram' => [$timbang ? 'required' : 'nullable', 'integer', 'min:0', 'max:1000000'],
            'items.*.unit_value_rupiah' => ['nullable', 'integer', 'min:0', 'max:5000000'],
            'items.*.disposition' => ['sometimes', Rule::in(['discarded', 'sold_surplus', 'staff_meal', 'donated'])],
        ]);

        $tanggal = Carbon::parse($data['log_date'] ?? now()->toDateString());
        if ($tanggal->isFuture() && ! $tanggal->isToday()) {
            throw ValidationException::withMessages(['log_date' => 'Sisa belum bisa dicatat untuk hari yang belum terjadi.']);
        }
        abort_if($this->terkunci($tanggal), 409, 'Catatan tanggal ini sudah terkunci. Hubungi pemilik toko bila perlu dikoreksi.');

        $produk = DB::table('products')->whereIn('id', array_filter(array_column($data['items'], 'product_id')))->get()->keyBy('id');
        $baris = collect($data['items'])
            // Baris kosong dari layar (qty 0, berat 0) tidak disimpan.
            ->filter(fn (array $i) => ($i['qty'] ?? 0) > 0 || ($i['weight_gram'] ?? 0) > 0)
            ->map(function (array $i) use ($produk, $timbang) {
                $p = isset($i['product_id']) ? $produk[$i['product_id']] : null;
                $satuan = $i['unit_value_rupiah'] ?? ($p === null ? 0 : $this->nilaiSatuan($p));
                $berat = $i['weight_gram'] ?? ($p?->weight_gram && isset($i['qty']) ? $p->weight_gram * $i['qty'] : null);

                // Timbang: nilai dihitung dari berat per satuan produk bila diketahui.
                $nilai = $timbang
                    ? ($p?->weight_gram ? (int) round($satuan * $i['weight_gram'] / $p->weight_gram) : 0)
                    : $satuan * $i['qty'];

                return [
                    'product_id' => $p?->id,
                    'label' => $p?->name ?? $i['label'],
                    'qty' => $i['qty'] ?? null,
                    'weight_gram' => $berat,
                    'unit_value_rupiah' => $satuan,
                    'value_rupiah' => $nilai,
                    'disposition' => $i['disposition'] ?? 'discarded',
                ];
            })->values();

        // Yang dihitung sebagai pemborosan hanya discarded (kamus data).
        $terbuang = $baris->where('disposition', 'discarded');

        DB::transaction(function () use ($store, $tanggal, $data, $baris, $terbuang, $request) {
            DB::table('waste_logs')->upsert([[
                'store_id' => $store,
                'log_date' => $tanggal->toDateString(),
                'method' => $data['method'],
                'total_value_rupiah' => $terbuang->sum('value_rupiah'),
                'total_weight_gram' => $terbuang->sum('weight_gram'),
                'total_items' => $terbuang->sum('qty'),
                'note' => $data['note'] ?? null,
                'recorded_by_user_id' => $request->user()->id,
                'created_at' => now(),
                'updated_at' => now(),
            ]], ['store_id', 'log_date'], ['method', 'total_value_rupiah', 'total_weight_gram', 'total_items', 'note', 'recorded_by_user_id', 'updated_at']);

            $id = DB::table('waste_logs')->where('store_id', $store)->where('log_date', $tanggal->toDateString())->value('id');
            DB::table('waste_log_items')->where('waste_log_id', $id)->delete();
            DB::table('waste_log_items')->insert($baris->map(fn (array $b) => [
                ...$b, 'waste_log_id' => $id, 'created_at' => now(), 'updated_at' => now(),
            ])->all());

            DB::table('audit_logs')->insert([
                'user_id' => $request->user()->id, 'store_id' => $store, 'action' => 'waste_log.save',
                'subject_type' => 'waste_log', 'subject_id' => $id,
                'meta' => json_encode(['log_date' => $tanggal->toDateString(), 'total_value_rupiah' => $terbuang->sum('value_rupiah')]),
                'ip' => $request->ip(), 'created_at' => now(), 'updated_at' => now(),
            ]);
        });

        $request->query->set('date', $tanggal->toDateString());

        return $this->tampil($request, $store);
    }

    /** GET /api/partner/stores/{store}/reports/weekly?week_start=Y-m-d (Senin) */
    public function laporanMingguan(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);
        $f = $request->validate(['week_start' => ['sometimes', 'date_format:Y-m-d']]);

        $mulai = Carbon::parse($f['week_start'] ?? now()->toDateString())->startOfWeek(Carbon::MONDAY);
        $akhir = $mulai->copy()->endOfWeek(Carbon::SUNDAY);
        $ini = $this->ringkasan->rentang($store, $mulai, $akhir);
        $lalu = $this->ringkasan->rentang($store, $mulai->copy()->subWeek(), $akhir->copy()->subWeek());

        $teratas = DB::table('waste_log_items')
            ->join('waste_logs', 'waste_logs.id', '=', 'waste_log_items.waste_log_id')
            ->where('waste_logs.store_id', $store)
            ->whereBetween('waste_logs.log_date', [$mulai->toDateString(), $akhir->toDateString()])
            ->where('waste_log_items.disposition', 'discarded')
            ->groupBy('waste_log_items.product_id', 'waste_log_items.label')
            ->selectRaw('waste_log_items.product_id, waste_log_items.label, SUM(COALESCE(waste_log_items.qty, 0)) AS qty, SUM(waste_log_items.weight_gram) AS weight_gram, SUM(waste_log_items.value_rupiah) AS value_rupiah')
            ->orderByDesc('value_rupiah')->limit(5)->get()
            ->map(fn ($t) => ['product_id' => $t->product_id, 'label' => $t->label, 'qty' => (int) $t->qty,
                'weight_gram' => $t->weight_gram === null ? null : (int) $t->weight_gram, 'value_rupiah' => (int) $t->value_rupiah]);

        $laporan = [
            'week_start' => $mulai->toDateString(),
            'week_end' => $akhir->toDateString(),
            ...$ini,
            // "Nilai yang tidak terjual" = terbuang + terselamatkan lewat tas.
            'unsold_value_rupiah' => $ini['wasted_value_rupiah'] + $ini['rescued_value_rupiah'],
            'wasted_change_percent' => $this->persen($ini['wasted_value_rupiah'], $lalu['logged_days'] > 0 ? $lalu['wasted_value_rupiah'] : null),
            'top_wasted_products' => $teratas,
            'daily' => $this->ringkasan->harian($store, $mulai),
        ];

        // Disimpan sebagai cache untuk dashboard dan laporan bisnis; sumber kebenaran tetap tabel mentah.
        DB::table('weekly_reports')->upsert([[
            'store_id' => $store, 'week_start' => $laporan['week_start'], 'week_end' => $laporan['week_end'],
            'wasted_value_rupiah' => $ini['wasted_value_rupiah'], 'wasted_weight_gram' => $ini['wasted_weight_gram'],
            'rescued_value_rupiah' => $ini['rescued_value_rupiah'], 'rescued_weight_gram' => 0,
            'orders_count' => $ini['orders_count'], 'logged_days' => $ini['logged_days'],
            'top_wasted_products' => json_encode($teratas), 'generated_at' => now(), 'created_at' => now(), 'updated_at' => now(),
        ]], ['store_id', 'week_start'], ['week_end', 'wasted_value_rupiah', 'wasted_weight_gram', 'rescued_value_rupiah',
            'orders_count', 'logged_days', 'top_wasted_products', 'generated_at', 'updated_at']);

        return response()->json(['data' => $laporan]);
    }

    private function nilaiSatuan(object $produk): int
    {
        return (int) ($produk->cost_rupiah ?? $produk->price_rupiah);
    }

    private function persen(int $sekarang, ?int $sebelumnya): ?int
    {
        return $sebelumnya === null || $sebelumnya === 0 ? null : (int) round(($sekarang - $sebelumnya) / $sebelumnya * 100);
    }

    private function terkunci(Carbon $tanggal): bool
    {
        return $tanggal->copy()->addDays(self::HARI_BOLEH_UBAH)->endOfDay()->isPast();
    }

    private function tanggal(Request $request): Carbon
    {
        $f = $request->validate(['date' => ['sometimes', 'date_format:Y-m-d']]);

        return Carbon::parse($f['date'] ?? now()->toDateString());
    }
}
