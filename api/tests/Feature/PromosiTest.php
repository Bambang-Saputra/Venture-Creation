<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\SeederAlergen;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * Iklan mitra (#3): layar Promosikan toko, label "Iklan" di daftar utama K07, dan banner K07.
 */
class PromosiTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private User $kasir;

    private int $toko;

    private int $tokoLain;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(SeederAlergen::class);
        $this->travelTo('2026-10-06 17:00:00');
        $this->pemilik = User::forceCreate(['phone' => '6281299887766', 'role' => 'partner'])->refresh();
        $this->kasir = User::forceCreate(['phone' => '6281322445566', 'role' => 'partner'])->refresh();
        $this->toko = $this->buatToko($this->pemilik->id, 'Rotiku Palmerah', -6.1985, 106.7802);
        $this->tokoLain = $this->buatToko($this->pemilik->id, 'Warung Dekat', -6.2002, 106.7858);
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $this->kasir->id, 'role' => 'cashier', 'invited_at' => now()]);
    }

    public function test_pemilik_membeli_promosi_dengan_tarif_per_hari_tanpa_potong_saldo(): void
    {
        DB::table('store_balances')->insert(['store_id' => $this->toko, 'available_rupiah' => 50000]);

        $this->sebagai($this->pemilik)->getJson($this->url())->assertOk()
            ->assertJsonPath('packages.0.code', 'search_priority')
            ->assertJsonPath('packages.0.price_per_day_rupiah', 5000)
            ->assertJsonPath('packages.1.price_per_day_rupiah', 10000)
            ->assertJsonPath('billing.enabled', false);

        $this->sebagai($this->pemilik)->postJson($this->url(), ['package' => 'home_banner', 'days' => 3, 'headline' => 'Roti sore mulai Rp8.000'])
            ->assertCreated()
            ->assertJsonPath('data.total_rupiah', 30000)
            ->assertJsonPath('data.starts_on', '2026-10-06')
            ->assertJsonPath('data.ends_on', '2026-10-08')
            ->assertJsonPath('data.status', 'active')
            ->assertJsonPath('data.payment_status', 'simulated');

        $this->assertSame(50000, (int) DB::table('store_balances')->where('store_id', $this->toko)->value('available_rupiah'));
        $this->assertSame(0, DB::table('balance_transactions')->count());
    }

    public function test_promosi_paket_sama_disambung_setelah_yang_masih_tayang(): void
    {
        $this->sebagai($this->pemilik)->postJson($this->url(), ['package' => 'search_priority', 'days' => 2])->assertCreated();
        $this->sebagai($this->pemilik)->postJson($this->url(), ['package' => 'search_priority', 'days' => 5])->assertCreated()
            ->assertJsonPath('data.starts_on', '2026-10-08')
            ->assertJsonPath('data.ends_on', '2026-10-12')
            ->assertJsonPath('data.status', 'scheduled')
            ->assertJsonPath('data.headline', null);
    }

    public function test_validasi_dan_hak_akses(): void
    {
        $this->sebagai($this->pemilik)->postJson($this->url(), ['package' => 'pop_up', 'days' => 0])
            ->assertUnprocessable()->assertJsonValidationErrors(['package', 'days']);
        $this->sebagai($this->pemilik)->postJson($this->url(), ['package' => 'home_banner', 'days' => 31])
            ->assertUnprocessable()->assertJsonValidationErrors(['days']);
        $this->sebagai($this->kasir)->postJson($this->url(), ['package' => 'home_banner', 'days' => 1])->assertForbidden();
        $this->sebagai($this->kasir)->getJson($this->url())->assertForbidden();
    }

    public function test_daftar_utama_menaruh_paling_banyak_dua_iklan_di_atas_halaman_pertama(): void
    {
        $biasa = $this->listing($this->tokoLain, ['pickup_end' => now()->setTime(18, 30)]);
        $iklan = [
            $this->listing($this->toko),
            $this->listing($this->toko),
            $this->listing($this->toko),
        ];
        $this->promosi($this->toko, 'search_priority');

        $data = $this->getJson('/api/listings?sponsored=1')->assertOk()->json('data');
        $this->assertSame([$iklan[0], $iklan[1], $biasa, $iklan[2]], array_column($data, 'id'));
        $this->assertSame([true, true, false, false], array_column($data, 'is_sponsored'));

        // Tanpa sponsored=1 (Populer, Tutup kurang dari satu jam) urutannya tidak berubah.
        $data = $this->getJson('/api/listings')->assertOk()->json('data');
        $this->assertSame($biasa, $data[0]['id']);
        $this->assertNotContains(true, array_column($data, 'is_sponsored'));

        // Halaman kedua tidak diberi iklan lagi.
        $data = $this->getJson('/api/listings?sponsored=1&per_page=1&page=2')->assertOk()->json('data');
        $this->assertNotContains(true, array_column($data, 'is_sponsored'));
    }

    public function test_iklan_tetap_tunduk_pada_filter_alergi_dan_tanggal_tayang(): void
    {
        $this->listing($this->toko, alergen: ['kacang_tanah' => 'contains']);
        $this->promosi($this->toko, 'search_priority');

        $data = $this->getJson('/api/listings?sponsored=1&exclude_allergens[]=kacang_tanah')->assertOk()->json('data');
        $this->assertSame([], $data);

        DB::table('store_promotions')->update(['ends_on' => '2026-10-05']);
        $data = $this->getJson('/api/listings?sponsored=1')->assertOk()->json('data');
        $this->assertSame([false], array_column($data, 'is_sponsored'));
    }

    public function test_banner_menunjuk_jualan_termurah_toko_yang_membeli_banner(): void
    {
        $this->listing($this->toko, ['price_rupiah' => 15000]);
        $murah = $this->listing($this->toko, ['price_rupiah' => 9000]);
        $this->listing($this->tokoLain);
        $this->promosi($this->toko, 'home_banner', 'Roti sore mulai Rp9.000');
        $this->promosi($this->tokoLain, 'search_priority');

        $this->getJson('/api/promotions/banners?lat=-6.2&lng=106.78')->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.store_id', $this->toko)
            ->assertJsonPath('data.0.listing_id', $murah)
            ->assertJsonPath('data.0.price_from_rupiah', 9000)
            ->assertJsonPath('data.0.headline', 'Roti sore mulai Rp9.000');

        // Toko yang semua jualannya mengandung alergen pembeli, atau terlalu jauh, tidak tampil.
        DB::table('listing_allergens')->insert(DB::table('listings')->where('store_id', $this->toko)->pluck('id')
            ->map(fn ($id) => ['listing_id' => $id, 'allergen_id' => DB::table('allergens')->where('code', 'susu')->value('id'), 'presence' => 'contains'])->all());
        $this->getJson('/api/promotions/banners?exclude_allergens[]=susu')->assertOk()->assertJsonCount(0, 'data');
        $this->getJson('/api/promotions/banners?lat=-6.9175&lng=107.6191')->assertOk()->assertJsonCount(0, 'data');
    }

    private function url(): string
    {
        return "/api/partner/stores/{$this->toko}/promotions";
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }

    private function buatToko(int $pemilik, string $nama, float $lat, float $lng): int
    {
        return DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik, 'name' => $nama, 'slug' => str($nama)->slug(), 'category' => 'bakery',
            'address' => 'Jl. Uji No. 1', 'latitude' => $lat, 'longitude' => $lng,
        ]);
    }

    private function promosi(int $toko, string $paket, ?string $kalimat = null): void
    {
        DB::table('store_promotions')->insert([
            'store_id' => $toko, 'package' => $paket, 'headline' => $kalimat,
            'starts_on' => '2026-10-06', 'ends_on' => '2026-10-12', 'days' => 7,
            'price_per_day_rupiah' => 5000, 'total_rupiah' => 35000,
        ]);
    }

    /** @param array<string, string> $alergen kode => presence */
    private function listing(int $toko, array $kolom = [], array $alergen = []): int
    {
        $id = DB::table('listings')->insertGetId([
            'store_id' => $toko, 'type' => 'surprise_bag', 'title' => 'Tas Kejutan',
            'price_rupiah' => 18000, 'original_value_rupiah' => 55000, 'qty_total' => 5,
            'pickup_date' => now()->toDateString(), 'pickup_start' => now()->setTime(18, 0),
            'pickup_end' => now()->setTime(19, 0), 'status' => 'active', 'ingredients_text' => 'Tepung terigu, gula',
            ...$kolom,
        ]);

        foreach ($alergen as $kode => $presence) {
            DB::table('listing_allergens')->insert([
                'listing_id' => $id,
                'allergen_id' => DB::table('allergens')->where('code', $kode)->value('id'),
                'presence' => $presence,
            ]);
        }

        return $id;
    }
}
