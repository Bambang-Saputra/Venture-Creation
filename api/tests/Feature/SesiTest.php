<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * GET /api/me dan POST /api/auth/logout.
 */
class SesiTest extends TestCase
{
    use RefreshDatabase;

    public function test_tanpa_token_ditolak_401(): void
    {
        $this->getJson('/api/me')->assertUnauthorized();
        $this->postJson('/api/auth/logout')->assertUnauthorized();
    }

    public function test_konsumen_baru_belum_lengkap_profilnya(): void
    {
        $user = $this->user(['phone' => '6281234567890']);

        $this->withToken($this->token($user))->getJson('/api/me')
            ->assertOk()
            ->assertJsonPath('user.phone', '6281234567890')
            ->assertJsonPath('user.has_google', false)
            ->assertJsonPath('is_profile_complete', false)
            ->assertJsonPath('consumer_profile', null)
            ->assertJsonMissingPath('user.google_sub');
    }

    public function test_profil_konsumen_ikut_dikembalikan(): void
    {
        $user = $this->user(['name' => 'Budi', 'google_sub' => 'sub-1']);
        DB::table('consumer_profiles')->insert([
            'user_id' => $user->id, 'area_label' => 'Kemanggisan', 'latitude' => -6.2006, 'longitude' => 106.7837,
        ]);

        $this->withToken($this->token($user))->getJson('/api/me')
            ->assertOk()
            ->assertJsonPath('user.has_google', true)
            ->assertJsonPath('is_profile_complete', true)
            ->assertJsonPath('consumer_profile.area_label', 'Kemanggisan')
            ->assertJsonPath('consumer_profile.latitude', -6.2006)
            ->assertJsonPath('consumer_profile.notify_promo', false);
    }

    public function test_keluar_hanya_mencabut_token_perangkat_ini(): void
    {
        $user = $this->user();
        $tokenIni = $this->token($user);
        $tokenLain = $this->token($user);

        $this->withToken($tokenIni)->postJson('/api/auth/logout')->assertNoContent();
        $this->app['auth']->forgetGuards();

        $this->withToken($tokenIni)->getJson('/api/me')->assertUnauthorized();
        $this->app['auth']->forgetGuards();
        $this->withToken($tokenLain)->getJson('/api/me')->assertOk();
    }

    public function test_token_akun_yang_dinonaktifkan_ditolak(): void
    {
        $user = $this->user();
        $token = $this->token($user);
        $user->forceFill(['is_active' => false])->save();

        $this->withToken($token)->getJson('/api/me')->assertForbidden()->assertJsonPath('code', 'account_inactive');
    }

    private function user(array $atribut = []): User
    {
        return User::forceCreate([...['phone' => '6289876543210', 'role' => 'consumer'], ...$atribut])->refresh();
    }

    private function token(User $user): string
    {
        return $user->createToken('uji')->plainTextToken;
    }
}
