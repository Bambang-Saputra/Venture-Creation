<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * M06/M19 Catat sisa dan M07 Laporan mingguan.
 */
class CatatSisaTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private User $kasir;

    private int $toko;

    private int $croissant;

    private int $kopi;

    protected function setUp(): void
    {
        parent::setUp();

        // Kamis, 17 September 2026 seperti di M06.
        $this->travelTo('2026-09-17 21:00:00');
        $this->pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner'])->refresh();
        $this->kasir = User::forceCreate(['phone' => '6281200000002', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Bakerman', 'slug' => 'bakerman', 'category' => 'bakery', 'address' => 'x',
        ]);
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $this->kasir->id, 'role' => 'cashier']);
        $this->croissant = DB::table('products')->insertGetId(['store_id' => $this->toko, 'name' => 'Croissant mentega', 'price_rupiah' => 28000, 'cost_rupiah' => 12000, 'weight_gram' => 80]);
        $this->kopi = DB::table('products')->insertGetId(['store_id' => $this->toko, 'name' => 'Kopi susu botol', 'price_rupiah' => 25000]);
    }

    public function test_kasir_mencatat_per_item_nilai_memakai_hpp(): void
    {
        $this->simpan($this->kasir, ['method' => 'per_item', 'items' => [
            ['product_id' => $this->croissant, 'qty' => 6],
            ['product_id' => $this->kopi, 'qty' => 5],
            ['product_id' => null, 'label' => 'Roti sisa etalase', 'qty' => 2, 'unit_value_rupiah' => 5000],
            ['product_id' => null, 'label' => 'Kue lapis', 'qty' => 1, 'unit_value_rupiah' => 4000],
        ]])
            ->assertOk()
            // 6 x HPP 12.000 + 5 x harga jual 25.000 (tanpa HPP) + 2 x 5.000 + 4.000
            ->assertJsonPath('data.total_value_rupiah', 72000 + 125000 + 10000 + 4000)
            ->assertJsonPath('data.total_items', 14)
            ->assertJsonPath('data.total_weight_gram', 480)
            ->assertJsonPath('data.products.0.qty', 6)
            ->assertJsonCount(2, 'data.other_items');

        // HPP naik bulan depan tidak mengubah catatan lama.
        DB::table('products')->where('id', $this->croissant)->update(['cost_rupiah' => 20000]);
        $this->sebagai($this->kasir)->getJson($this->url('/waste-logs?date=2026-09-17'))->assertJsonPath('data.total_value_rupiah', 211000);
        $this->assertDatabaseHas('audit_logs', ['action' => 'waste_log.save', 'store_id' => $this->toko]);
    }

    public function test_simpan_ulang_mengganti_isi_hari_itu_dan_hanya_discarded_yang_dihitung(): void
    {
        $this->simpan($this->pemilik, ['method' => 'per_item', 'items' => [['product_id' => $this->croissant, 'qty' => 6]]]);
        $this->simpan($this->pemilik, ['method' => 'per_item', 'items' => [
            ['product_id' => $this->croissant, 'qty' => 2],
            ['product_id' => $this->kopi, 'qty' => 3, 'disposition' => 'donated'],
            ['product_id' => null, 'label' => 'Kosong', 'qty' => 0],
        ]])->assertOk()->assertJsonPath('data.total_value_rupiah', 24000);

        $this->assertDatabaseCount('waste_logs', 1);
        $this->assertDatabaseCount('waste_log_items', 2);
    }

    public function test_m19_timbang_nilai_dari_berat_per_satuan(): void
    {
        $this->simpan($this->pemilik, ['method' => 'weight', 'items' => [['product_id' => $this->croissant, 'weight_gram' => 400]]])
            ->assertOk()
            ->assertJsonPath('data.method', 'weight')
            ->assertJsonPath('data.total_weight_gram', 400)
            ->assertJsonPath('data.total_value_rupiah', 60000);
    }

    public function test_perbandingan_dengan_hari_yang_sama_pekan_lalu(): void
    {
        $this->simpan($this->pemilik, ['log_date' => '2026-09-10', 'method' => 'per_item', 'items' => [['product_id' => $this->croissant, 'qty' => 10]]])
            ->assertStatus(409);

        DB::table('waste_logs')->insert(['store_id' => $this->toko, 'log_date' => '2026-09-10', 'total_value_rupiah' => 100000, 'recorded_by_user_id' => $this->pemilik->id]);
        $this->simpan($this->pemilik, ['method' => 'per_item', 'items' => [['product_id' => $this->kopi, 'qty' => 4]]])
            ->assertJsonPath('data.change_vs_last_week_percent', 0);
    }

    public function test_tanggal_mendatang_ditolak_kemarin_masih_boleh(): void
    {
        $isi = ['method' => 'per_item', 'items' => [['product_id' => $this->kopi, 'qty' => 1]]];

        $this->simpan($this->pemilik, [...$isi, 'log_date' => '2026-09-18'])->assertJsonValidationErrors('log_date');
        $this->simpan($this->pemilik, [...$isi, 'log_date' => '2026-09-16'])->assertOk()->assertJsonPath('data.is_locked', false);
        $this->simpan($this->pemilik, [...$isi, 'log_date' => '2026-09-15'])->assertStatus(409);
    }

    public function test_produk_toko_lain_ditolak(): void
    {
        $lain = DB::table('stores')->insertGetId(['owner_user_id' => $this->pemilik->id, 'name' => 'Lain', 'slug' => 'lain', 'category' => 'cafe', 'address' => 'x']);
        $asing = DB::table('products')->insertGetId(['store_id' => $lain, 'name' => 'Asing']);

        $this->simpan($this->pemilik, ['method' => 'per_item', 'items' => [['product_id' => $asing, 'qty' => 1]]])
            ->assertJsonValidationErrors('items.0.product_id');
    }

    public function test_laporan_mingguan_sisa_terselamatkan_dan_produk_teratas(): void
    {
        // Minggu 14-20 September 2026: Senin 14 sampai Kamis 17.
        $this->simpan($this->pemilik, ['log_date' => '2026-09-16', 'method' => 'per_item', 'items' => [['product_id' => $this->kopi, 'qty' => 2]]]);
        $this->simpan($this->pemilik, ['method' => 'per_item', 'items' => [['product_id' => $this->croissant, 'qty' => 6], ['product_id' => $this->kopi, 'qty' => 1]]]);
        DB::table('waste_logs')->insert(['store_id' => $this->toko, 'log_date' => '2026-09-09', 'total_value_rupiah' => 200000, 'recorded_by_user_id' => $this->pemilik->id]);

        $pembeli = User::forceCreate(['phone' => '6281234567890', 'role' => 'consumer']);
        $listing = DB::table('listings')->insertGetId(['store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas', 'price_rupiah' => 20000,
            'qty_total' => 5, 'qty_sold' => 3, 'pickup_date' => '2026-09-15', 'pickup_start' => '2026-09-15 18:00', 'pickup_end' => '2026-09-15 20:00',
            'status' => 'expired', 'ingredients_text' => 'Tepung']);
        $order = DB::table('orders')->insertGetId(['code' => 'LOF-AAAAAA', 'user_id' => $pembeli->id, 'store_id' => $this->toko, 'status' => 'completed',
            'subtotal_rupiah' => 60000, 'total_rupiah' => 60000, 'pickup_start' => '2026-09-15 18:00', 'pickup_end' => '2026-09-15 20:00',
            'placed_at' => '2026-09-15 12:00', 'completed_at' => '2026-09-15 18:30']);
        DB::table('order_items')->insert(['order_id' => $order, 'listing_id' => $listing, 'title_snapshot' => 'Tas', 'unit_price_rupiah' => 20000, 'qty' => 3, 'line_total_rupiah' => 60000]);

        $this->sebagai($this->pemilik)->getJson($this->url('/reports/weekly'))
            ->assertOk()
            ->assertJsonPath('data.week_start', '2026-09-14')
            ->assertJsonPath('data.week_end', '2026-09-20')
            ->assertJsonPath('data.wasted_value_rupiah', 50000 + 72000 + 25000)
            ->assertJsonPath('data.rescued_value_rupiah', 60000)
            ->assertJsonPath('data.items_sold', 3)
            ->assertJsonPath('data.unsold_value_rupiah', 147000 + 60000)
            ->assertJsonPath('data.logged_days', 2)
            ->assertJsonPath('data.wasted_change_percent', -27)
            ->assertJsonPath('data.top_wasted_products.0.label', 'Kopi susu botol')
            ->assertJsonPath('data.top_wasted_products.0.qty', 3)
            ->assertJsonPath('data.daily.2.wasted_value_rupiah', 50000)
            ->assertJsonPath('data.daily.0.wasted_value_rupiah', null);

        $this->assertDatabaseHas('weekly_reports', ['store_id' => $this->toko, 'week_start' => '2026-09-14', 'wasted_value_rupiah' => 147000]);
    }

    public function test_laporan_hanya_untuk_pemilik(): void
    {
        $this->sebagai($this->kasir)->getJson($this->url('/reports/weekly'))->assertForbidden();
    }

    public function test_hari_belum_dicatat_terisi_dari_jualan_yang_tidak_laku(): void
    {
        $jualan = fn (array $isi) => DB::table('listings')->insertGetId(array_merge([
            'store_id' => $this->toko, 'pickup_date' => '2026-09-17',
            'pickup_start' => '2026-09-17 19:00:00', 'pickup_end' => '2026-09-17 21:00:00',
            'status' => 'expired', 'ingredients_text' => 'Tepung, mentega, telur',
        ], $isi));
        // Menu croissant: dipasang 6, laku 2, jadi sisa 4.
        $jualan(['type' => 'menu_item', 'product_id' => $this->croissant, 'title' => 'Croissant mentega', 'price_rupiah' => 14000, 'qty_total' => 6, 'qty_sold' => 2]);
        // Tas kejutan: dipasang 5, laku 3, 1 masih dipesan, jadi sisa 1.
        $jualan(['type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000, 'qty_total' => 5, 'qty_sold' => 3, 'qty_reserved' => 1]);
        // Habis terjual, draf, dan jualan hari lain tidak ikut.
        $jualan(['type' => 'menu_item', 'product_id' => $this->kopi, 'title' => 'Kopi susu', 'price_rupiah' => 12000, 'qty_total' => 3, 'qty_sold' => 3]);
        $jualan(['type' => 'surprise_bag', 'title' => 'Draf', 'price_rupiah' => 10000, 'qty_total' => 2, 'status' => 'draft']);
        $jualan(['type' => 'menu_item', 'product_id' => $this->kopi, 'title' => 'Kopi kemarin', 'price_rupiah' => 12000, 'qty_total' => 5,
            'pickup_date' => '2026-09-16', 'pickup_start' => '2026-09-16 19:00:00', 'pickup_end' => '2026-09-16 21:00:00']);

        $r = $this->sebagai($this->kasir)->getJson($this->url('/waste-logs?date=2026-09-17'))->assertOk()
            ->assertJsonPath('data.is_recorded', false)
            ->assertJsonPath('data.prefilled_from_listings', 2)
            ->assertJsonCount(1, 'data.other_items')
            ->assertJsonPath('data.other_items.0.label', 'Tas Pastry Sore')
            ->assertJsonPath('data.other_items.0.qty', 1)
            ->assertJsonPath('data.other_items.0.value_rupiah', 18000);
        $produk = collect($r->json('data.products'))->keyBy('product_id');
        $this->assertSame(4, $produk[$this->croissant]['qty']);
        $this->assertSame(0, $produk[$this->kopi]['qty']);

        // Setelah disimpan, yang tampil catatan mitra, bukan isian otomatis lagi.
        $this->simpan($this->kasir, ['method' => 'per_item', 'items' => [['product_id' => $this->croissant, 'qty' => 3]]])
            ->assertOk()
            ->assertJsonPath('data.prefilled_from_listings', 0)
            ->assertJsonPath('data.products.0.qty', 3)
            ->assertJsonCount(0, 'data.other_items');
    }

    public function test_menu_dari_produk_nonaktif_tetap_tercatat_sebagai_item_lain(): void
    {
        DB::table('products')->where('id', $this->kopi)->update(['is_active' => false]);
        DB::table('listings')->insert([
            'store_id' => $this->toko, 'type' => 'menu_item', 'product_id' => $this->kopi, 'title' => 'Kopi susu botol',
            'price_rupiah' => 12000, 'qty_total' => 2, 'pickup_date' => '2026-09-17', 'status' => 'expired',
            'pickup_start' => '2026-09-17 19:00:00', 'pickup_end' => '2026-09-17 21:00:00', 'ingredients_text' => 'Kopi, susu',
        ]);

        $this->sebagai($this->pemilik)->getJson($this->url('/waste-logs?date=2026-09-17'))->assertOk()
            ->assertJsonPath('data.prefilled_from_listings', 1)
            ->assertJsonPath('data.other_items.0.label', 'Kopi susu botol')
            ->assertJsonPath('data.other_items.0.value_rupiah', 24000);

        // Tiga hari kemudian tanggal itu sudah terkunci: tidak ada isian yang tidak bisa disimpan.
        $this->travelTo('2026-09-20 10:00:00');
        $this->sebagai($this->pemilik)->getJson($this->url('/waste-logs?date=2026-09-17'))->assertOk()
            ->assertJsonPath('data.is_locked', true)
            ->assertJsonPath('data.prefilled_from_listings', 0)
            ->assertJsonCount(0, 'data.other_items');
    }

    private function simpan(User $user, array $isi): TestResponse
    {
        return $this->sebagai($user)->postJson($this->url('/waste-logs'), $isi);
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
}
