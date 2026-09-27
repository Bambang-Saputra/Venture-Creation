<?php

namespace App\Http\Controllers;

use App\Services\LayananPesanan;
use App\Services\Notifikasi;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * Pesanan dari sisi konsumen: K12/K13 Ringkasan, K14 Kode pickup,
 * K15 Pesanan saya (F-06, F-07, F-08). Bayar di tempat selama pilot
 * (ADR-0004), jadi tidak ada langkah pembayaran di sini.
 */
class PesananController extends Controller
{
    public function __construct(
        private readonly LayananPesanan $pesanan,
        private readonly Notifikasi $notifikasi,
    ) {}

    /** POST /api/orders/preview */
    public function pratinjau(Request $request): JsonResponse
    {
        $this->khususKonsumen($request);
        $h = $this->pesanan->hitung($request->user(), $this->validasiItem($request)['items']);

        return response()->json(['data' => [
            ...$h,
            'pickup_start' => $h['pickup_start']->toIso8601String(),
            'pickup_end' => $h['pickup_end']->toIso8601String(),
        ]]);
    }

    /** POST /api/orders */
    public function buat(Request $request): JsonResponse
    {
        $this->khususKonsumen($request);
        $data = $this->validasiItem($request, [
            'note' => ['nullable', 'string', 'max:300'],
            'payment_method' => ['sometimes', Rule::in(['cash', 'qris_static'])],
        ]);

        $id = $this->pesanan->buat($request->user(), $data);
        $this->notifikasi->pesananBaru($id);

        return response()->json(['data' => $this->detailPesanan($id, $request->user()->id)], 201);
    }

    /** GET /api/orders?status=active|history (K15) */
    public function daftar(Request $request): JsonResponse
    {
        $f = $request->validate(['status' => ['sometimes', Rule::in(['active', 'history'])]]);

        $halaman = DB::table('orders')
            ->join('stores', 'stores.id', '=', 'orders.store_id')
            ->where('orders.user_id', $request->user()->id)
            ->when(($f['status'] ?? null) === 'active', fn ($q) => $q->where('orders.status', 'pending_pickup'))
            ->when(($f['status'] ?? null) === 'history', fn ($q) => $q->where('orders.status', '!=', 'pending_pickup'))
            ->orderByDesc('orders.placed_at')->orderByDesc('orders.id')
            ->select('orders.id', 'orders.code', 'orders.status', 'orders.total_rupiah', 'orders.pickup_start',
                'orders.pickup_end', 'orders.placed_at', 'stores.name as store_name')
            ->selectSub(fn ($q) => $q->from('order_items')->whereColumn('order_id', 'orders.id')->selectRaw('COALESCE(SUM(qty), 0)'), 'item_count')
            ->paginate(20);

        $halaman->through(fn (object $o) => [
            'id' => $o->id,
            'code' => $o->code,
            'status' => $o->status,
            'store_name' => $o->store_name,
            'item_count' => (int) $o->item_count,
            'total_rupiah' => $o->total_rupiah,
            'pickup_start' => Carbon::parse($o->pickup_start)->toIso8601String(),
            'pickup_end' => Carbon::parse($o->pickup_end)->toIso8601String(),
            'placed_at' => Carbon::parse($o->placed_at)->toIso8601String(),
        ]);

        return response()->json($halaman);
    }

    /** GET /api/orders/{order} (K14) */
    public function detail(Request $request, int $order): JsonResponse
    {
        return response()->json(['data' => $this->detailPesanan($order, $request->user()->id)]);
    }

    /** POST /api/orders/{order}/cancel */
    public function batalkan(Request $request, int $order): JsonResponse
    {
        $data = $request->validate(['reason' => ['nullable', 'string', 'max:255']]);
        $this->pesanan->batalkan($order, $request->user()->id, $data['reason'] ?? null);

        return response()->json(['data' => $this->detailPesanan($order, $request->user()->id)]);
    }

    private function validasiItem(Request $request, array $tambahan = []): array
    {
        $aturan = config('lof.pesanan');

        return $request->validate([
            'items' => ['required', 'array', 'min:1', 'max:'.$aturan['maks_item']],
            'items.*.listing_id' => ['required', 'integer', 'distinct'],
            'items.*.qty' => ['required', 'integer', 'min:1', 'max:'.$aturan['maks_qty_per_item']],
            ...$tambahan,
        ]);
    }

    private function khususKonsumen(Request $request): void
    {
        abort_if($request->user()->role !== 'consumer', 403, 'Pesanan hanya bisa dibuat dari akun konsumen.');
    }

    /** Pesanan orang lain dijawab 404, bukan 403. */
    private function detailPesanan(int $id, int $userId): array
    {
        $o = DB::table('orders')
            ->join('stores', 'stores.id', '=', 'orders.store_id')
            ->leftJoin('pickup_codes', 'pickup_codes.order_id', '=', 'orders.id')
            ->where('orders.id', $id)->where('orders.user_id', $userId)
            ->select('orders.*', 'stores.name as store_name', 'stores.address as store_address',
                'stores.latitude as store_latitude', 'stores.longitude as store_longitude',
                'pickup_codes.code as pickup_code', 'pickup_codes.status as pickup_code_status')
            ->first();

        abort_if($o === null, 404, 'Pesanan tidak ditemukan.');

        $item = DB::table('order_items')->where('order_id', $o->id)->orderBy('id')
            ->get(['listing_id', 'title_snapshot as title', 'unit_price_rupiah', 'qty', 'line_total_rupiah']);

        return [
            'id' => $o->id,
            'code' => $o->code,
            'status' => $o->status,
            // Kode hanya ditampilkan selama masih bisa ditukar.
            'pickup_code' => $o->pickup_code_status === 'active' ? $o->pickup_code : null,
            'pickup_code_status' => $o->pickup_code_status,
            'pickup_start' => Carbon::parse($o->pickup_start)->toIso8601String(),
            'pickup_end' => Carbon::parse($o->pickup_end)->toIso8601String(),
            'store' => [
                'id' => $o->store_id, 'name' => $o->store_name, 'address' => $o->store_address,
                'latitude' => $o->store_latitude === null ? null : (float) $o->store_latitude,
                'longitude' => $o->store_longitude === null ? null : (float) $o->store_longitude,
            ],
            'items' => $item,
            'item_count' => (int) $item->sum('qty'),
            'subtotal_rupiah' => $o->subtotal_rupiah,
            'service_fee_rupiah' => $o->service_fee_rupiah,
            'discount_rupiah' => $o->discount_rupiah,
            'total_rupiah' => $o->total_rupiah,
            'payment_method' => $o->payment_method,
            'payment_status' => $o->payment_status,
            'note' => $o->note,
            'placed_at' => Carbon::parse($o->placed_at)->toIso8601String(),
            'completed_at' => $o->completed_at === null ? null : Carbon::parse($o->completed_at)->toIso8601String(),
            'cancelled_at' => $o->cancelled_at === null ? null : Carbon::parse($o->cancelled_at)->toIso8601String(),
        ];
    }
}
