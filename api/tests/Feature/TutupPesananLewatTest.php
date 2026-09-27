<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * php artisan pesanan:tutup-yang-lewat
 */
class TutupPesananLewatTest extends TestCase
{
    use RefreshDatabase;

    private User $pembeli;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();

        $this->travelTo(now()->setTime(17, 0));
        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $this->pembeli = User::forceCreate(['phone' => '6281234567890', 'role' => 'consumer'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik->id, 'name' => 'Kopi Kalyan', 'slug' => 'kopi-kalyan', 'category' => 'cafe', 'address' => 'Jl. Uji',
        ]);
    }

    public function test_pesanan_tak_diambil_jadi_no_show_setelah_toleransi(): void
    {
        $listing = $this->listing(qty: 2);
        $pesanan = $this->pesan($listing, 2);
        $this->assertSame('sold_out', DB::table('listings')->where('id', $listing)->value('status'));

        // 20.20: jam ambil lewat, masih dalam toleransi 30 menit.
        $this->travelTo(now()->setTime(20, 20));
        $this->artisan('pesanan:tutup-yang-lewat')->assertSuccessful();
        $this->assertSame('pending_pickup', DB::table('orders')->where('id', $pesanan)->value('status'));
        $this->assertSame('expired', DB::table('listings')->where('id', $listing)->value('status'));

        $this->travelTo(now()->setTime(20, 31));
        $this->artisan('pesanan:tutup-yang-lewat')->expectsOutputToContain('1 pesanan jadi no_show')->assertSuccessful();

        $this->assertSame('no_show', DB::table('orders')->where('id', $pesanan)->value('status'));
        $this->assertSame('expired', DB::table('pickup_codes')->where('order_id', $pesanan)->value('status'));
        $this->assertSame(0, DB::table('listings')->where('id', $listing)->value('qty_reserved'));
        $this->assertSame(0, DB::table('store_balances')->where('store_id', $this->toko)->value('pending_rupiah'));

        // Dijalankan lagi tidak mengubah apa pun.
        $this->artisan('pesanan:tutup-yang-lewat')->expectsOutputToContain('0 pesanan jadi no_show')->assertSuccessful();
    }

    public function test_pesanan_selesai_dan_listing_mendatang_tidak_disentuh(): void
    {
        $sore = $this->listing(qty: 3);
        $besok = $this->listing(qty: 3, mulai: now()->addDay()->setTime(18, 0), akhir: now()->addDay()->setTime(20, 0));
        $selesai = $this->pesan($sore, 1);
        DB::table('orders')->where('id', $selesai)->update(['status' => 'completed']);

        $this->travelTo(now()->setTime(21, 0));
        $this->artisan('pesanan:tutup-yang-lewat')->assertSuccessful();

        $this->assertSame('completed', DB::table('orders')->where('id', $selesai)->value('status'));
        $this->assertSame('active', DB::table('listings')->where('id', $besok)->value('status'));
    }

    private function listing(int $qty, $mulai = null, $akhir = null): int
    {
        return DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas', 'price_rupiah' => 18000, 'qty_total' => $qty,
            'pickup_date' => ($mulai ?? now())->toDateString(), 'pickup_start' => $mulai ?? now()->setTime(18, 0),
            'pickup_end' => $akhir ?? now()->setTime(20, 0), 'status' => 'active', 'ingredients_text' => 'Tepung, gula',
        ]);
    }

    private function pesan(int $listing, int $qty): int
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($this->pembeli->createToken('uji')->plainTextToken)
            ->postJson('/api/orders', ['items' => [['listing_id' => $listing, 'qty' => $qty]]])
            ->assertCreated()->json('data.id');
    }
}
