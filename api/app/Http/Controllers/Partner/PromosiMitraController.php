<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use App\Services\Promosi;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * Layar "Promosikan toko" (dibuka dari M14). Hanya pemilik toko.
 *
 * Selama uji coba belum ada tagihan: iklan langsung tayang dengan payment_status simulated,
 * saldo M13 tidak dipotong. Harga tetap dihitung dan ditampilkan apa adanya.
 */
class PromosiMitraController extends Controller
{
    use AksesToko;

    /** GET /api/partner/stores/{store}/promotions */
    public function daftar(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);

        $promosi = DB::table('store_promotions')
            ->where('store_id', $store)
            ->orderByDesc('ends_on')->orderByDesc('id')
            ->limit(20)
            ->get();

        return response()->json([
            'packages' => $this->paket(),
            'billing' => [
                'enabled' => false,
                'reason' => 'Selama uji coba, promosi belum ditagih dan saldo tidak dipotong.',
            ],
            'data' => $promosi->map(fn (object $p) => $this->bentuk($p))->values(),
        ]);
    }

    /**
     * POST /api/partner/stores/{store}/promotions
     * Kalau paket yang sama masih tayang, promosi baru disambung setelah tanggal berakhirnya.
     */
    public function buat(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);

        $f = $request->validate([
            'package' => ['required', Rule::in(array_keys(Promosi::TARIF))],
            'days' => ['required', 'integer', 'min:1', 'max:'.Promosi::MAKS_HARI],
            'headline' => ['nullable', 'string', 'min:3', 'max:80'],
        ]);

        $tarif = Promosi::TARIF[$f['package']];
        $terakhir = Promosi::berakhirTerakhir($store, $f['package']);
        $mulai = $terakhir === null ? today() : Carbon::parse($terakhir)->addDay();
        $akhir = $mulai->copy()->addDays($f['days'] - 1);

        $id = DB::transaction(function () use ($request, $store, $f, $tarif, $mulai, $akhir) {
            $id = DB::table('store_promotions')->insertGetId([
                'store_id' => $store,
                'package' => $f['package'],
                'headline' => $f['package'] === Promosi::BANNER ? ($f['headline'] ?? null) : null,
                'starts_on' => $mulai->toDateString(),
                'ends_on' => $akhir->toDateString(),
                'days' => $f['days'],
                'price_per_day_rupiah' => $tarif,
                'total_rupiah' => $tarif * $f['days'],
                'payment_status' => 'simulated',
                'created_by_user_id' => $request->user()->id,
                'created_at' => now(),
                'updated_at' => now(),
            ]);

            DB::table('audit_logs')->insert([
                'user_id' => $request->user()->id, 'store_id' => $store, 'action' => 'promotion.create',
                'subject_type' => 'store_promotion', 'subject_id' => $id,
                'meta' => json_encode(['package' => $f['package'], 'days' => $f['days']]),
                'ip' => $request->ip(), 'created_at' => now(), 'updated_at' => now(),
            ]);

            return $id;
        });

        return response()->json(['data' => $this->bentuk(DB::table('store_promotions')->find($id))], 201);
    }

    private function paket(): array
    {
        return [
            [
                'code' => Promosi::PRIORITAS,
                'name' => 'Prioritas pencarian',
                'description' => 'Jualanmu tampil paling atas di daftar Beranda dan kategori, dengan label "Iklan".',
                'price_per_day_rupiah' => Promosi::TARIF[Promosi::PRIORITAS],
            ],
            [
                'code' => Promosi::BANNER,
                'name' => 'Banner Beranda',
                'description' => 'Tokomu tampil di banner geser paling atas Beranda pembeli.',
                'price_per_day_rupiah' => Promosi::TARIF[Promosi::BANNER],
            ],
        ];
    }

    private function bentuk(object $p): array
    {
        $hariIni = today()->toDateString();

        return [
            'id' => $p->id,
            'package' => $p->package,
            'headline' => $p->headline,
            'starts_on' => $p->starts_on,
            'ends_on' => $p->ends_on,
            'days' => (int) $p->days,
            'price_per_day_rupiah' => (int) $p->price_per_day_rupiah,
            'total_rupiah' => (int) $p->total_rupiah,
            'payment_status' => $p->payment_status,
            'status' => $p->ends_on < $hariIni ? 'ended' : ($p->starts_on > $hariIni ? 'scheduled' : 'active'),
        ];
    }
}
