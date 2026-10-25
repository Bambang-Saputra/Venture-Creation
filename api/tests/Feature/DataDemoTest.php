<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * php artisan demo:segarkan: data demo terasa aktif tanpa menghapus apa pun,
 * dan tanpa menyentuh penguji atau mitra sungguhan.
 */
class DataDemoTest extends TestCase
{
    use RefreshDatabase;

    private int $tokoDemo;

    private int $tokoAsli;

    private int $penguji;

    /** @var list<int> */
    private array $pembeli = [];

    protected function setUp(): void
    {
        parent::setUp();
        $this->travelTo('2026-10-25 20:00:00');

        $pemilik = User::forceCreate(['phone' => '62811000201', 'role' => 'partner'])->refresh();
        $this->tokoDemo = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik->id, 'name' => 'Roti Sari Bakery', 'slug' => 'roti-sari', 'category' => 'bakery', 'address' => 'Jl. Tebet Raya',
        ]);
        DB::table('store_balances')->insert(['store_id' => $this->tokoDemo]);

        $mitraAsli = User::forceCreate(['phone' => '6281299990000', 'role' => 'partner'])->refresh();
        $this->tokoAsli = DB::table('stores')->insertGetId([
            'owner_user_id' => $mitraAsli->id, 'name' => 'Toko Sungguhan', 'slug' => 'toko-sungguhan', 'category' => 'cafe', 'address' => 'Jl. Asli No. 1',
        ]);

        foreach (['62811000101', '62811000102', '62811000103'] as $hp) {
            $this->pembeli[] = User::forceCreate(['phone' => $hp, 'role' => 'consumer'])->id;
        }
        $this->penguji = User::forceCreate(['phone' => '6289670000500', 'role' => 'consumer'])->id;
        $susu = DB::table('allergens')->insertGetId(['code' => 'susu', 'name' => 'Susu']);

        // Jualan terakhir dua hari lalu dan sudah kedaluwarsa.
        foreach ([[$this->tokoDemo, 'Tas Roti Sore'], [$this->tokoDemo, 'Tas Pastry'], [$this->tokoDemo, 'Tas Donat'], [$this->tokoAsli, 'Tas Toko Asli']] as $i => [$toko, $judul]) {
            $id = DB::table('listings')->insertGetId([
                'store_id' => $toko, 'type' => 'surprise_bag', 'title' => $judul, 'price_rupiah' => 15000, 'qty_total' => 5, 'qty_sold' => 3,
                'pickup_date' => '2026-10-23', 'pickup_start' => '2026-10-23 18:00:00', 'pickup_end' => '2026-10-23 23:00:00',
                'status' => 'expired', 'ingredients_text' => 'Tepung, mentega, susu',
            ]);
            if ($i === 0) {
                DB::table('listing_allergens')->insert(['listing_id' => $id, 'allergen_id' => $susu, 'presence' => 'contains']);
            }
        }
    }

    public function test_jualan_disalin_ke_hari_ini_dan_besok_hanya_untuk_toko_demo(): void
    {
        $this->artisan('demo:segarkan')->assertSuccessful();

        $hariIni = DB::table('listings')->where('store_id', $this->tokoDemo)->whereDate('pickup_date', '2026-10-25')->get();
        $besok = DB::table('listings')->where('store_id', $this->tokoDemo)->whereDate('pickup_date', '2026-10-26')->get();
        $this->assertCount(3, $hariIni);
        $this->assertCount(3, $besok);
        $this->assertTrue($hariIni->every(fn ($l) => $l->status === 'active' && $l->pickup_end === '2026-10-25 23:00:00'));
        $this->assertTrue($besok->every(fn ($l) => $l->qty_sold === 0 && $l->qty_reserved === 0));
        // Alergen ikut tersalin (1 asli + 2 salinan), supaya filter alergi tetap bekerja.
        $this->assertSame(3, DB::table('listing_allergens')->count());

        $this->assertSame(1, DB::table('listings')->where('store_id', $this->tokoAsli)->count());
    }

    public function test_pesanan_iklan_dan_ulasan_hanya_dari_akun_demo(): void
    {
        $this->artisan('demo:segarkan')->assertSuccessful();

        $pesanan = DB::table('orders')->whereDate('placed_at', '2026-10-25')->get();
        $this->assertGreaterThan(0, $pesanan->count());
        $this->assertTrue($pesanan->every(fn ($o) => in_array($o->user_id, $this->pembeli, true)));
        $this->assertSame(0, DB::table('orders')->where('user_id', $this->penguji)->count());
        $this->assertSame(0, DB::table('orders')->where('store_id', $this->tokoAsli)->count());
        $this->assertSame($pesanan->count(), DB::table('pickup_codes')->count());

        // Setiap jualan hari ini masih menyisakan stok, supaya tetap tampil di Beranda.
        DB::table('listings')->whereDate('pickup_date', '2026-10-25')->get()
            ->each(fn ($l) => $this->assertGreaterThan($l->qty_sold + $l->qty_reserved, $l->qty_total));

        $selesai = $pesanan->where('status', 'completed')->count();
        $this->assertGreaterThan(0, $selesai);
        $this->assertSame($selesai, DB::table('reviews')->count());
        $this->assertSame(
            $pesanan->where('status', 'completed')->sum('total_rupiah'),
            (int) DB::table('store_balances')->where('store_id', $this->tokoDemo)->value('available_rupiah'),
        );

        $this->assertDatabaseHas('store_promotions', ['store_id' => $this->tokoDemo, 'package' => 'home_banner']);
        $this->assertDatabaseMissing('store_promotions', ['store_id' => $this->tokoAsli]);
    }

    public function test_dijalankan_ulang_tidak_menambah_apa_pun(): void
    {
        $this->artisan('demo:segarkan')->assertSuccessful();
        $sebelum = $this->hitung();

        $this->artisan('demo:segarkan')->assertSuccessful();

        $this->assertSame($sebelum, $this->hitung());
    }

    public function test_jam_ambil_hari_ini_yang_sudah_lewat_diperpanjang_sampai_tutup(): void
    {
        $this->travelTo('2026-10-25 23:10:00');

        $this->artisan('demo:segarkan')->assertSuccessful();

        $this->assertSame(
            ['2026-10-25 23:30:00'],
            DB::table('listings')->where('store_id', $this->tokoDemo)->whereDate('pickup_date', '2026-10-25')->distinct()->pluck('pickup_end')->all(),
        );
        // Jualan besok tetap memakai jam asli, bukan jam hari ini yang diperpanjang.
        $this->assertSame(
            ['2026-10-26 23:00:00'],
            DB::table('listings')->where('store_id', $this->tokoDemo)->whereDate('pickup_date', '2026-10-26')->distinct()->pluck('pickup_end')->all(),
        );
    }

    public function test_lewat_jam_tutup_hanya_jualan_besok_yang_dibuat(): void
    {
        $this->travelTo('2026-10-25 23:40:00');

        $this->artisan('demo:segarkan')->assertSuccessful();

        $this->assertSame(0, DB::table('listings')->whereDate('pickup_date', '2026-10-25')->count());
        $this->assertSame(3, DB::table('listings')->whereDate('pickup_date', '2026-10-26')->count());
        $this->assertSame(0, DB::table('orders')->count());
    }

    public function test_menolak_di_production_tanpa_force(): void
    {
        $this->app['env'] = 'production';

        $this->artisan('demo:segarkan')->assertFailed();

        $this->assertSame(4, DB::table('listings')->count());
        $this->assertSame(0, DB::table('store_promotions')->count());
    }

    /** @return array<string,int> */
    private function hitung(): array
    {
        return collect(['listings', 'orders', 'store_promotions', 'reviews', 'pickup_codes'])
            ->mapWithKeys(fn ($t) => [$t => DB::table($t)->count()])->all();
    }
}
