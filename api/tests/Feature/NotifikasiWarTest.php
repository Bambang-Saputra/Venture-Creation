<?php

namespace Tests\Feature;

use App\Models\User;
use App\Services\Notifikasi;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * Notifikasi "War dimulai!" (perintah notifikasi:war).
 */
class NotifikasiWarTest extends TestCase
{
    use RefreshDatabase;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();
        $this->travelTo(now()->setTime(18, 0));
        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik->id, 'name' => 'Roti Sari Bakery', 'slug' => 'roti-sari',
            'category' => 'bakery', 'address' => 'Jl. Tebet Raya No. 12',
        ]);
    }

    public function test_hanya_ke_pembeli_yang_menyalakan_promo(): void
    {
        $mau = $this->pembeli('6281234500001', true);
        $tidak = $this->pembeli('6281234500002', false);
        $this->listing(['title' => 'Tas Roti Sore', 'price_rupiah' => 21000, 'original_value_rupiah' => 60000]);

        $this->assertSame(1, app(Notifikasi::class)->warDimulai());

        $n = DB::table('notifications')->where('user_id', $mau)->first();
        $this->assertSame('war_dimulai', $n->type);
        $this->assertSame('War dimulai! Roti Sari Bakery diskon 65%', $n->title);
        $this->assertStringContainsString('Tas Roti Sore cuma Rp21.000', $n->body);
        $this->assertSame(0, DB::table('notifications')->where('user_id', $tidak)->count());
    }

    public function test_tidak_mengulang_jualan_yang_sama_dan_maks_dua_sehari(): void
    {
        $pembeli = $this->pembeli('6281234500001', true);
        $this->listing(['price_rupiah' => 10000, 'original_value_rupiah' => 40000]);

        app(Notifikasi::class)->warDimulai();
        app(Notifikasi::class)->warDimulai();
        $this->assertSame(1, DB::table('notifications')->where('user_id', $pembeli)->count());

        $this->listing(['price_rupiah' => 12000, 'original_value_rupiah' => 30000]);
        app(Notifikasi::class)->warDimulai();
        $this->listing(['price_rupiah' => 15000, 'original_value_rupiah' => 30000]);
        app(Notifikasi::class)->warDimulai();
        $this->assertSame(2, DB::table('notifications')->where('user_id', $pembeli)->count());
    }

    public function test_diskon_kecil_atau_sudah_lewat_tidak_dikirim(): void
    {
        $this->pembeli('6281234500001', true);
        $this->listing(['price_rupiah' => 18000, 'original_value_rupiah' => 20000]);
        $this->listing(['price_rupiah' => 5000, 'original_value_rupiah' => 30000, 'pickup_end' => now()->addMinutes(10)]);

        $this->assertSame(0, app(Notifikasi::class)->warDimulai());
    }

    private function pembeli(string $hp, bool $promo): int
    {
        $u = User::forceCreate(['phone' => $hp, 'role' => 'consumer']);
        DB::table('consumer_profiles')->insert(['user_id' => $u->id, 'notify_promo' => $promo]);

        return $u->id;
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
