<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use App\Services\RingkasanSisa;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * M05 Dashboard mitra (F-24) dan M08 Saran produksi (F-15).
 */
class DasborMitraController extends Controller
{
    use AksesToko;

    private const NAMA_HARI = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];

    // Hari yang sama dalam 4 minggu terakhir.
    private const MINGGU_SAMPEL = 4;

    // Kolom production_suggestions.sample_days: di bawah tiga hari, saran tidak ditampilkan.
    private const SAMPEL_MINIMUM = 3;

    // Sisa yang tetap ada masih bisa dijual lewat tas, jadi saran tidak memangkas seluruh sisa.
    private const PORSI_DIPANGKAS = 0.75;

    public function __construct(private readonly RingkasanSisa $ringkasan) {}

    /** GET /api/partner/stores/{store}/summary */
    public function ringkasan(Request $request, int $store): JsonResponse
    {
        $toko = $this->tokoMilik($request, $store, pemilikSaja: false);
        $senin = now()->startOfWeek(Carbon::MONDAY);
        $minggu = $this->ringkasan->rentang($store, $senin, now());
        $jam = DB::table('store_hours')->where('store_id', $store)->where('day_of_week', now()->dayOfWeek)->first();
        $tutup = $jam && ! $jam->is_closed && $jam->close_time ? now()->setTimeFromTimeString($jam->close_time) : null;
        $buka = $jam && ! $jam->is_closed && $jam->open_time ? now()->setTimeFromTimeString($jam->open_time) : null;

        $tujuhHari = DB::table('waste_logs')->where('store_id', $store)
            ->whereBetween('log_date', [now()->subDays(6)->toDateString(), now()->toDateString()])
            ->selectRaw('COUNT(*) d, COALESCE(SUM(total_weight_gram), 0) w')->first();

        $harian = $this->ringkasan->harian($store, $senin);
        $puncak = collect($harian)->whereNotNull('wasted_value_rupiah')->sortByDesc('wasted_value_rupiah')->first();

        return response()->json(['data' => [
            'store' => ['id' => $toko->id, 'name' => $toko->name],
            'is_open' => ! $toko->is_temporarily_closed && $buka && $tutup && now()->between($buka, $tutup),
            'closes_at' => $tutup?->format('H:i'),
            // "Toko tutup 2 jam lagi" di pengingat catat sisa.
            'minutes_until_close' => $tutup && $tutup->isFuture() ? (int) now()->diffInMinutes($tutup) : null,
            'is_today_logged' => DB::table('waste_logs')->where('store_id', $store)->where('log_date', now()->toDateString())->exists(),
            // "Kerugian yang tertahan minggu ini", dari pesanan yang sudah diambil.
            'rescued_value_this_week_rupiah' => $minggu['rescued_value_rupiah'],
            'items_sold_this_week' => $minggu['items_sold'],
            'avg_daily_waste_gram' => $tujuhHari->d > 0 ? (int) round($tujuhHari->w / $tujuhHari->d) : null,
            'unsold_bags_last_7_days' => (int) DB::table('listings')->where('store_id', $store)->where('type', 'surprise_bag')
                ->where('status', '!=', 'draft')->where('pickup_end', '<', now())
                ->where('pickup_date', '>=', now()->subDays(6)->toDateString())
                ->sum(DB::raw('qty_total - qty_sold')),
            'pending_orders_today' => DB::table('orders')->where('store_id', $store)->where('status', 'pending_pickup')
                ->whereDate('pickup_start', now()->toDateString())->count(),
            'daily' => $harian,
            'peak_day' => $puncak === null ? null : self::NAMA_HARI[Carbon::parse($puncak['date'])->dayOfWeek],
        ]]);
    }

    /** GET /api/partner/stores/{store}/suggestions?date=Y-m-d (bawaan: besok) */
    public function saran(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);
        $f = $request->validate(['date' => ['sometimes', 'date_format:Y-m-d', 'after:today']]);
        $untuk = Carbon::parse($f['date'] ?? now()->addDay()->toDateString());

        $tanggalSampel = collect(range(1, self::MINGGU_SAMPEL))->map(fn (int $m) => $untuk->copy()->subWeeks($m)->toDateString());
        $logSampel = DB::table('waste_logs')->where('store_id', $store)->whereIn('log_date', $tanggalSampel)->pluck('id');
        $jumlahSampel = $logSampel->count();

        $sisaPerProduk = DB::table('waste_log_items')->whereIn('waste_log_id', $logSampel)
            ->where('disposition', 'discarded')->whereNotNull('product_id')
            ->groupBy('product_id')->selectRaw('product_id, SUM(COALESCE(qty, 0)) q')->pluck('q', 'product_id');

        $produk = DB::table('products')->where('store_id', $store)->where('is_active', true)
            ->whereNotNull('daily_production_qty')->orderBy('name')->get();

        $saran = collect();
        if ($jumlahSampel >= self::SAMPEL_MINIMUM) {
            foreach ($produk as $p) {
                // Hari tanpa sisa untuk produk ini ikut dihitung sebagai 0.
                $rata = ($sisaPerProduk[$p->id] ?? 0) / $jumlahSampel;
                $pangkas = min((int) floor($rata * self::PORSI_DIPANGKAS), $p->daily_production_qty - 1);
                if ($pangkas < 1) {
                    continue;
                }

                $hemat = $pangkas * (int) ($p->cost_rupiah ?? $p->price_rupiah);
                DB::table('production_suggestions')->upsert([[
                    'store_id' => $store, 'product_id' => $p->id, 'suggested_for_date' => $untuk->toDateString(),
                    'current_avg_production' => $p->daily_production_qty,
                    'suggested_production' => $p->daily_production_qty - $pangkas,
                    'avg_waste_qty' => round($rata, 2),
                    'estimated_saving_rupiah' => $hemat,
                    'rationale_text' => sprintf('Rata-rata sisa %s setiap %s dari %d catatan terakhir.',
                        rtrim(rtrim(number_format($rata, 1, ',', ''), '0'), ','), self::NAMA_HARI[$untuk->dayOfWeek], $jumlahSampel),
                    'sample_days' => $jumlahSampel,
                    'generated_at' => now(), 'created_at' => now(), 'updated_at' => now(),
                ]], ['store_id', 'product_id', 'suggested_for_date'], ['current_avg_production', 'suggested_production',
                    'avg_waste_qty', 'estimated_saving_rupiah', 'rationale_text', 'sample_days', 'generated_at', 'updated_at']);
                $saran->push($p->id);
            }
        }

        // Status new/accepted/dismissed dipertahankan saat saran dihitung ulang.
        $baris = DB::table('production_suggestions')
            ->join('products', 'products.id', '=', 'production_suggestions.product_id')
            ->where('production_suggestions.store_id', $store)
            ->where('suggested_for_date', $untuk->toDateString())
            ->whereIn('product_id', $saran)
            ->orderByDesc('estimated_saving_rupiah')
            ->get(['production_suggestions.*', 'products.name', 'products.unit'])
            ->map(fn ($s) => [
                'id' => $s->id, 'product_id' => $s->product_id, 'name' => $s->name, 'unit' => $s->unit,
                'current_production' => $s->current_avg_production, 'suggested_production' => $s->suggested_production,
                'reduce_by' => $s->current_avg_production - $s->suggested_production,
                'avg_waste_qty' => (float) $s->avg_waste_qty,
                'estimated_saving_per_week_rupiah' => $s->estimated_saving_rupiah,
                'rationale' => $s->rationale_text, 'status' => $s->status,
            ]);

        return response()->json(['data' => [
            'suggested_for_date' => $untuk->toDateString(),
            'weekday' => self::NAMA_HARI[$untuk->dayOfWeek],
            'sample_days' => $jumlahSampel,
            // false: catatan belum cukup. Android menampilkan ajakan mencatat, bukan daftar kosong.
            'has_enough_data' => $jumlahSampel >= self::SAMPEL_MINIMUM,
            'products_missing_production_qty' => DB::table('products')->where('store_id', $store)->where('is_active', true)
                ->whereNull('daily_production_qty')->count(),
            'total_saving_per_week_rupiah' => $baris->where('status', '!=', 'dismissed')->sum('estimated_saving_per_week_rupiah'),
            'items' => $baris->values(),
        ]]);
    }

    /** POST /api/partner/stores/{store}/suggestions/{suggestion}/{accept|dismiss} */
    public function tanggapi(Request $request, int $store, int $suggestion, string $aksi): JsonResponse
    {
        $this->tokoMilik($request, $store);

        $ubah = DB::table('production_suggestions')->where('id', $suggestion)->where('store_id', $store)
            ->update(['status' => $aksi === 'accept' ? 'accepted' : 'dismissed', 'responded_at' => now(), 'updated_at' => now()]);
        abort_if($ubah === 0 && ! DB::table('production_suggestions')->where('id', $suggestion)->where('store_id', $store)->exists(), 404, 'Saran tidak ditemukan.');

        return response()->json(['data' => ['id' => $suggestion, 'status' => $aksi === 'accept' ? 'accepted' : 'dismissed']]);
    }

    /** PATCH /api/partner/stores/{store}/products/{product} — produksi harian untuk M08 */
    public function ubahProduksi(Request $request, int $store, int $product): JsonResponse
    {
        $this->tokoMilik($request, $store);
        $data = $request->validate(['daily_production_qty' => ['present', 'nullable', 'integer', 'min:1', 'max:10000']]);

        $ada = DB::table('products')->where('id', $product)->where('store_id', $store)->exists();
        abort_if(! $ada, 404, 'Produk tidak ditemukan.');
        DB::table('products')->where('id', $product)->update([...$data, 'updated_at' => now()]);

        return response()->json(['data' => DB::table('products')->where('id', $product)
            ->first(['id', 'name', 'unit', 'price_rupiah', 'cost_rupiah', 'daily_production_qty'])]);
    }
}
