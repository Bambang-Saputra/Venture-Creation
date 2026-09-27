<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * K16 Favorit, K17 Notifikasi, K20 Hapus akun.
 */
class AkunKonsumenTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private User $kasir;

    private User $pembeli;

    private int $toko;

    private int $listing;

    protected function setUp(): void
    {
        parent::setUp();

        // Jumat 18 September 2026, 17.00.
        $this->travelTo('2026-09-18 17:00:00');
        $this->pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner'])->refresh();
        $this->kasir = User::forceCreate(['phone' => '6281200000002', 'role' => 'partner'])->refresh();
        $this->pembeli = User::forceCreate(['phone' => '6281234567890', 'role' => 'consumer', 'name' => 'Dara Renata', 'email' => 'dara@contoh.id'])->refresh();

        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Kopi Kalyan', 'slug' => 'kopi-kalyan', 'category' => 'cafe', 'address' => 'Jl. Uji',
        ]);
        DB::table('store_hours')->insert(['store_id' => $this->toko, 'day_of_week' => 5, 'open_time' => '07:00', 'close_time' => '21:00']);
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $this->kasir->id, 'role' => 'cashier']);
        $this->listing = $this->listingBaru('active', 4);
    }

    public function test_favorit_tambah_daftar_dan_hapus(): void
    {
        $this->sebagai($this->pembeli)->postJson('/api/favorites', ['store_id' => $this->toko])->assertCreated();
        // Mengirim ulang tidak menggandakan.
        $this->sebagai($this->pembeli)->postJson('/api/favorites', ['store_id' => $this->toko])->assertOk();
        $this->assertSame(1, DB::table('favorites')->count());

        $this->sebagai($this->pembeli)->getJson('/api/favorites')->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Kopi Kalyan')
            ->assertJsonPath('data.0.closes_at', '21:00')
            ->assertJsonPath('data.0.available_bags', 4)
            ->assertJsonPath('data.0.has_menu_available', false)
            ->assertJsonPath('data.0.usual_publish_time', '16:00');

        $this->sebagai($this->pembeli)->deleteJson("/api/favorites/{$this->toko}")->assertNoContent();
        $this->sebagai($this->pembeli)->getJson('/api/favorites')->assertOk()->assertJsonCount(0, 'data');
    }

    public function test_favorit_ditolak_untuk_mitra_dan_toko_tidak_ada(): void
    {
        $this->sebagai($this->pemilik)->getJson('/api/favorites')->assertForbidden();
        $this->sebagai($this->pembeli)->postJson('/api/favorites', ['store_id' => 99999])->assertNotFound();
    }

    public function test_pesanan_baru_memberi_tahu_pemilik_dan_kasir(): void
    {
        $this->pesan();

        foreach ([$this->pemilik, $this->kasir] as $staf) {
            $this->sebagai($staf)->getJson('/api/notifications')->assertOk()
                ->assertJsonPath('unread_count', 1)
                ->assertJsonPath('data.0.type', 'pesanan_baru')
                ->assertJsonPath('data.0.data.screen', 'M11');
        }
        $this->assertSame(0, DB::table('notifications')->where('user_id', $this->pembeli->id)->count());
    }

    public function test_tukar_kode_memberi_tahu_porsi_terselamatkan(): void
    {
        $kode = $this->pesan(2)['pickup_code'];
        $this->sebagai($this->kasir)->postJson('/api/pickup-codes/redeem', ['store_id' => $this->toko, 'code' => $kode])->assertOk();

        $this->sebagai($this->pembeli)->getJson('/api/notifications')->assertOk()
            ->assertJsonPath('data.0.title', '2 porsi terselamatkan')
            ->assertJsonPath('data.0.body', 'Pesanan Tas Pastry Sore di Kopi Kalyan selesai. Totalmu sekarang 2 porsi.');
    }

    public function test_mitra_favorit_memasang_paling_banyak_sekali_sehari_dan_mengikuti_sakelar(): void
    {
        $lain = User::forceCreate(['phone' => '6281234567891', 'role' => 'consumer'])->refresh();
        DB::table('favorites')->insert([['user_id' => $this->pembeli->id, 'store_id' => $this->toko], ['user_id' => $lain->id, 'store_id' => $this->toko]]);
        DB::table('consumer_profiles')->insert(['user_id' => $lain->id, 'notify_favorite_store' => false]);

        foreach ([$this->listingBaru('draft'), $this->listingBaru('draft')] as $id) {
            $this->sebagai($this->pemilik)->postJson("/api/partner/stores/{$this->toko}/listings/{$id}/publish")->assertOk();
        }

        $this->assertSame(1, DB::table('notifications')->where('user_id', $this->pembeli->id)->where('type', 'mitra_favorit_memasang')->count());
        $this->assertSame(0, DB::table('notifications')->where('user_id', $lain->id)->count());

        // Hari berikutnya boleh lagi.
        $this->travelTo('2026-09-19 12:00:00');
        $this->sebagai($this->pemilik)->postJson("/api/partner/stores/{$this->toko}/listings/{$this->listingBaru('draft')}/publish")->assertOk();
        $this->assertSame(2, DB::table('notifications')->where('user_id', $this->pembeli->id)->count());
    }

    public function test_pengingat_ambil_sekali_per_pesanan(): void
    {
        $kode = $this->pesan()['pickup_code'];
        DB::table('notifications')->delete();

        // Jam ambil 18.00: belum masuk jendela 30 menit.
        $this->artisan('notifikasi:pengingat-ambil')->assertSuccessful();
        $this->assertSame(0, DB::table('notifications')->count());

        $this->travelTo('2026-09-18 17:35:00');
        $this->artisan('notifikasi:pengingat-ambil')->assertSuccessful();
        $this->artisan('notifikasi:pengingat-ambil')->assertSuccessful();

        $n = DB::table('notifications')->where('user_id', $this->pembeli->id)->get();
        $this->assertCount(1, $n);
        $this->assertSame('Pesananmu siap diambil', $n[0]->title);
        $this->assertStringContainsString($kode, $n[0]->body);
    }

    public function test_tandai_dibaca_dan_notifikasi_orang_lain(): void
    {
        $this->pesan();
        $milikPemilik = DB::table('notifications')->where('user_id', $this->pemilik->id)->value('id');
        $milikKasir = DB::table('notifications')->where('user_id', $this->kasir->id)->value('id');

        $this->sebagai($this->pemilik)->postJson("/api/notifications/{$milikKasir}/read")->assertNotFound();
        $this->sebagai($this->pemilik)->postJson("/api/notifications/{$milikPemilik}/read")->assertNoContent();
        $this->sebagai($this->pemilik)->getJson('/api/notifications')->assertJsonPath('unread_count', 0)->assertJsonPath('data.0.is_read', true);

        $this->sebagai($this->kasir)->postJson('/api/notifications/read-all')->assertNoContent();
        $this->sebagai($this->kasir)->getJson('/api/notifications?unread=1')->assertJsonCount(0, 'data');
    }

    public function test_hapus_akun_tanpa_pesanan_menghapus_penuh(): void
    {
        DB::table('favorites')->insert(['user_id' => $this->pembeli->id, 'store_id' => $this->toko]);

        $this->sebagai($this->pembeli)->deleteJson('/api/me')->assertStatus(422);
        $this->sebagai($this->pembeli)->deleteJson('/api/me', ['confirm' => true])->assertNoContent();

        $this->assertDatabaseMissing('users', ['id' => $this->pembeli->id]);
        $this->assertSame(0, DB::table('favorites')->count());
        $this->assertSame(0, DB::table('personal_access_tokens')->count());
        $this->assertDatabaseHas('audit_logs', ['action' => 'user.delete', 'user_id' => null]);
    }

    public function test_hapus_akun_dengan_riwayat_pesanan_dianonimkan(): void
    {
        $kode = $this->pesan(1, 'Alergi kacang')['pickup_code'];

        $this->sebagai($this->pembeli)->deleteJson('/api/me', ['confirm' => true])->assertStatus(409);

        $this->sebagai($this->kasir)->postJson('/api/pickup-codes/redeem', ['store_id' => $this->toko, 'code' => $kode])->assertOk();
        $this->sebagai($this->pembeli)->deleteJson('/api/me', ['confirm' => true])->assertNoContent();

        $u = DB::table('users')->find($this->pembeli->id);
        $this->assertSame([null, null, null, 0], [$u->phone, $u->name, $u->email, (int) $u->is_active]);
        $o = DB::table('orders')->where('user_id', $u->id)->first();
        $this->assertSame(['completed', null], [$o->status, $o->note]);
        $this->assertSame(0, DB::table('notifications')->where('user_id', $u->id)->count());

        // Laporan mitra tetap utuh, dan nomornya bisa mendaftar lagi sebagai akun baru.
        $this->assertDatabaseHas('balance_transactions', ['order_id' => $o->id]);
        $this->assertSame(0, DB::table('users')->where('phone', '6281234567890')->count());
    }

    public function test_hapus_akun_mitra_ditolak(): void
    {
        $this->sebagai($this->pemilik)->deleteJson('/api/me', ['confirm' => true])->assertForbidden();
    }

    private function listingBaru(string $status, int $qty = 3): int
    {
        return DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000,
            'qty_total' => $qty, 'pickup_date' => now()->toDateString(), 'pickup_start' => now()->setTime(18, 0),
            'pickup_end' => now()->setTime(20, 0), 'status' => $status, 'ingredients_text' => 'Tepung, gula',
            'published_at' => $status === 'active' ? now()->subHour() : null,
        ]);
    }

    private function pesan(int $qty = 1, ?string $catatan = null): array
    {
        return $this->sebagai($this->pembeli)->postJson('/api/orders', [
            'items' => [['listing_id' => $this->listing, 'qty' => $qty]], 'note' => $catatan,
        ])->assertCreated()->json('data');
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }
}
