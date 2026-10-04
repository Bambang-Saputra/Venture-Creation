<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\SeederAlergen;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * /api/orders (K12-K15).
 */
class PesananTest extends TestCase
{
    use RefreshDatabase;

    private User $pembeli;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(SeederAlergen::class);
        $this->travelTo(now()->setTime(17, 0));

        $this->pembeli = User::forceCreate(['phone' => '6281234567890', 'role' => 'consumer'])->refresh();
        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $this->toko = $this->toko($pemilik->id, 'kopi-kalyan');
    }

    public function test_pratinjau_menghitung_tanpa_menyimpan_dan_memberi_peringatan_alergi(): void
    {
        $susu = DB::table('allergens')->where('code', 'susu')->value('id');
        DB::table('user_allergens')->insert(['user_id' => $this->pembeli->id, 'allergen_id' => $susu, 'severity' => 'severe']);
        $croissant = $this->listing(['title' => 'Croissant mentega', 'price_rupiah' => 9000]);
        DB::table('listing_allergens')->insert(['listing_id' => $croissant, 'allergen_id' => $susu]);
        $danish = $this->listing(['title' => 'Danish keju', 'price_rupiah' => 9000, 'pickup_start' => now()->setTime(18, 30)]);

        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $croissant, 'qty' => 2], ['listing_id' => $danish, 'qty' => 1]]])
            ->assertOk()
            ->assertJsonPath('data.subtotal_rupiah', 27000)
            ->assertJsonPath('data.total_rupiah', 27000)
            ->assertJsonPath('data.store.name', 'Kopi Kalyan')
            ->assertJsonPath('data.pickup_start', now()->setTime(18, 30)->toIso8601String())
            ->assertJsonPath('data.allergen_warnings.0.code', 'susu')
            ->assertJsonPath('data.allergen_warnings.0.severity', 'severe');

        $this->assertDatabaseCount('orders', 0);
        $this->assertSame(0, DB::table('listings')->where('id', $croissant)->value('qty_reserved'));
    }

    public function test_buat_pesanan_memesan_stok_dan_menerbitkan_kode_pickup(): void
    {
        $id = $this->listing(['qty_total' => 3, 'price_rupiah' => 18000]);

        $pesanan = $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 2]], 'note' => 'Alergi kacang, tolong dipisah ya'])
            ->assertCreated()
            ->assertJsonPath('data.status', 'pending_pickup')
            ->assertJsonPath('data.total_rupiah', 36000)
            ->assertJsonPath('data.item_count', 2)
            ->assertJsonPath('data.items.0.title', 'Tas Kejutan')
            ->assertJsonPath('data.pickup_code_status', 'active')
            ->json('data');

        $this->assertMatchesRegularExpression('/^[A-HJKMNP-Z2-9]{6}$/', $pesanan['pickup_code']);
        $this->assertMatchesRegularExpression('/^LOF-[A-HJKMNP-Z2-9]{6}$/', $pesanan['code']);
        $this->assertSame(2, DB::table('listings')->where('id', $id)->value('qty_reserved'));
        $this->assertSame(36000, DB::table('store_balances')->where('store_id', $this->toko)->value('pending_rupiah'));

        $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 2]]])
            ->assertUnprocessable()->assertJsonPath('errors.items.0', 'Tas Kejutan tinggal 1.');
    }

    public function test_stok_habis_mengubah_status_dan_hilang_dari_beranda(): void
    {
        $id = $this->listing(['qty_total' => 1]);

        $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 1]]])->assertCreated();

        $this->assertSame('sold_out', DB::table('listings')->where('id', $id)->value('status'));
        $this->getJson('/api/listings')->assertJsonCount(0, 'data');
        $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 1]]])->assertUnprocessable();
    }

    public function test_batal_mengembalikan_stok_dan_mencabut_kode(): void
    {
        $id = $this->listing(['qty_total' => 1]);
        $pesanan = $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 1]]])->json('data.id');

        $this->kirim("/api/orders/{$pesanan}/cancel", ['reason' => 'Tidak jadi'])
            ->assertOk()
            ->assertJsonPath('data.status', 'cancelled')
            ->assertJsonPath('data.pickup_code', null);

        $this->assertSame(0, DB::table('listings')->where('id', $id)->value('qty_reserved'));
        $this->assertSame('active', DB::table('listings')->where('id', $id)->value('status'));
        $this->assertSame(0, DB::table('store_balances')->where('store_id', $this->toko)->value('pending_rupiah'));
        $this->kirim("/api/orders/{$pesanan}/cancel")->assertStatus(409);
    }

    public function test_satu_pesanan_satu_toko_dan_jam_ambil_harus_bertemu(): void
    {
        $lainToko = $this->toko(User::forceCreate(['phone' => '6281200000002', 'role' => 'partner'])->id, 'bakerman');
        $a = $this->listing();
        $b = $this->listing(['store_id' => $lainToko]);
        $malam = $this->listing(['pickup_start' => now()->setTime(20, 0), 'pickup_end' => now()->setTime(22, 0)]);

        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $a, 'qty' => 1], ['listing_id' => $b, 'qty' => 1]]])
            ->assertUnprocessable()->assertJsonPath('errors.items.0', 'Satu pesanan hanya untuk satu toko.');
        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $a, 'qty' => 1], ['listing_id' => $malam, 'qty' => 1]]])
            ->assertUnprocessable();
    }

    public function test_jualan_tidak_tersedia_ditolak(): void
    {
        $jeda = $this->listing(['status' => 'paused']);
        $lewat = $this->listing(['pickup_start' => now()->subHours(2), 'pickup_end' => now()->subMinute()]);

        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $jeda, 'qty' => 1]]])->assertUnprocessable();
        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $lewat, 'qty' => 1]]])->assertUnprocessable();
        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => 999999, 'qty' => 1]]])->assertUnprocessable();
        $this->kirim('/api/orders/preview', ['items' => [['listing_id' => $jeda, 'qty' => 6]]])->assertJsonValidationErrors('items.0.qty');
    }

    public function test_batas_pesanan_aktif(): void
    {
        $id = $this->listing(['qty_total' => 10]);
        for ($i = 0; $i < 3; $i++) {
            $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 1]]])->assertCreated();
        }

        $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 1]]])->assertUnprocessable()
            ->assertJsonPath('code', 'active_order_limit')->assertJsonValidationErrors('items');
    }

    public function test_daftar_dan_detail_hanya_milik_sendiri(): void
    {
        $id = $this->listing(['qty_total' => 5]);
        $pesanan = $this->kirim('/api/orders', ['items' => [['listing_id' => $id, 'qty' => 2]]])->json('data.id');

        $this->sebagai($this->pembeli)->getJson('/api/orders?status=active')
            ->assertOk()->assertJsonCount(1, 'data')->assertJsonPath('data.0.item_count', 2);
        $this->sebagai($this->pembeli)->getJson('/api/orders?status=history')->assertJsonCount(0, 'data');

        $orangLain = User::forceCreate(['phone' => '6281299999999', 'role' => 'consumer'])->refresh();
        $this->sebagai($orangLain)->getJson("/api/orders/{$pesanan}")->assertNotFound();
        $this->sebagai($orangLain)->postJson("/api/orders/{$pesanan}/cancel")->assertNotFound();
    }

    public function test_mitra_tidak_bisa_memesan(): void
    {
        $mitra = User::forceCreate(['phone' => '6281200000009', 'role' => 'partner'])->refresh();

        $this->sebagai($mitra)->postJson('/api/orders', ['items' => [['listing_id' => $this->listing(), 'qty' => 1]]])->assertForbidden();
    }

    private function kirim(string $url, array $data = []): TestResponse
    {
        return $this->sebagai($this->pembeli)->postJson($url, $data);
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }

    private function toko(int $pemilik, string $slug): int
    {
        return DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik, 'name' => $slug === 'kopi-kalyan' ? 'Kopi Kalyan' : 'Bakerman', 'slug' => $slug,
            'category' => 'cafe', 'address' => 'Jl. Jend. Sudirman Kav 52',
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
