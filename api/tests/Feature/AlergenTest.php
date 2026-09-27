<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\SeederAlergen;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * GET /api/allergens dan PUT /api/me/allergens (K05).
 */
class AlergenTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(SeederAlergen::class);
    }

    public function test_daftar_hanya_alergen_aktif_berurutan_tanpa_token(): void
    {
        DB::table('allergens')->where('code', 'wijen')->update(['is_active' => false]);

        $data = $this->getJson('/api/allergens')->assertOk()->json('data');

        $this->assertSame('kacang_tanah', $data[0]['code']);
        $this->assertNotContains('wijen', array_column($data, 'code'));
        $this->assertSame(['code', 'name', 'type'], array_keys($data[0]));
    }

    public function test_pilihan_disimpan_dan_muncul_di_me(): void
    {
        $user = $this->user();

        $this->ganti($user, [['code' => 'kacang_tanah', 'severity' => 'severe'], ['code' => 'vegetarian']])
            ->assertOk()
            ->assertJsonCount(2, 'data')
            ->assertJsonPath('data.0.severity', 'severe')
            ->assertJsonPath('data.1.severity', 'avoid');

        $this->app['auth']->forgetGuards();
        $this->withToken($user->createToken('uji')->plainTextToken)->getJson('/api/me')
            ->assertJsonPath('allergens.0.code', 'kacang_tanah')
            ->assertJsonPath('allergens.1.code', 'vegetarian');
    }

    public function test_pilihan_baru_mengganti_yang_lama_dan_bisa_dikosongkan(): void
    {
        $user = $this->user();
        $this->ganti($user, [['code' => 'susu'], ['code' => 'telur']]);

        $this->ganti($user, [['code' => 'gluten']])->assertOk()->assertJsonPath('data.0.code', 'gluten')->assertJsonCount(1, 'data');
        $this->ganti($user, [])->assertOk()->assertJsonCount(0, 'data');

        $this->assertDatabaseCount('user_allergens', 0);
    }

    public function test_kode_tidak_dikenal_ganda_atau_nonaktif_ditolak(): void
    {
        DB::table('allergens')->where('code', 'wijen')->update(['is_active' => false]);
        $user = $this->user();

        $this->ganti($user, [['code' => 'durian']])->assertUnprocessable()->assertJsonValidationErrors('allergens.0.code');
        $this->ganti($user, [['code' => 'susu'], ['code' => 'susu']])->assertUnprocessable();
        $this->ganti($user, [['code' => 'wijen']])->assertUnprocessable();
        $this->ganti($user, [['code' => 'susu', 'severity' => 'sedikit']])->assertUnprocessable();
    }

    public function test_mitra_ditolak(): void
    {
        $this->ganti($this->user('partner'), [['code' => 'susu']])->assertForbidden();
    }

    private function user(string $role = 'consumer'): User
    {
        return User::forceCreate(['phone' => '6289876543210', 'role' => $role])->refresh();
    }

    private function ganti(User $user, array $alergen): TestResponse
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken)
            ->putJson('/api/me/allergens', ['allergens' => $alergen]);
    }
}
