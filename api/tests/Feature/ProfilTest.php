<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * PATCH /api/me (K04, K19, K20).
 */
class ProfilTest extends TestCase
{
    use RefreshDatabase;

    public function test_k04_melengkapi_profil_konsumen(): void
    {
        $user = $this->user();

        $this->ubah($user, ['name' => 'Dara Renata', 'email' => 'dara@email.com', 'area_label' => 'SCBD, Jakarta Selatan'])
            ->assertOk()
            ->assertJsonPath('is_profile_complete', true)
            ->assertJsonPath('user.email', 'dara@email.com')
            ->assertJsonPath('consumer_profile.area_label', 'SCBD, Jakarta Selatan')
            ->assertJsonPath('consumer_profile.notify_promo', false);

        $this->assertDatabaseHas('consumer_profiles', ['user_id' => $user->id, 'area_label' => 'SCBD, Jakarta Selatan']);
    }

    public function test_k20_cukup_mengirim_satu_sakelar(): void
    {
        $user = $this->user(['name' => 'Dara']);
        $this->ubah($user, ['area_label' => 'Kemanggisan']);

        $this->ubah($user, ['notify_promo' => true])
            ->assertOk()
            ->assertJsonPath('user.name', 'Dara')
            ->assertJsonPath('consumer_profile.area_label', 'Kemanggisan')
            ->assertJsonPath('consumer_profile.notify_promo', true);
    }

    public function test_nama_dan_area_tidak_boleh_dikosongkan(): void
    {
        $this->ubah($this->user(), ['name' => '', 'area_label' => ''])
            ->assertUnprocessable()
            ->assertJsonValidationErrors(['name', 'area_label']);
    }

    public function test_email_milik_akun_lain_ditolak(): void
    {
        $this->user(['phone' => '6281111111111', 'email' => 'dipakai@email.com']);

        $this->ubah($this->user(), ['email' => 'dipakai@email.com'])
            ->assertUnprocessable()
            ->assertJsonValidationErrors('email');
    }

    public function test_koordinat_harus_berpasangan(): void
    {
        $this->ubah($this->user(), ['latitude' => -6.2])
            ->assertUnprocessable()
            ->assertJsonValidationErrors('longitude');
    }

    public function test_mitra_tidak_bisa_mengisi_profil_konsumen(): void
    {
        $mitra = $this->user(['role' => 'partner']);

        $this->ubah($mitra, ['area_label' => 'SCBD'])->assertUnprocessable()->assertJsonValidationErrors('area_label');
        $this->ubah($mitra, ['name' => 'Bu Sari'])->assertOk()->assertJsonPath('consumer_profile', null);
    }

    public function test_peran_tidak_bisa_diubah_lewat_badan_request(): void
    {
        $user = $this->user();

        $this->ubah($user, ['name' => 'Dara', 'role' => 'admin'])->assertOk()->assertJsonPath('user.role', 'consumer');
    }

    public function test_k18_dampak_hanya_dari_pesanan_yang_diambil(): void
    {
        $user = $this->user(['name' => 'Dara']);
        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik->id, 'name' => 'Kopi Kalyan', 'slug' => 'kopi', 'category' => 'cafe', 'address' => 'Jl. Uji',
        ]);
        $listing = DB::table('listings')->insertGetId([
            'store_id' => $toko, 'type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000,
            'original_value_rupiah' => 54000, 'qty_total' => 5, 'pickup_date' => '2026-09-18',
            'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00', 'status' => 'active',
            'ingredients_text' => 'Tepung',
        ]);
        foreach (['completed' => 2, 'no_show' => 1] as $status => $qty) {
            $order = DB::table('orders')->insertGetId([
                'user_id' => $user->id, 'store_id' => $toko, 'code' => 'ORD-'.$status, 'status' => $status,
                'subtotal_rupiah' => 18000 * $qty, 'total_rupiah' => 18000 * $qty,
                'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00', 'placed_at' => now(),
            ]);
            DB::table('order_items')->insert(['order_id' => $order, 'listing_id' => $listing, 'title_snapshot' => 'Tas Pastry Sore',
                'unit_price_rupiah' => 18000, 'qty' => $qty, 'line_total_rupiah' => 18000 * $qty]);
        }

        $this->withToken($user->createToken('uji')->plainTextToken)->getJson('/api/me/impact')->assertOk()
            ->assertJsonPath('data.portions_rescued', 2)
            ->assertJsonPath('data.saved_rupiah', 72000)
            ->assertJsonPath('data.orders_completed', 1);

        $this->app['auth']->forgetGuards();
        $this->withToken($pemilik->createToken('uji')->plainTextToken)->getJson('/api/me/impact')->assertForbidden();
    }

    private function user(array $atribut = []): User
    {
        return User::forceCreate([...['phone' => '6289876543210', 'role' => 'consumer'], ...$atribut])->refresh();
    }

    private function ubah(User $user, array $data): TestResponse
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken)->patchJson('/api/me', $data);
    }
}
