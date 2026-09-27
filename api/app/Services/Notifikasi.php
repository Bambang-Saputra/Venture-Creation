<?php

namespace App\Services;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Notifikasi di dalam aplikasi (K17, F-18). Hanya disimpan di tabel
 * notifications; Android membacanya lewat GET /notifications. Push (FCM)
 * belum ada, jadi pengiriman ke perangkat menyusul.
 *
 * Semua pemicu dipanggil SETELAH transaksi utamanya selesai: kalau
 * penulisan notifikasi gagal, pesanan atau jualan tetap tersimpan.
 */
class Notifikasi
{
    /** Pesanan baru masuk: ke pemilik dan semua kasir aktif toko (M11). */
    public function pesananBaru(int $orderId): void
    {
        $o = DB::table('orders')->where('id', $orderId)->first();
        $jumlah = (int) DB::table('order_items')->where('order_id', $orderId)->sum('qty');
        $jam = Carbon::parse($o->pickup_start)->format('H.i').' sampai '.Carbon::parse($o->pickup_end)->format('H.i');

        foreach ($this->stafToko($o->store_id) as $userId) {
            $this->kirim($userId, 'pesanan_baru', 'Pesanan baru', "{$jumlah} item, diambil {$jam}.",
                ['screen' => 'M11', 'store_id' => $o->store_id, 'order_id' => $orderId]);
        }
    }

    /** Kode sudah ditukar kasir: "1 porsi terselamatkan" ke pembeli. */
    public function porsiTerselamatkan(int $orderId): void
    {
        $o = DB::table('orders')->join('stores', 'stores.id', '=', 'orders.store_id')
            ->where('orders.id', $orderId)->select('orders.user_id', 'stores.name as store_name')->first();
        $porsi = (int) DB::table('order_items')->where('order_id', $orderId)->sum('qty');
        $total = (int) DB::table('order_items')->join('orders', 'orders.id', '=', 'order_items.order_id')
            ->where('orders.user_id', $o->user_id)->where('orders.status', 'completed')->sum('order_items.qty');

        $this->kirim($o->user_id, 'porsi_terselamatkan', "{$porsi} porsi terselamatkan",
            "Pesanan {$this->judulPesanan($orderId)} di {$o->store_name} selesai. Totalmu sekarang {$total} porsi.",
            ['screen' => 'K14', 'order_id' => $orderId]);
    }

    /**
     * Jualan terbit: ke konsumen yang memfavoritkan toko dan menyalakan
     * notify_favorite_store. Paling banyak satu per toko per orang per hari (K16).
     */
    public function mitraFavoritMemasang(int $listingId): void
    {
        $l = DB::table('listings')->join('stores', 'stores.id', '=', 'listings.store_id')
            ->where('listings.id', $listingId)->select('listings.store_id', 'listings.type', 'listings.title', 'stores.name as store_name')->first();

        $penerima = DB::table('favorites as f')
            ->join('users as u', 'u.id', '=', 'f.user_id')
            ->leftJoin('consumer_profiles as p', 'p.user_id', '=', 'f.user_id')
            ->where('f.store_id', $l->store_id)->where('u.is_active', true)
            // Profil yang belum dibuat memakai nilai bawaan kolom (true).
            ->where(fn ($q) => $q->whereNull('p.user_id')->orWhere('p.notify_favorite_store', true))
            ->whereNotExists(fn ($q) => $q->from('notifications as n')->whereColumn('n.user_id', 'f.user_id')
                ->where('n.type', 'mitra_favorit_memasang')->where('n.created_at', '>=', now()->startOfDay())
                ->whereRaw("JSON_UNQUOTE(JSON_EXTRACT(n.data, '$.store_id')) = ?", [(string) $l->store_id]))
            ->pluck('f.user_id');

        $jenis = $l->type === 'surprise_bag' ? 'tas kejutan' : 'menu satuan';
        foreach ($penerima as $userId) {
            $this->kirim($userId, 'mitra_favorit_memasang', 'Mitra favoritmu memasang jualan',
                "{$l->store_name} baru saja membuka {$jenis} {$l->title}.",
                ['screen' => 'K10', 'store_id' => $l->store_id, 'listing_id' => $listingId]);
        }
    }

    /**
     * "Pesananmu siap diambil": 30 menit sebelum jam ambil dimulai, untuk yang
     * menyalakan notify_pickup_reminder. Aman dipanggil berulang, satu per pesanan.
     *
     * @return int jumlah pengingat yang dikirim
     */
    public function pengingatAmbil(int $menitSebelum = 30): int
    {
        $pesanan = DB::table('orders as o')
            ->join('stores as s', 's.id', '=', 'o.store_id')
            ->leftJoin('pickup_codes as pc', 'pc.order_id', '=', 'o.id')
            ->leftJoin('consumer_profiles as p', 'p.user_id', '=', 'o.user_id')
            ->where('o.status', 'pending_pickup')
            ->whereBetween('o.pickup_start', [now(), now()->addMinutes($menitSebelum)])
            ->where(fn ($q) => $q->whereNull('p.user_id')->orWhere('p.notify_pickup_reminder', true))
            ->whereNotExists(fn ($q) => $q->from('notifications as n')->whereColumn('n.user_id', 'o.user_id')
                ->where('n.type', 'pengingat_ambil')
                ->whereRaw("JSON_UNQUOTE(JSON_EXTRACT(n.data, '$.order_id')) = CAST(o.id AS CHAR)"))
            ->select('o.id', 'o.user_id', 's.name as store_name', 'pc.code')
            ->get();

        foreach ($pesanan as $o) {
            $this->kirim($o->user_id, 'pengingat_ambil', 'Pesananmu siap diambil',
                "{$this->judulPesanan($o->id)} di {$o->store_name}. Tunjukkan kode {$o->code} ke kasir.",
                ['screen' => 'K14', 'order_id' => $o->id]);
        }

        return $pesanan->count();
    }

    private function kirim(int $userId, string $jenis, string $judul, string $isi, array $data): void
    {
        DB::table('notifications')->insert([
            'user_id' => $userId, 'type' => $jenis, 'title' => mb_substr($judul, 0, 140), 'body' => $isi,
            'data' => json_encode($data), 'created_at' => now(), 'updated_at' => now(),
        ]);
    }

    /** @return array<int, int> */
    private function stafToko(int $store): array
    {
        $pemilik = DB::table('stores')->where('id', $store)->value('owner_user_id');

        return DB::table('store_members')->where('store_id', $store)->whereNull('revoked_at')->pluck('user_id')
            ->push($pemilik)->map(fn ($id) => (int) $id)->unique()->values()->all();
    }

    private function judulPesanan(int $orderId): string
    {
        $item = DB::table('order_items')->where('order_id', $orderId)->orderBy('id')->pluck('title_snapshot');

        return $item->first().($item->count() > 1 ? ' +'.($item->count() - 1).' lainnya' : '');
    }
}
