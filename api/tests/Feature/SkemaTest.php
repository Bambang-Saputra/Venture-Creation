<?php

namespace Tests\Feature;

use Illuminate\Database\QueryException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * Menjaga janji yang ditegakkan database, bukan aplikasi.
 *
 * Kalau seseorang menghapus CHECK constraint di migrasi berikutnya, uji ini
 * yang gagal lebih dulu, bukan pembeli yang alergi kacang.
 */
class SkemaTest extends TestCase
{
    use RefreshDatabase;

    private int $storeId;

    protected function setUp(): void
    {
        parent::setUp();

        $userId = DB::table('users')->insertGetId([
            'phone' => '62811000999',
            'name' => 'Pemilik Uji',
            'role' => 'partner',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        $this->storeId = DB::table('stores')->insertGetId([
            'owner_user_id' => $userId,
            'name' => 'Toko Uji',
            'slug' => 'toko-uji',
            'category' => 'bakery',
            'address' => 'Jakarta Selatan',
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }

    public function test_listing_tidak_bisa_terbit_tanpa_kandungan(): void
    {
        $this->expectException(QueryException::class);

        DB::table('listings')->insert($this->listing(['status' => 'active']));
    }

    public function test_listing_boleh_terbit_setelah_kandungan_diisi(): void
    {
        DB::table('listings')->insert($this->listing([
            'status' => 'active',
            'ingredients_text' => 'Tepung terigu, telur, susu sapi',
        ]));

        $this->assertDatabaseCount('listings', 1);
    }

    public function test_draft_boleh_kosong_kandungannya(): void
    {
        DB::table('listings')->insert($this->listing(['status' => 'draft']));

        $this->assertDatabaseCount('listings', 1);
    }

    public function test_stok_terjual_tidak_boleh_melebihi_kuota(): void
    {
        $this->expectException(QueryException::class);

        DB::table('listings')->insert($this->listing([
            'status' => 'draft',
            'qty_total' => 5,
            'qty_reserved' => 3,
            'qty_sold' => 4,
        ]));
    }

    public function test_jam_ambil_tidak_boleh_terbalik(): void
    {
        $this->expectException(QueryException::class);

        DB::table('listings')->insert($this->listing([
            'status' => 'draft',
            'pickup_end' => '2026-10-01 16:00:00',
        ]));
    }

    public function test_total_pesanan_harus_sama_dengan_rinciannya(): void
    {
        $this->expectException(QueryException::class);

        DB::table('orders')->insert($this->pesanan(['total_rupiah' => 15000]));
    }

    public function test_kode_pickup_unik_lintas_toko(): void
    {
        $tokoLain = $this->tokoKedua();

        $pertama = DB::table('orders')->insertGetId($this->pesanan(['code' => 'LOF-0001']));
        $kedua = DB::table('orders')->insertGetId($this->pesanan([
            'code' => 'LOF-0002',
            'store_id' => $tokoLain,
        ]));

        DB::table('pickup_codes')->insert($this->kodePickup($pertama, $this->storeId));

        // Kode yang sama di toko berbeda tetap ditolak. Kalau tidak, kasir bisa
        // menukar kode milik toko sebelah karena kebetulan angkanya sama.
        $this->expectException(QueryException::class);

        DB::table('pickup_codes')->insert($this->kodePickup($kedua, $tokoLain));
    }

    /**
     * @param  array<string,mixed>  $ubah
     * @return array<string,mixed>
     */
    private function listing(array $ubah = []): array
    {
        return array_merge([
            'store_id' => $this->storeId,
            'type' => 'surprise_bag',
            'title' => 'Tas Kejutan Uji',
            'price_rupiah' => 20000,
            'qty_total' => 5,
            'pickup_date' => '2026-10-01',
            'pickup_start' => '2026-10-01 18:00:00',
            'pickup_end' => '2026-10-01 20:00:00',
            'created_at' => now(),
            'updated_at' => now(),
        ], $ubah);
    }

    /**
     * @param  array<string,mixed>  $ubah
     * @return array<string,mixed>
     */
    private function pesanan(array $ubah = []): array
    {
        return array_merge([
            'code' => 'LOF-9999',
            'user_id' => DB::table('users')->value('id'),
            'store_id' => $this->storeId,
            'subtotal_rupiah' => 20000,
            'service_fee_rupiah' => 0,
            'discount_rupiah' => 0,
            'total_rupiah' => 20000,
            'pickup_start' => '2026-10-01 18:00:00',
            'pickup_end' => '2026-10-01 20:00:00',
            'placed_at' => now(),
            'created_at' => now(),
            'updated_at' => now(),
        ], $ubah);
    }

    private function tokoKedua(): int
    {
        $userId = DB::table('users')->insertGetId([
            'phone' => '62811000998',
            'name' => 'Pemilik Uji Dua',
            'role' => 'partner',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return DB::table('stores')->insertGetId([
            'owner_user_id' => $userId,
            'name' => 'Toko Uji Dua',
            'slug' => 'toko-uji-dua',
            'category' => 'resto',
            'address' => 'Jakarta Timur',
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }

    /**
     * @return array<string,mixed>
     */
    private function kodePickup(int $orderId, int $storeId): array
    {
        return [
            'order_id' => $orderId,
            'store_id' => $storeId,
            'code' => '123456',
            'issued_at' => now(),
            'expires_at' => now()->addHours(3),
            'created_at' => now(),
            'updated_at' => now(),
        ];
    }
}
