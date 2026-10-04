<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Http\Exceptions\HttpResponseException;
use Illuminate\Support\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Pesanan konsumen (F-06, F-07): hitung, buat, batalkan.
 *
 * Aturan stok: sisa = qty_total - qty_reserved - qty_sold. Pesanan menambah
 * qty_reserved di dalam transaksi dengan SELECT ... FOR UPDATE, dan
 * chk_listings_kuota di database menjadi pengaman terakhir.
 */
class LayananPesanan
{
    // Tanpa 0/O, 1/I/L supaya kasir tidak salah baca.
    private const HURUF_KODE = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

    /**
     * Hitung tanpa menyimpan apa pun (POST /orders/preview).
     *
     * @param  array<int, array{listing_id: int, qty: int}>  $item
     */
    public function hitung(User $user, array $item, bool $kunci = false): array
    {
        $qty = collect($item)->mapWithKeys(fn (array $i) => [$i['listing_id'] => $i['qty']]);

        $listing = DB::table('listings')
            ->join('stores', 'stores.id', '=', 'listings.store_id')
            ->whereIn('listings.id', $qty->keys())
            ->select('listings.*', 'stores.name as store_name', 'stores.address as store_address',
                'stores.is_active as store_active', 'stores.is_temporarily_closed')
            ->orderBy('listings.id')
            ->when($kunci, fn ($q) => $q->lockForUpdate())
            ->get()
            ->keyBy('id');

        foreach ($qty as $id => $jumlah) {
            $l = $listing->get($id);
            $kunciGalat = 'items';

            if ($l === null || $l->status === 'draft') {
                throw ValidationException::withMessages([$kunciGalat => 'Jualan tidak ditemukan.']);
            }
            if ($l->status !== 'active' || ! $l->store_active || $l->is_temporarily_closed || Carbon::parse($l->pickup_end)->isPast()) {
                throw ValidationException::withMessages([$kunciGalat => "{$l->title} sudah tidak tersedia."]);
            }
            $sisa = $l->qty_total - $l->qty_reserved - $l->qty_sold;
            if ($jumlah > $sisa) {
                throw ValidationException::withMessages([$kunciGalat => $sisa > 0 ? "{$l->title} tinggal {$sisa}." : "{$l->title} sudah habis."]);
            }
        }

        if ($listing->pluck('store_id')->unique()->count() > 1) {
            throw ValidationException::withMessages(['items' => 'Satu pesanan hanya untuk satu toko.']);
        }

        // Semua item diambil sekali datang: jam ambilnya irisan dari semua item.
        $mulai = $listing->map(fn ($l) => Carbon::parse($l->pickup_start))->max();
        $akhir = $listing->map(fn ($l) => Carbon::parse($l->pickup_end))->min();
        if ($akhir->lte($mulai)) {
            throw ValidationException::withMessages(['items' => 'Jam ambil item-item ini tidak bertemu. Pesan terpisah.']);
        }

        $baris = $qty->map(fn (int $jumlah, int $id) => [
            'listing_id' => $id,
            'title' => $listing[$id]->title,
            'qty' => $jumlah,
            'unit_price_rupiah' => $listing[$id]->price_rupiah,
            'line_total_rupiah' => $listing[$id]->price_rupiah * $jumlah,
        ])->values();

        $subtotal = $baris->sum('line_total_rupiah');
        $biaya = config('lof.service_fee');
        $toko = $listing->first();

        return [
            'store' => ['id' => $toko->store_id, 'name' => $toko->store_name, 'address' => $toko->store_address],
            'items' => $baris,
            'pickup_start' => $mulai,
            'pickup_end' => $akhir,
            'subtotal_rupiah' => $subtotal,
            'service_fee_rupiah' => $biaya,
            'discount_rupiah' => 0,
            'total_rupiah' => $subtotal + $biaya,
            'allergen_warnings' => $this->peringatanAlergen($user->id, $qty->keys()),
        ];
    }

    /**
     * @param  array{items: array, note?: ?string, payment_method?: string}  $data
     */
    public function buat(User $user, array $data): int
    {
        $aturan = config('lof.pesanan');
        $aktif = DB::table('orders')->where('user_id', $user->id)->where('status', 'pending_pickup')->count();
        if ($aktif >= $aturan['maks_pesanan_aktif']) {
            $pesan = "Kamu masih punya {$aktif} pesanan yang belum diambil. Ambil dulu sebelum memesan lagi.";
            // Bentuknya tetap seperti galat validasi 422, ditambah code supaya
            // Android tidak perlu mencocokkan teks pesan.
            throw new HttpResponseException(response()->json([
                'message' => $pesan, 'code' => 'active_order_limit', 'errors' => ['items' => [$pesan]],
            ], 422));
        }

        return DB::transaction(function () use ($user, $data) {
            $h = $this->hitung($user, $data['items'], kunci: true);

            foreach ($h['items'] as $i) {
                DB::table('listings')->where('id', $i['listing_id'])->increment('qty_reserved', $i['qty']);
                DB::table('listings')->where('id', $i['listing_id'])
                    ->whereRaw('qty_total <= qty_reserved + qty_sold')
                    ->update(['status' => 'sold_out']);
            }

            $orderId = DB::table('orders')->insertGetId([
                'code' => $this->kodeUnik('orders', 'code', 'LOF-'),
                'user_id' => $user->id,
                'store_id' => $h['store']['id'],
                'status' => 'pending_pickup',
                'subtotal_rupiah' => $h['subtotal_rupiah'],
                'service_fee_rupiah' => $h['service_fee_rupiah'],
                'discount_rupiah' => 0,
                'total_rupiah' => $h['total_rupiah'],
                'payment_method' => $data['payment_method'] ?? 'cash',
                'payment_status' => 'unpaid',
                'note' => $data['note'] ?? null,
                'allergen_snapshot' => json_encode(DB::table('user_allergens')
                    ->join('allergens', 'allergens.id', '=', 'user_allergens.allergen_id')
                    ->where('user_id', $user->id)
                    ->get(['allergens.code', 'allergens.name', 'user_allergens.severity'])),
                'pickup_start' => $h['pickup_start'],
                'pickup_end' => $h['pickup_end'],
                'placed_at' => now(),
                'created_at' => now(),
                'updated_at' => now(),
            ]);

            DB::table('order_items')->insert($h['items']->map(fn (array $i) => [
                'order_id' => $orderId,
                'listing_id' => $i['listing_id'],
                'title_snapshot' => $i['title'],
                'unit_price_rupiah' => $i['unit_price_rupiah'],
                'qty' => $i['qty'],
                'line_total_rupiah' => $i['line_total_rupiah'],
                'created_at' => now(),
                'updated_at' => now(),
            ])->all());

            DB::table('pickup_codes')->insert([
                'order_id' => $orderId,
                'store_id' => $h['store']['id'],
                'code' => $this->kodeUnik('pickup_codes', 'code'),
                'status' => 'active',
                'issued_at' => now(),
                'expires_at' => $h['pickup_end'],
                'created_at' => now(),
                'updated_at' => now(),
            ]);

            $this->ubahSaldoTertunda($h['store']['id'], $h['total_rupiah']);

            return $orderId;
        });
    }

    /** Pembeli membatalkan sebelum jam ambil berakhir: stok dikembalikan. */
    public function batalkan(int $orderId, int $userId, ?string $alasan): void
    {
        DB::transaction(function () use ($orderId, $userId, $alasan) {
            $o = DB::table('orders')->where('id', $orderId)->where('user_id', $userId)->lockForUpdate()->first();
            abort_if($o === null, 404, 'Pesanan tidak ditemukan.');
            abort_if($o->status !== 'pending_pickup', 409, 'Pesanan ini sudah tidak bisa dibatalkan.');

            $item = DB::table('order_items')->where('order_id', $o->id)->get();
            DB::table('listings')->whereIn('id', $item->pluck('listing_id'))->orderBy('id')->lockForUpdate()->get();

            foreach ($item as $i) {
                DB::table('listings')->where('id', $i->listing_id)->decrement('qty_reserved', $i->qty);
                // Stok kembali: yang tadinya habis tampil lagi selama jam ambil belum lewat.
                DB::table('listings')->where('id', $i->listing_id)->where('status', 'sold_out')
                    ->where('pickup_end', '>', now())
                    ->whereRaw('qty_total > qty_reserved + qty_sold')
                    ->update(['status' => 'active']);
            }

            DB::table('orders')->where('id', $o->id)->update([
                'status' => 'cancelled', 'cancelled_at' => now(), 'cancel_reason' => $alasan, 'updated_at' => now(),
            ]);
            DB::table('pickup_codes')->where('order_id', $o->id)->update(['status' => 'cancelled', 'updated_at' => now()]);
            $this->ubahSaldoTertunda($o->store_id, -$o->total_rupiah);
        });
    }

    /**
     * Alergi di profil pembeli yang ada di jualan yang dipesan. Tidak memblokir,
     * tetapi K12/K13 wajib menampilkannya sebelum tombol "Buat pesanan".
     *
     * @param  Collection<int, int>  $listingId
     */
    private function peringatanAlergen(int $userId, Collection $listingId): Collection
    {
        return DB::table('listing_allergens')
            ->join('user_allergens', 'user_allergens.allergen_id', '=', 'listing_allergens.allergen_id')
            ->join('allergens', 'allergens.id', '=', 'listing_allergens.allergen_id')
            ->where('user_allergens.user_id', $userId)
            ->whereIn('listing_allergens.listing_id', $listingId)
            ->orderBy('listing_allergens.listing_id')->orderBy('allergens.sort_order')
            ->get(['listing_allergens.listing_id', 'allergens.code', 'allergens.name', 'listing_allergens.presence', 'user_allergens.severity']);
    }

    private function ubahSaldoTertunda(int $storeId, int $selisih): void
    {
        DB::table('store_balances')->insertOrIgnore(['store_id' => $storeId, 'created_at' => now(), 'updated_at' => now()]);
        DB::table('store_balances')->where('store_id', $storeId)->update([
            'pending_rupiah' => DB::raw('GREATEST(0, CAST(pending_rupiah AS SIGNED) + '.(int) $selisih.')'),
            'updated_at' => now(),
        ]);
    }

    private function kodeUnik(string $tabel, string $kolom, string $awalan = ''): string
    {
        do {
            $kode = $awalan;
            for ($i = 0; $i < 6; $i++) {
                $kode .= self::HURUF_KODE[random_int(0, strlen(self::HURUF_KODE) - 1)];
            }
        } while (DB::table($tabel)->where($kolom, $kode)->exists());

        return $kode;
    }
}
