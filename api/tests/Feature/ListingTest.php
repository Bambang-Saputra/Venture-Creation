<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\SeederAlergen;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * GET /api/listings dan GET /api/listings/{id} (K07-K11).
 */
class ListingTest extends TestCase
{
    use RefreshDatabase;

    // Titik uji di SCBD. Toko "dekat" sekitar 0,5 km, "jauh" sekitar 8 km.
    private const LAT = -6.2250;

    private const LNG = 106.8090;

    private int $tokoDekat;

    private int $tokoJauh;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(SeederAlergen::class);
        $this->travelTo(now()->setTime(17, 0));

        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $this->tokoDekat = $this->toko($pemilik->id, 'Kopi Kalyan', 'cafe', -6.2270, 106.8120);
        $this->tokoJauh = $this->toko($pemilik->id, 'Bakerman Blok M', 'bakery', -6.2440, 106.7400);
    }

    public function test_hanya_jualan_yang_bisa_dibeli_yang_tampil(): void
    {
        $tampil = $this->listing($this->tokoDekat, ['title' => 'Tas Pastry Sore']);
        $this->listing($this->tokoDekat, ['status' => 'draft', 'ingredients_text' => null]);
        $this->listing($this->tokoDekat, ['status' => 'paused']);
        $this->listing($this->tokoDekat, ['qty_total' => 2, 'qty_sold' => 2]);
        $this->listing($this->tokoDekat, ['pickup_start' => now()->subHours(3), 'pickup_end' => now()->subMinute()]);
        DB::table('stores')->where('id', $this->tokoJauh)->update(['is_temporarily_closed' => true]);
        $this->listing($this->tokoJauh);

        $this->cari()->assertOk()->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $tampil)
            ->assertJsonPath('data.0.store.name', 'Kopi Kalyan')
            ->assertJsonPath('data.0.qty_remaining', 5)
            ->assertJsonPath('data.0.minutes_until_end', 120);
    }

    public function test_urut_jarak_dan_radius(): void
    {
        $jauh = $this->listing($this->tokoJauh);
        $dekat = $this->listing($this->tokoDekat);

        $data = $this->cari(['lat' => self::LAT, 'lng' => self::LNG])->assertOk()->json('data');
        $this->assertSame([$dekat, $jauh], array_column($data, 'id'));
        $this->assertLessThan(1, $data[0]['distance_km']);

        $this->cari(['lat' => self::LAT, 'lng' => self::LNG, 'radius_km' => 1])
            ->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $dekat);
    }

    public function test_alergen_yang_dihindari_disembunyikan_termasuk_mungkin_mengandung(): void
    {
        $kacang = $this->listing($this->tokoDekat, alergen: ['kacang_tanah' => 'contains']);
        $mungkin = $this->listing($this->tokoDekat, alergen: ['kacang_tanah' => 'may_contain']);
        $susu = $this->listing($this->tokoDekat, alergen: ['susu' => 'contains']);

        $this->cari()->assertJsonCount(3, 'data');
        $data = $this->cari(['exclude_allergens' => ['kacang_tanah']])->assertOk()->json('data');

        $this->assertSame([$susu], array_column($data, 'id'));
        $this->assertSame('susu', $data[0]['allergens'][0]['code']);
        $this->assertNotContains($kacang, array_column($data, 'id'));
        $this->assertNotContains($mungkin, array_column($data, 'id'));
    }

    public function test_filter_jenis_kategori_halal_cari_dan_jam_ambil(): void
    {
        $tas = $this->listing($this->tokoDekat, ['title' => 'Tas Pastry Sore', 'halal_label' => 'certified']);
        $menu = $this->listing($this->tokoJauh, [
            'type' => 'menu_item', 'title' => 'Roti Hari Ini',
            'pickup_start' => now()->setTime(20, 0), 'pickup_end' => now()->setTime(22, 0),
        ]);

        $this->cari(['type' => ['menu_item']])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $menu);
        $this->cari(['category' => 'cafe'])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $tas);
        $this->cari(['halal' => 1])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $tas);
        $this->cari(['q' => 'bakerman'])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $menu);
        $this->cari(['q' => '100%'])->assertJsonCount(0, 'data');
        $this->cari(['pickup_from' => '20:00', 'pickup_until' => '22:00'])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $menu);
        $this->cari(['ends_within_minutes' => 60 * 3])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $tas);
        // K10 "Lihat menu satuan" dan K11: jualan satu toko saja.
        $this->cari(['store_id' => $this->tokoJauh])->assertJsonCount(1, 'data')->assertJsonPath('data.0.id', $menu);
        $this->cari(['store_id' => $this->tokoDekat, 'type' => ['menu_item']])->assertJsonCount(0, 'data');
    }

    public function test_detail_memuat_isi_toko_dan_ketersediaan(): void
    {
        $id = $this->listing($this->tokoDekat, ['type' => 'menu_item', 'content_hint' => '2 roti manis'], ['susu' => 'contains']);
        DB::table('listing_items')->insert(['listing_id' => $id, 'label' => 'Croissant', 'qty' => 2]);
        DB::table('store_hours')->insert(['store_id' => $this->tokoDekat, 'day_of_week' => now()->dayOfWeek, 'open_time' => '07:00', 'close_time' => '21:00']);

        $this->getJson("/api/listings/{$id}")->assertOk()
            ->assertJsonPath('data.is_available', true)
            ->assertJsonPath('data.content_hint', '2 roti manis')
            ->assertJsonPath('data.items.0.label', 'Croissant')
            ->assertJsonPath('data.allergens.0.code', 'susu')
            ->assertJsonPath('data.store.address', 'Jl. Uji No. 1')
            ->assertJsonPath('data.store.hours_today.close_time', '21:00:00');
    }

    public function test_detail_habis_tetap_bisa_dibuka_draft_tidak(): void
    {
        $habis = $this->listing($this->tokoDekat, ['qty_total' => 1, 'qty_sold' => 1, 'status' => 'sold_out']);
        $draft = $this->listing($this->tokoDekat, ['status' => 'draft']);

        $this->getJson("/api/listings/{$habis}")->assertOk()->assertJsonPath('data.is_available', false);
        $this->getJson("/api/listings/{$draft}")->assertNotFound();
        $this->getJson('/api/listings/999999')->assertNotFound();
    }

    public function test_parameter_tidak_valid_ditolak(): void
    {
        $this->cari(['type' => ['voucher']])->assertUnprocessable()->assertJsonValidationErrors('type.0');
        $this->cari(['radius_km' => 3])->assertUnprocessable()->assertJsonValidationErrors(['lat', 'lng']);
        $this->cari(['pickup_from' => '8 malam'])->assertUnprocessable();
    }

    private function cari(array $query = []): TestResponse
    {
        return $this->getJson('/api/listings?'.http_build_query($query));
    }

    private function toko(int $pemilik, string $nama, string $kategori, float $lat, float $lng): int
    {
        return DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik, 'name' => $nama, 'slug' => str($nama)->slug(), 'category' => $kategori,
            'address' => 'Jl. Uji No. 1', 'latitude' => $lat, 'longitude' => $lng,
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
