<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * M11 Pesanan masuk dan M12 Cocokkan kode.
 */
class PesananMitraTest extends TestCase
{
    use RefreshDatabase;

    private User $pembeli;

    private User $kasir;

    private int $toko;

    private int $listing;

    protected function setUp(): void
    {
        parent::setUp();

        $this->travelTo(now()->setTime(17, 0));

        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $this->kasir = User::forceCreate(['phone' => '6281200000002', 'role' => 'partner'])->refresh();
        $this->pembeli = User::forceCreate(['phone' => '6281234567890', 'role' => 'consumer', 'name' => 'Dara Renata'])->refresh();

        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik->id, 'name' => 'Kopi Kalyan', 'slug' => 'kopi-kalyan', 'category' => 'cafe', 'address' => 'Jl. Uji',
        ]);
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $this->kasir->id, 'role' => 'cashier']);
        $this->listing = DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000,
            'qty_total' => 4, 'pickup_date' => now()->toDateString(), 'pickup_start' => now()->setTime(18, 0),
            'pickup_end' => now()->setTime(20, 0), 'status' => 'active', 'ingredients_text' => 'Tepung, gula',
        ]);
    }

    public function test_pesanan_masuk_terlihat_kasir_dengan_catatan_dan_nama_depan(): void
    {
        $this->pesan(2, 'Alergi kacang, tolong dipisah ya');

        $this->sebagai($this->kasir)->getJson("/api/partner/stores/{$this->toko}/orders")
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.buyer_name', 'Dara')
            ->assertJsonPath('data.0.item_count', 2)
            ->assertJsonPath('data.0.note', 'Alergi kacang, tolong dipisah ya')
            ->assertJsonMissingPath('data.0.pickup_code');
    }

    public function test_tukar_kode_menyelesaikan_pesanan_dan_memindahkan_stok_serta_saldo(): void
    {
        $kode = $this->pesan(2)['pickup_code'];

        $this->tukar(strtolower($kode))
            ->assertOk()
            ->assertJsonPath('data.status', 'completed')
            ->assertJsonPath('data.payment_status', 'paid');

        $l = DB::table('listings')->find($this->listing);
        $this->assertSame([0, 2], [$l->qty_reserved, $l->qty_sold]);
        $saldo = DB::table('store_balances')->where('store_id', $this->toko)->first();
        $this->assertSame([0, 36000, 36000], [$saldo->pending_rupiah, $saldo->available_rupiah, $saldo->lifetime_rupiah]);
        $this->assertDatabaseHas('balance_transactions', ['store_id' => $this->toko, 'type' => 'sale', 'amount_rupiah' => 36000]);
        $this->assertDatabaseHas('pickup_codes', ['code' => $kode, 'status' => 'used', 'used_by_user_id' => $this->kasir->id]);
        $this->assertDatabaseHas('audit_logs', ['action' => 'pickup_code.redeem', 'store_id' => $this->toko]);
        $this->assertStringNotContainsString($kode, DB::table('audit_logs')->value('meta'));

        $this->tukar($kode)->assertStatus(409);
        $this->sebagai($this->kasir)->getJson("/api/partner/stores/{$this->toko}/orders?status=history")
            ->assertJsonCount(1, 'data')->assertJsonPath('data.0.status', 'completed')
            ->assertJsonPath('data.0.pickup_code', $kode);
    }

    public function test_riwayat_m21_dibatasi_hari_dan_membawa_ringkasan(): void
    {
        $diambil = $this->pesan(1)['pickup_code'];
        $this->tukar($diambil)->assertOk();
        $this->pesan(1);
        $tidak = DB::table('orders')->latest('id')->value('id');
        DB::table('orders')->where('id', $tidak)->update(['status' => 'no_show']);

        // Pesanan selesai 10 hari lalu: masuk "30 hari" dan "Semua", tidak masuk "7 hari".
        $lama = $this->pesan(1)['id'];
        DB::table('orders')->where('id', $lama)->update([
            'status' => 'completed', 'pickup_start' => now()->subDays(10)->setTime(18, 0), 'pickup_end' => now()->subDays(10)->setTime(20, 0),
        ]);

        $url = "/api/partner/stores/{$this->toko}/orders?status=history";
        $this->sebagai($this->kasir)->getJson("{$url}&days=7")->assertOk()
            ->assertJsonCount(2, 'data')
            ->assertJsonPath('summary', ['completed' => 1, 'no_show' => 1, 'cancelled' => 0]);
        $this->sebagai($this->kasir)->getJson("{$url}&days=30")->assertJsonCount(3, 'data')
            ->assertJsonPath('summary.completed', 2)
            ->assertJsonPath('data.2.id', $lama);
        $this->sebagai($this->kasir)->getJson($url)->assertJsonCount(3, 'data');
        $this->sebagai($this->kasir)->getJson("{$url}&days=14")->assertStatus(422);
    }

    public function test_kode_salah_batal_atau_lewat_ditolak(): void
    {
        $this->tukar('ZZZZZZ')->assertNotFound();

        $batal = $this->pesan(1);
        $this->sebagai($this->pembeli)->postJson("/api/orders/{$batal['id']}/cancel")->assertOk();
        $this->tukar($batal['pickup_code'] ?? DB::table('pickup_codes')->where('order_id', $batal['id'])->value('code'))->assertStatus(409);

        $lewat = $this->pesan(1)['pickup_code'];
        $this->travelTo(now()->setTime(20, 31));
        $this->tukar($lewat)->assertStatus(409);
    }

    public function test_kode_toko_lain_tidak_bisa_ditukar(): void
    {
        $kode = $this->pesan(1)['pickup_code'];
        $pemilikLain = User::forceCreate(['phone' => '6281200000003', 'role' => 'partner'])->refresh();
        $tokoLain = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilikLain->id, 'name' => 'Lain', 'slug' => 'lain', 'category' => 'cafe', 'address' => 'x',
        ]);

        $this->sebagai($pemilikLain)->postJson('/api/pickup-codes/redeem', ['store_id' => $tokoLain, 'code' => $kode])->assertNotFound();
        $this->sebagai($pemilikLain)->postJson('/api/pickup-codes/redeem', ['store_id' => $this->toko, 'code' => $kode])->assertNotFound();
        $this->sebagai($pemilikLain)->getJson("/api/partner/stores/{$this->toko}/orders")->assertNotFound();
    }

    private function pesan(int $qty, ?string $catatan = null): array
    {
        return $this->sebagai($this->pembeli)
            ->postJson('/api/orders', ['items' => [['listing_id' => $this->listing, 'qty' => $qty]], 'note' => $catatan])
            ->assertCreated()->json('data');
    }

    private function tukar(string $kode)
    {
        return $this->sebagai($this->kasir)->postJson('/api/pickup-codes/redeem', ['store_id' => $this->toko, 'code' => $kode]);
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }
}
