<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\SeederAlergen;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * /api/partner/stores/{store}/listings (M09, M10, M16, M17).
 */
class ListingMitraTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(SeederAlergen::class);
        $this->travelTo(now()->setTime(15, 0));

        $this->pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Kopi Kalyan', 'slug' => 'kopi-kalyan', 'category' => 'cafe',
            'address' => 'Jl. Uji', 'halal_label' => 'self_claim', 'default_ingredients_text' => 'Tepung terigu, gula, mentega',
        ]);
    }

    public function test_m09_terbitkan_tas_dari_template_dalam_satu_ketukan(): void
    {
        $template = DB::table('surprise_bag_templates')->insertGetId([
            'store_id' => $this->toko, 'name' => 'Tas Pastry Sore', 'content_hint' => 'empat sampai enam potong',
            'price_rupiah' => 18000, 'original_value_rupiah' => 55000,
        ]);

        $this->sebagai($this->pemilik)->getJson($this->url('/templates'))->assertOk()->assertJsonPath('data.0.name', 'Tas Pastry Sore');

        $this->buat(['type' => 'surprise_bag', 'template_id' => $template, 'qty_total' => 4,
            'pickup_start' => '18:00', 'pickup_end' => '20:00', 'allergens' => [['code' => 'gluten'], ['code' => 'kacang_tanah', 'presence' => 'may_contain']]])
            ->assertCreated()
            ->assertJsonPath('data.0.status', 'active')
            ->assertJsonPath('data.0.title', 'Tas Pastry Sore')
            ->assertJsonPath('data.0.price_rupiah', 18000)
            ->assertJsonPath('data.0.potential_income_rupiah', 72000)
            ->assertJsonPath('data.0.ingredients_text', 'Tepung terigu, gula, mentega')
            // Label template lebih spesifik daripada label toko, walaupun not_stated.
            ->assertJsonPath('data.0.halal_label', 'not_stated')
            ->assertJsonPath('data.0.allergens.0.code', 'kacang_tanah')
            ->assertJsonPath('data.0.allergens.0.presence', 'may_contain');

        // Langsung tampil di beranda konsumen.
        $this->getJson('/api/listings')->assertJsonCount(1, 'data')->assertJsonPath('data.0.store.name', 'Kopi Kalyan');
    }

    public function test_m16_menu_satuan_satu_listing_per_item(): void
    {
        $croissant = $this->produk('Croissant mentega', 28000);
        $danish = $this->produk('Danish keju', 27000);

        $this->buat(['type' => 'menu_item', 'pickup_start' => '18:00', 'pickup_end' => '20:00', 'items' => [
            ['product_id' => $croissant, 'qty_total' => 4, 'price_rupiah' => 9000, 'allergens' => [['code' => 'susu'], ['code' => 'gluten']]],
            ['product_id' => $danish, 'qty_total' => 3, 'price_rupiah' => 9000],
        ]])
            ->assertCreated()
            ->assertJsonCount(2, 'data')
            ->assertJsonPath('data.0.title', 'Croissant mentega')
            ->assertJsonPath('data.0.original_value_rupiah', 28000)
            ->assertJsonPath('data.0.halal_label', 'self_claim')
            ->assertJsonPath('data.0.allergens.0.code', 'susu')
            ->assertJsonPath('data.1.allergens', []);

        $this->sebagai($this->pemilik)->getJson($this->url('/listings?type=menu_item'))->assertOk()->assertJsonCount(2, 'data');
    }

    public function test_tanpa_kandungan_tidak_bisa_terbit_tapi_bisa_disimpan_draf(): void
    {
        DB::table('stores')->where('id', $this->toko)->update(['default_ingredients_text' => null]);
        $isian = ['type' => 'surprise_bag', 'title' => 'Tas campur', 'price_rupiah' => 22000, 'qty_total' => 2,
            'pickup_start' => '18:00', 'pickup_end' => '20:00'];

        $this->buat($isian)->assertUnprocessable()->assertJsonValidationErrors('ingredients_text');
        $id = $this->buat([...$isian, 'publish' => false])->assertCreated()->assertJsonPath('data.0.status', 'draft')->json('data.0.id');

        $this->aksi($id, 'publish')->assertUnprocessable();
        DB::table('listings')->where('id', $id)->update(['ingredients_text' => 'Nasi, ayam']);
        $this->aksi($id, 'publish')->assertOk()->assertJsonPath('data.status', 'active');
    }

    public function test_jeda_lalu_terbitkan_lagi(): void
    {
        $id = $this->buat(['type' => 'surprise_bag', 'title' => 'Tas', 'price_rupiah' => 15000, 'qty_total' => 3,
            'pickup_start' => '18:00', 'pickup_end' => '20:00'])->json('data.0.id');

        $this->aksi($id, 'pause')->assertOk()->assertJsonPath('data.status', 'paused');
        $this->getJson('/api/listings')->assertJsonCount(0, 'data');
        $this->aksi($id, 'pause')->assertStatus(409);
        $this->aksi($id, 'publish')->assertOk()->assertJsonPath('data.status', 'active');
    }

    public function test_jam_ambil_tidak_valid_ditolak(): void
    {
        $isian = ['type' => 'surprise_bag', 'title' => 'Tas', 'price_rupiah' => 15000, 'qty_total' => 3];

        $this->buat([...$isian, 'pickup_start' => '20:00', 'pickup_end' => '18:00'])->assertJsonValidationErrors('pickup_end');
        $this->buat([...$isian, 'pickup_start' => '12:00', 'pickup_end' => '14:00'])->assertJsonValidationErrors('pickup_end');
    }

    public function test_produk_dan_template_toko_lain_ditolak(): void
    {
        $lain = DB::table('stores')->insertGetId(['owner_user_id' => $this->pemilik->id, 'name' => 'Lain', 'slug' => 'lain', 'category' => 'cafe', 'address' => 'x']);
        $produkLain = DB::table('products')->insertGetId(['store_id' => $lain, 'name' => 'Roti']);

        $this->buat(['type' => 'menu_item', 'pickup_start' => '18:00', 'pickup_end' => '20:00',
            'items' => [['product_id' => $produkLain, 'qty_total' => 1, 'price_rupiah' => 5000]]])
            ->assertJsonValidationErrors('items.0.product_id');
    }

    public function test_hanya_pemilik_yang_bisa_mengelola(): void
    {
        $kasir = User::forceCreate(['phone' => '6281200000002', 'role' => 'partner'])->refresh();
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $kasir->id, 'role' => 'cashier']);
        $orangLain = User::forceCreate(['phone' => '6281200000003', 'role' => 'partner'])->refresh();

        $this->sebagai($kasir)->getJson($this->url('/listings'))->assertForbidden();
        $this->sebagai($orangLain)->getJson($this->url('/listings'))->assertNotFound();
        $this->sebagai($this->pemilik)->getJson('/api/partner/stores/999999/listings')->assertNotFound();
        $this->app['auth']->forgetGuards();
        $this->flushHeaders()->getJson($this->url('/listings'))->assertUnauthorized();
    }

    public function test_m16_tambah_menu_baru_lalu_langsung_bisa_dijual(): void
    {
        $id = $this->sebagai($this->pemilik)->postJson($this->url('/products'), [
            'name' => '  Pisang goreng keju ', 'price_rupiah' => 12000, 'ingredients_text' => 'Pisang, tepung terigu, keju',
        ])
            ->assertCreated()
            ->assertJsonPath('data.name', 'Pisang goreng keju')
            ->assertJsonPath('data.unit', 'pcs')
            ->assertJsonPath('data.price_rupiah', 12000)
            ->json('data.id');

        $this->sebagai($this->pemilik)->getJson($this->url('/products'))->assertJsonPath('data.0.id', $id);

        $this->buat(['type' => 'menu_item', 'pickup_start' => '18:00', 'pickup_end' => '20:00',
            'items' => [['product_id' => $id, 'qty_total' => 5, 'price_rupiah' => 6000, 'allergens' => [['code' => 'gluten']]]]])
            ->assertCreated()
            ->assertJsonPath('data.0.title', 'Pisang goreng keju')
            ->assertJsonPath('data.0.original_value_rupiah', 12000)
            ->assertJsonPath('data.0.ingredients_text', 'Pisang, tepung terigu, keju');
    }

    public function test_m16_menu_baru_divalidasi(): void
    {
        $this->produk('Croissant mentega', 28000);

        $this->sebagai($this->pemilik)->postJson($this->url('/products'), ['name' => 'croissant MENTEGA', 'price_rupiah' => 20000, 'ingredients_text' => 'Tepung'])
            ->assertUnprocessable()->assertJsonValidationErrors('name');
        $this->sebagai($this->pemilik)->postJson($this->url('/products'), ['name' => 'Donat', 'price_rupiah' => 100])
            ->assertJsonValidationErrors(['price_rupiah', 'ingredients_text']);

        $kasir = User::forceCreate(['phone' => '6281200000002', 'role' => 'partner'])->refresh();
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $kasir->id, 'role' => 'cashier']);
        $this->sebagai($kasir)->postJson($this->url('/products'), ['name' => 'Donat', 'price_rupiah' => 8000, 'ingredients_text' => 'Tepung'])
            ->assertForbidden();
        $this->assertSame(1, DB::table('products')->where('store_id', $this->toko)->count());
    }

    private function produk(string $nama, int $harga): int
    {
        return DB::table('products')->insertGetId(['store_id' => $this->toko, 'name' => $nama, 'price_rupiah' => $harga, 'ingredients_text' => 'Tepung, mentega, susu']);
    }

    private function url(string $akhir): string
    {
        return "/api/partner/stores/{$this->toko}{$akhir}";
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }

    private function buat(array $isian): TestResponse
    {
        return $this->sebagai($this->pemilik)->postJson($this->url('/listings'), $isian);
    }

    private function aksi(int $id, string $aksi): TestResponse
    {
        return $this->sebagai($this->pemilik)->postJson($this->url("/listings/{$id}/{$aksi}"));
    }
}
