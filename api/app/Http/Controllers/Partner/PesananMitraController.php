<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * Pesanan dari sisi mitra: M11 Pesanan masuk, M21 Riwayat pesanan,
 * M12 Cocokkan kode (F-10, F-11). Kasir boleh memakai semuanya.
 */
class PesananMitraController extends Controller
{
    use AksesToko;

    // Pembeli yang datang sedikit terlambat tetap dilayani.
    private const TOLERANSI_MENIT = 30;

    /** GET /api/partner/stores/{store}/orders?status=pending|history&date= */
    public function daftar(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store, pemilikSaja: false);
        $f = $request->validate([
            'status' => ['sometimes', Rule::in(['pending', 'history'])],
            'date' => ['sometimes', 'date_format:Y-m-d'],
        ]);
        $riwayat = ($f['status'] ?? 'pending') === 'history';

        $halaman = DB::table('orders')
            ->join('users', 'users.id', '=', 'orders.user_id')
            ->where('orders.store_id', $store)
            ->when(! $riwayat, fn ($q) => $q->where('orders.status', 'pending_pickup')->orderBy('orders.pickup_start'))
            ->when($riwayat, fn ($q) => $q->where('orders.status', '!=', 'pending_pickup')->orderByDesc('orders.placed_at'))
            ->when($f['date'] ?? null, fn ($q, $tgl) => $q->whereDate('orders.pickup_start', $tgl))
            ->orderBy('orders.id')
            ->select('orders.*', 'users.name as buyer_name')
            ->paginate(30);

        $item = $this->itemPer($halaman->pluck('id'));
        $halaman->through(fn (object $o) => $this->bentuk($o, $item->get($o->id, collect())));

        return response()->json($halaman);
    }

    /** POST /api/pickup-codes/redeem { store_id, code } */
    public function tukar(Request $request): JsonResponse
    {
        $data = $request->validate([
            'store_id' => ['required', 'integer'],
            'code' => ['required', 'string', 'size:6'],
        ]);
        $this->tokoMilik($request, $data['store_id'], pemilikSaja: false);
        $kode = strtoupper(trim($data['code']));
        $kasir = $request->user();

        $orderId = DB::transaction(function () use ($data, $kode, $kasir, $request) {
            $pc = DB::table('pickup_codes')->where('store_id', $data['store_id'])->where('code', $kode)->lockForUpdate()->first();

            abort_if($pc === null, 404, 'Kode tidak ditemukan di toko ini. Periksa lagi hurufnya.');
            abort_if($pc->status === 'used', 409, 'Kode ini sudah dipakai pada '.Carbon::parse($pc->used_at)->format('H.i').'.');
            abort_if($pc->status !== 'active', 409, 'Pesanan ini sudah dibatalkan.');
            abort_if(Carbon::parse($pc->expires_at)->addMinutes(self::TOLERANSI_MENIT)->isPast(), 409, 'Jam ambil pesanan ini sudah lewat.');

            $o = DB::table('orders')->where('id', $pc->order_id)->lockForUpdate()->first();
            abort_if($o->status !== 'pending_pickup', 409, 'Pesanan ini sudah tidak menunggu diambil.');

            $item = DB::table('order_items')->where('order_id', $o->id)->get();
            DB::table('listings')->whereIn('id', $item->pluck('listing_id'))->orderBy('id')->lockForUpdate()->get();
            foreach ($item as $i) {
                DB::table('listings')->where('id', $i->listing_id)->update([
                    'qty_reserved' => DB::raw('qty_reserved - '.(int) $i->qty),
                    'qty_sold' => DB::raw('qty_sold + '.(int) $i->qty),
                    'updated_at' => now(),
                ]);
            }

            DB::table('pickup_codes')->where('id', $pc->id)->update([
                'status' => 'used', 'used_at' => now(), 'used_by_user_id' => $kasir->id, 'updated_at' => now(),
            ]);
            // Bayar di tempat selama pilot (ADR-0004): kode ditukar berarti sudah dibayar.
            DB::table('orders')->where('id', $o->id)->update([
                'status' => 'completed', 'payment_status' => 'paid', 'completed_at' => now(), 'updated_at' => now(),
            ]);

            DB::table('store_balances')->insertOrIgnore(['store_id' => $o->store_id, 'created_at' => now(), 'updated_at' => now()]);
            $saldo = DB::table('store_balances')->where('store_id', $o->store_id)->lockForUpdate()->first();
            $tersedia = $saldo->available_rupiah + $o->total_rupiah;
            DB::table('store_balances')->where('store_id', $o->store_id)->update([
                'pending_rupiah' => max(0, $saldo->pending_rupiah - $o->total_rupiah),
                'available_rupiah' => $tersedia,
                'lifetime_rupiah' => $saldo->lifetime_rupiah + $o->total_rupiah,
                'updated_at' => now(),
            ]);
            DB::table('balance_transactions')->insert([
                'store_id' => $o->store_id, 'order_id' => $o->id, 'type' => 'sale',
                'amount_rupiah' => $o->total_rupiah, 'balance_after_rupiah' => $tersedia,
                'description' => "Pesanan {$o->code}", 'created_by_user_id' => $kasir->id,
                'created_at' => now(), 'updated_at' => now(),
            ]);

            // Kamus data: jangan simpan nomor HP lengkap atau kode mentah di meta.
            DB::table('audit_logs')->insert([
                'user_id' => $kasir->id, 'store_id' => $o->store_id, 'action' => 'pickup_code.redeem',
                'subject_type' => 'order', 'subject_id' => $o->id,
                'meta' => json_encode(['order_code' => $o->code, 'total_rupiah' => $o->total_rupiah]),
                'ip' => $request->ip(), 'created_at' => now(), 'updated_at' => now(),
            ]);

            return $o->id;
        });

        $o = DB::table('orders')->join('users', 'users.id', '=', 'orders.user_id')
            ->where('orders.id', $orderId)->select('orders.*', 'users.name as buyer_name')->first();

        return response()->json(['data' => $this->bentuk($o, $this->itemPer(collect([$orderId]))->get($orderId, collect()))]);
    }

    /** @return Collection<int, Collection<int, object>> */
    private function itemPer(Collection $orderId): Collection
    {
        return DB::table('order_items')->whereIn('order_id', $orderId)->orderBy('id')
            ->get(['order_id', 'title_snapshot as title', 'qty', 'unit_price_rupiah', 'line_total_rupiah'])
            ->groupBy('order_id');
    }

    private function bentuk(object $o, Collection $item): array
    {
        return [
            'id' => $o->id,
            'code' => $o->code,
            'status' => $o->status,
            // Nama depan saja: cukup untuk memanggil pembeli di kasir.
            'buyer_name' => $o->buyer_name === null ? 'Pembeli' : strtok($o->buyer_name, ' '),
            'items' => $item->map(fn ($i) => ['title' => $i->title, 'qty' => $i->qty, 'line_total_rupiah' => $i->line_total_rupiah])->values(),
            'item_count' => (int) $item->sum('qty'),
            'total_rupiah' => $o->total_rupiah,
            'payment_method' => $o->payment_method,
            'payment_status' => $o->payment_status,
            'note' => $o->note,
            // Salinan saat memesan (kamus data), supaya mitra melihat kondisi saat itu.
            'allergen_snapshot' => json_decode($o->allergen_snapshot ?? '[]', true),
            'pickup_start' => Carbon::parse($o->pickup_start)->toIso8601String(),
            'pickup_end' => Carbon::parse($o->pickup_end)->toIso8601String(),
            'placed_at' => Carbon::parse($o->placed_at)->toIso8601String(),
            'completed_at' => $o->completed_at === null ? null : Carbon::parse($o->completed_at)->toIso8601String(),
        ];
    }
}
