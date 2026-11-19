<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * Voucher, Gosok & Menang, dan potongan di pesanan (ADR-0008).
 */
class VoucherTest extends TestCase
{
    use RefreshDatabase;

    private User $pembeli;

    private User $pemilik;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();
        $this->travelTo(now()->setTime(17, 0));
        $this->pembeli = User::forceCreate(['phone' => '6281234567890', 'role' => 'consumer'])->refresh();
        $this->pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Roti Sari Bakery', 'slug' => 'roti-sari',
            'category' => 'bakery', 'address' => 'Jl. Tebet Raya No. 12',
        ]);
    }

    public function test_klaim_voucher_popup_sekali_saja(): void
    {
        $v = $this->voucher(['code' => 'WARHEMAT', 'source' => 'popup', 'type' => 'percent', 'value' => 20]);

        $this->ambil('/api/vouchers')->assertOk()->assertJsonPath('data.promo.id', $v)->assertJsonPath('data.mine', []);
        $this->kirim("/api/vouchers/{$v}/claim")->assertCreated()->assertJsonPath('data.code', 'WARHEMAT');
        $this->kirim("/api/vouchers/{$v}/claim")->assertStatus(409);
        $this->ambil('/api/vouchers')->assertJsonPath('data.promo', null)->assertJsonCount(1, 'data.mine');
    }

    public function test_gosok_sekali_sehari(): void
    {
        $this->voucher(['code' => 'GOSOK5RB', 'source' => 'game', 'weight' => 100]);

        $hasil = $this->kirim('/api/games/scratch')->assertOk();
        $this->assertIsBool($hasil->json('data.won'));
        $this->kirim('/api/games/scratch')->assertStatus(409);
        $this->ambil('/api/vouchers')->assertJsonPath('data.can_scratch_today', false);

        $this->travel(1)->days();
        $this->kirim('/api/games/scratch')->assertOk();
    }

    public function test_voucher_memotong_total_dan_disubsidi_saat_kode_ditukar(): void
    {
        $milik = $this->milik(['type' => 'percent', 'value' => 20, 'max_discount_rupiah' => 10000, 'min_spend_rupiah' => 15000]);
        $l = $this->listing(['price_rupiah' => 30000]);

        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $l, 'qty' => 1]], 'user_voucher_id' => $milik])
            ->assertOk()->assertJsonPath('data.discount_rupiah', 6000)->assertJsonPath('data.total_rupiah', 24000);

        $pesanan = $this->kirim('/api/orders', ['items' => [['listing_id' => $l, 'qty' => 1]], 'user_voucher_id' => $milik])
            ->assertCreated()->assertJsonPath('data.discount_rupiah', 6000)->assertJsonPath('data.total_rupiah', 24000);
        $this->assertNotNull(DB::table('user_vouchers')->where('id', $milik)->value('used_at'));

        // Voucher yang sudah dipakai tidak bisa dipakai lagi.
        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $l, 'qty' => 1]], 'user_voucher_id' => $milik])
            ->assertJsonValidationErrors('user_voucher_id');

        $kode = DB::table('pickup_codes')->where('order_id', $pesanan->json('data.id'))->value('code');
        $this->actingAs($this->pemilik)->postJson('/api/pickup-codes/redeem', ['store_id' => $this->toko, 'code' => $kode])->assertOk();

        $this->assertSame(30000, (int) DB::table('store_balances')->where('store_id', $this->toko)->value('available_rupiah'));
        $this->assertSame(6000, (int) DB::table('balance_transactions')->where('type', 'adjustment')->value('amount_rupiah'));
    }

    public function test_minimal_belanja_dan_batal_mengembalikan_voucher(): void
    {
        $milik = $this->milik(['type' => 'rupiah', 'value' => 5000, 'min_spend_rupiah' => 20000]);
        $murah = $this->listing(['price_rupiah' => 10000]);
        $mahal = $this->listing(['price_rupiah' => 25000]);

        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $murah, 'qty' => 1]], 'user_voucher_id' => $milik])
            ->assertJsonValidationErrors('user_voucher_id');

        $id = $this->kirim('/api/orders', ['items' => [['listing_id' => $mahal, 'qty' => 1]], 'user_voucher_id' => $milik])
            ->assertCreated()->assertJsonPath('data.total_rupiah', 20000)->json('data.id');
        $this->kirim("/api/orders/{$id}/cancel", [])->assertOk();

        $this->assertNull(DB::table('user_vouchers')->where('id', $milik)->value('used_at'));
    }

    public function test_voucher_orang_lain_ditolak(): void
    {
        $lain = User::forceCreate(['phone' => '6281234500009', 'role' => 'consumer']);
        $milikLain = $this->milik(['type' => 'rupiah', 'value' => 5000], $lain->id);
        $l = $this->listing();

        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $l, 'qty' => 1]], 'user_voucher_id' => $milikLain])
            ->assertJsonValidationErrors('user_voucher_id');
    }

    private function ambil(string $uri): TestResponse
    {
        return $this->actingAs($this->pembeli)->getJson($uri);
    }

    private function kirim(string $uri, array $data = []): TestResponse
    {
        return $this->actingAs($this->pembeli)->postJson($uri, $data);
    }

    private function voucher(array $kolom = []): int
    {
        return DB::table('vouchers')->insertGetId([
            'code' => 'V'.random_int(1000, 9999), 'title' => 'Voucher uji', 'type' => 'rupiah', 'value' => 5000,
            'min_spend_rupiah' => 0, 'valid_days' => 3, 'source' => 'admin', 'weight' => 0, 'is_active' => true,
            'created_at' => now(), 'updated_at' => now(), ...$kolom,
        ]);
    }

    private function milik(array $kolom, ?int $userId = null): int
    {
        return DB::table('user_vouchers')->insertGetId([
            'user_id' => $userId ?? $this->pembeli->id, 'voucher_id' => $this->voucher($kolom),
            'claimed_at' => now(), 'expires_at' => now()->addDays(3), 'created_at' => now(), 'updated_at' => now(),
        ]);
    }

    private function listing(array $kolom = []): int
    {
        return DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas Kejutan', 'price_rupiah' => 18000,
            'qty_total' => 5, 'pickup_date' => now()->toDateString(), 'pickup_start' => now()->setTime(18, 0),
            'pickup_end' => now()->setTime(20, 0), 'status' => 'active', 'ingredients_text' => 'Tepung, gula',
            ...$kolom,
        ]);
    }
}
