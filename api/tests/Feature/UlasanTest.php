<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Database\QueryException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * Rating dan ulasan: pembeli menilai pesanan yang sudah diambil, rating
 * toko tampil di listing (K07, K10) dan profil toko (M14).
 */
class UlasanTest extends TestCase
{
    use RefreshDatabase;

    private User $pembeli;

    private User $pemilik;

    private int $toko;

    private int $listing;

    protected function setUp(): void
    {
        parent::setUp();

        $this->travelTo('2026-09-18 19:30:00');
        $this->pembeli = User::forceCreate(['phone' => '6281200000001', 'name' => 'Dinda Pratiwi', 'role' => 'consumer'])->refresh();
        $this->pemilik = User::forceCreate(['phone' => '6281299887766', 'name' => 'Kalyan Pratama', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Kopi Kalyan SCBD', 'slug' => 'kopi-kalyan', 'category' => 'cafe',
            'address' => 'Jl. Jend. Sudirman Kav 52',
        ]);
        $this->listing = DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000,
            'qty_total' => 5, 'pickup_date' => '2026-09-18', 'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00',
            'status' => 'active', 'ingredients_text' => 'Tepung, mentega',
        ]);
    }

    public function test_pembeli_menilai_pesanan_selesai_lalu_mengubahnya(): void
    {
        $order = $this->pesanan('completed');

        $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => 4, 'comment' => '  Croissantnya masih renyah  '])
            ->assertCreated()->assertJsonPath('data.rating', 4)->assertJsonPath('data.comment', 'Croissantnya masih renyah');

        $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => 5])
            ->assertOk()->assertJsonPath('data.rating', 5)->assertJsonPath('data.comment', null);

        $this->assertDatabaseCount('reviews', 1);
        $this->sebagai($this->pembeli)->getJson("/api/orders/{$order}")->assertOk()
            ->assertJsonPath('data.review.rating', 5)->assertJsonPath('data.can_review', true);
        $this->sebagai($this->pembeli)->getJson('/api/orders?status=history')->assertOk()
            ->assertJsonPath('data.0.review_rating', 5)->assertJsonPath('data.0.can_review', true);
    }

    public function test_hanya_pesanan_selesai_dan_belum_lewat_tujuh_hari(): void
    {
        $menunggu = $this->pesanan('pending_pickup', null);
        $this->sebagai($this->pembeli)->postJson("/api/orders/{$menunggu}/review", ['rating' => 5])->assertStatus(409);

        $lama = $this->pesanan('completed', '2026-09-10 19:00:00');
        $this->sebagai($this->pembeli)->postJson("/api/orders/{$lama}/review", ['rating' => 5])->assertStatus(409);
        $this->sebagai($this->pembeli)->getJson("/api/orders/{$lama}")->assertJsonPath('data.can_review', false);

        $this->assertDatabaseCount('reviews', 0);
    }

    public function test_validasi_dan_akses(): void
    {
        $order = $this->pesanan('completed');

        $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => 6])->assertStatus(422)->assertJsonValidationErrors('rating');
        $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => 0])->assertStatus(422);

        $orangLain = User::forceCreate(['phone' => '6281200000002', 'name' => 'Rizky', 'role' => 'consumer'])->refresh();
        $this->sebagai($orangLain)->postJson("/api/orders/{$order}/review", ['rating' => 1])->assertNotFound();
        $this->sebagai($this->pemilik)->postJson("/api/orders/{$order}/review", ['rating' => 5])->assertForbidden();
    }

    public function test_rating_toko_di_listing_dan_profil_toko(): void
    {
        $this->getJson("/api/listings/{$this->listing}")->assertOk()
            ->assertJsonPath('data.store.rating_average', null)->assertJsonPath('data.store.rating_count', 0);

        foreach ([5, 4, 5] as $bintang) {
            $order = $this->pesanan('completed');
            $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => $bintang])->assertCreated();
        }

        $this->getJson("/api/listings/{$this->listing}")->assertOk()
            ->assertJsonPath('data.store.rating_average', 4.7)->assertJsonPath('data.store.rating_count', 3);
        $this->getJson('/api/listings')->assertOk()
            ->assertJsonPath('data.0.store.rating_average', 4.7)->assertJsonPath('data.0.store.rating_count', 3)
            // K08: alamat dan koordinat toko untuk tombol rute.
            ->assertJsonPath('data.0.store.address', 'Jl. Jend. Sudirman Kav 52')
            ->assertJsonPath('data.0.store.latitude', null);
        $this->sebagai($this->pemilik)->getJson("/api/partner/stores/{$this->toko}")->assertOk()
            ->assertJsonPath('data.rating_average', 4.7)->assertJsonPath('data.rating_count', 3);
    }

    public function test_mitra_membaca_ulasan_dengan_nama_depan_saja(): void
    {
        $order = $this->pesanan('completed');
        $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => 5, 'comment' => 'Mantap'])->assertCreated();

        $this->sebagai($this->pemilik)->getJson("/api/partner/stores/{$this->toko}/reviews")->assertOk()
            ->assertJsonPath('data.0.buyer_name', 'Dinda')
            ->assertJsonPath('data.0.comment', 'Mantap')
            ->assertJsonPath('summary.rating_count', 1)
            ->assertJsonMissingPath('data.0.phone');

        $this->sebagai($this->pembeli)->getJson("/api/partner/stores/{$this->toko}/reviews")->assertNotFound();
    }

    public function test_hapus_akun_menghapus_teks_ulasan_tapi_bintang_tetap(): void
    {
        $order = $this->pesanan('completed');
        $this->sebagai($this->pembeli)->postJson("/api/orders/{$order}/review", ['rating' => 4, 'comment' => 'Rumah saya dekat sini'])->assertCreated();

        $this->sebagai($this->pembeli)->deleteJson('/api/me', ['confirm' => true])->assertSuccessful();

        $this->assertDatabaseHas('reviews', ['order_id' => $order, 'rating' => 4, 'comment' => null]);
    }

    private int $nomor = 0;

    public function test_database_menolak_rating_di_luar_satu_sampai_lima(): void
    {
        $order = $this->pesanan('completed');
        $baris = ['order_id' => $order, 'store_id' => $this->toko, 'user_id' => $this->pembeli->id, 'created_at' => now(), 'updated_at' => now()];

        DB::table('reviews')->insert([...$baris, 'rating' => 5]);
        $this->expectException(QueryException::class);
        DB::table('reviews')->where('order_id', $order)->update(['rating' => 6]);
    }

    private function pesanan(string $status, ?string $selesai = '2026-09-18 19:15:00'): int
    {
        $this->nomor++;
        $id = DB::table('orders')->insertGetId([
            'user_id' => $this->pembeli->id, 'store_id' => $this->toko, 'code' => 'ORD-'.$this->nomor, 'status' => $status,
            'subtotal_rupiah' => 18000, 'total_rupiah' => 18000, 'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00',
            'placed_at' => now(), 'completed_at' => $status === 'completed' ? $selesai : null,
        ]);
        DB::table('order_items')->insert(['order_id' => $id, 'listing_id' => $this->listing, 'title_snapshot' => 'Tas Pastry Sore', 'unit_price_rupiah' => 18000, 'qty' => 1, 'line_total_rupiah' => 18000]);

        return $id;
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }
}
