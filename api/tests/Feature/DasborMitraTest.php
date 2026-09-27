<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * M05 Dashboard mitra dan M08 Saran produksi.
 */
class DasborMitraTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private int $toko;

    private int $croissant;

    private int $danish;

    protected function setUp(): void
    {
        parent::setUp();

        // Jumat, 18 September 2026 pukul 19.00. Saran bawaan untuk Sabtu 19 September.
        $this->travelTo('2026-09-18 19:00:00');
        $this->pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Kopi Kalyan SCBD', 'slug' => 'kopi-kalyan', 'category' => 'cafe', 'address' => 'x',
        ]);
        $this->croissant = DB::table('products')->insertGetId(['store_id' => $this->toko, 'name' => 'Croissant mentega', 'price_rupiah' => 28000, 'daily_production_qty' => 24]);
        $this->danish = DB::table('products')->insertGetId(['store_id' => $this->toko, 'name' => 'Danish keju', 'price_rupiah' => 27000, 'cost_rupiah' => 10000, 'daily_production_qty' => 18]);
    }

    public function test_saran_dari_empat_sabtu_terakhir(): void
    {
        // Sabtu 29 Agu, 5, 12 Sep dicatat; 22 Agu tidak. Croissant sisa 6,6,6; Danish 1,0,2.
        foreach (['2026-08-29' => [6, 1], '2026-09-05' => [6, 0], '2026-09-12' => [6, 2]] as $tgl => [$c, $d]) {
            $this->catat($tgl, [$this->croissant => $c, $this->danish => $d]);
        }

        $data = $this->minta('/suggestions')->assertOk()
            ->assertJsonPath('data.suggested_for_date', '2026-09-19')
            ->assertJsonPath('data.weekday', 'Sabtu')
            ->assertJsonPath('data.sample_days', 3)
            ->assertJsonPath('data.has_enough_data', true)
            ->json('data');

        // Croissant: rata-rata 6, dipangkas floor(6 x 0,75) = 4 -> 24 jadi 20, hemat 4 x 28.000.
        // Danish: rata-rata 1, floor(0,75) = 0 -> tidak ada saran.
        $this->assertCount(1, $data['items']);
        $this->assertSame(['Croissant mentega', 24, 20, 4, 112000], [
            $data['items'][0]['name'], $data['items'][0]['current_production'], $data['items'][0]['suggested_production'],
            $data['items'][0]['reduce_by'], $data['items'][0]['estimated_saving_per_week_rupiah'],
        ]);
        $this->assertSame(112000, $data['total_saving_per_week_rupiah']);

        $id = $data['items'][0]['id'];
        $this->sebagai()->postJson($this->url("/suggestions/{$id}/accept"))->assertOk()->assertJsonPath('data.status', 'accepted');
        // Dihitung ulang: status tetap.
        $this->minta('/suggestions')->assertJsonPath('data.items.0.status', 'accepted');
        $this->sebagai()->postJson($this->url('/suggestions/999999/accept'))->assertNotFound();
    }

    public function test_data_kurang_dari_tiga_hari_tidak_memberi_saran(): void
    {
        $this->catat('2026-09-12', [$this->croissant => 10]);

        $this->minta('/suggestions')->assertOk()
            ->assertJsonPath('data.has_enough_data', false)
            ->assertJsonCount(0, 'data.items');
        $this->assertDatabaseCount('production_suggestions', 0);
    }

    public function test_ubah_produksi_harian(): void
    {
        $this->sebagai()->patchJson($this->url("/products/{$this->croissant}"), ['daily_production_qty' => 30])
            ->assertOk()->assertJsonPath('data.daily_production_qty', 30);
        $this->sebagai()->patchJson($this->url('/products/999999'), ['daily_production_qty' => 30])->assertNotFound();
    }

    public function test_ringkasan_dashboard(): void
    {
        DB::table('store_hours')->insert(['store_id' => $this->toko, 'day_of_week' => 5, 'open_time' => '07:00', 'close_time' => '21:00']);
        $this->catat('2026-09-15', [$this->croissant => 2], 500);
        $this->catat('2026-09-17', [$this->croissant => 5], 1500);
        DB::table('listings')->insert(['store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas', 'price_rupiah' => 20000,
            'qty_total' => 10, 'qty_sold' => 2, 'pickup_date' => '2026-09-17', 'pickup_start' => '2026-09-17 18:00',
            'pickup_end' => '2026-09-17 20:00', 'status' => 'expired', 'ingredients_text' => 'Tepung']);

        $this->minta('/summary')->assertOk()
            ->assertJsonPath('data.store.name', 'Kopi Kalyan SCBD')
            ->assertJsonPath('data.is_open', true)
            ->assertJsonPath('data.closes_at', '21:00')
            ->assertJsonPath('data.minutes_until_close', 120)
            ->assertJsonPath('data.is_today_logged', false)
            ->assertJsonPath('data.avg_daily_waste_gram', 1000)
            ->assertJsonPath('data.unsold_bags_last_7_days', 8)
            ->assertJsonPath('data.peak_day', 'Kamis')
            ->assertJsonPath('data.daily.3.wasted_value_rupiah', 140000);
    }

    private function catat(string $tanggal, array $qtyPerProduk, int $gram = 0): void
    {
        $nilai = collect($qtyPerProduk)->map(fn ($q, $id) => $q * DB::table('products')->where('id', $id)->value('price_rupiah'))->sum();
        $log = DB::table('waste_logs')->insertGetId(['store_id' => $this->toko, 'log_date' => $tanggal, 'total_value_rupiah' => $nilai,
            'total_weight_gram' => $gram, 'recorded_by_user_id' => $this->pemilik->id]);
        foreach ($qtyPerProduk as $id => $q) {
            DB::table('waste_log_items')->insert(['waste_log_id' => $log, 'product_id' => $id, 'label' => 'x', 'qty' => $q]);
        }
    }

    private function minta(string $akhir): TestResponse
    {
        return $this->sebagai()->getJson($this->url($akhir));
    }

    private function url(string $akhir): string
    {
        return "/api/partner/stores/{$this->toko}{$akhir}";
    }

    private function sebagai(): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($this->pemilik->createToken('uji')->plainTextToken);
    }
}
