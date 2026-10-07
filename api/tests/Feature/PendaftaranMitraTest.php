<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * M03 Daftar jadi mitra, M04 Menunggu verifikasi, dan perintah mitra:setujui (#7).
 */
class PendaftaranMitraTest extends TestCase
{
    use RefreshDatabase;

    private User $mitra;

    protected function setUp(): void
    {
        parent::setUp();
        $this->travelTo('2026-10-06 10:00:00');
        $this->mitra = User::forceCreate(['phone' => '6281299001122', 'role' => 'partner'])->refresh();
    }

    public function test_alur_daftar_menunggu_lalu_disetujui(): void
    {
        $this->sebagai($this->mitra)->getJson('/api/partner/application')->assertOk()->assertJsonPath('data', null);
        $this->sebagai($this->mitra)->getJson('/api/partner/stores')->assertOk()->assertJsonCount(0, 'data');

        $id = $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian(['nib' => '1234567890123']))
            ->assertCreated()
            ->assertJsonPath('data.status', 'pending')
            ->assertJsonPath('data.nib', '1234567890123')
            ->assertJsonPath('data.open_time', '08:00')
            ->json('data.id');

        // Belum disetujui: tetap tidak punya toko, dan tidak bisa mendaftar dua kali.
        $this->sebagai($this->mitra)->getJson('/api/partner/stores')->assertJsonCount(0, 'data');
        $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian())->assertStatus(409);

        $this->artisan('mitra:setujui', ['id' => $id])->assertSuccessful();

        $this->sebagai($this->mitra)->getJson('/api/partner/application')->assertJsonPath('data.status', 'approved');
        $toko = $this->sebagai($this->mitra)->getJson('/api/partner/stores')->assertOk()
            ->assertJsonPath('data.0.name', 'Warung Bu Siti')
            ->assertJsonPath('data.0.my_role', 'owner')
            ->json('data.0.id');

        $this->assertSame(7, DB::table('store_hours')->where('store_id', $toko)->count());
        $this->assertSame('1234567890123', DB::table('stores')->where('id', $toko)->value('nib'));
        $this->assertSame($this->mitra->phone, DB::table('stores')->where('id', $toko)->value('whatsapp'));
        $this->assertSame('Siti Aminah', DB::table('users')->where('id', $this->mitra->id)->value('name'));
        $this->sebagai($this->mitra)->getJson("/api/partner/stores/{$toko}/summary")->assertOk();
        $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian())->assertStatus(409);
    }

    public function test_nib_opsional_tapi_harus_13_angka_kalau_diisi(): void
    {
        $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian(['nib' => '12345']))
            ->assertUnprocessable()->assertJsonValidationErrors('nib');
        $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian(['nib' => '']))
            ->assertCreated()->assertJsonPath('data.nib', null);
    }

    public function test_validasi_jam_dan_alamat(): void
    {
        $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian([
            'open_time' => '20:00', 'close_time' => '08:00', 'address' => 'Jl. A', 'category' => 'warung',
        ]))->assertUnprocessable()->assertJsonValidationErrors(['close_time', 'address', 'category']);
    }

    public function test_ditolak_boleh_mendaftar_ulang(): void
    {
        $id = $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian())->json('data.id');
        $this->artisan('mitra:setujui', ['id' => $id, '--tolak' => 'Alamat tidak ditemukan di peta.'])->assertSuccessful();

        $this->sebagai($this->mitra)->getJson('/api/partner/application')
            ->assertJsonPath('data.status', 'rejected')
            ->assertJsonPath('data.rejection_reason', 'Alamat tidak ditemukan di peta.');
        $this->sebagai($this->mitra)->postJson('/api/partner/application', $this->isian())->assertCreated();
        $this->artisan('mitra:setujui', ['id' => $id])->assertFailed();
    }

    public function test_konsumen_tidak_bisa_mendaftarkan_toko(): void
    {
        $konsumen = User::forceCreate(['phone' => '6281299001133', 'role' => 'consumer'])->refresh();
        $this->sebagai($konsumen)->postJson('/api/partner/application', $this->isian())->assertForbidden();
        $this->app['auth']->forgetGuards();
        $this->flushHeaders()->postJson('/api/partner/application', $this->isian())->assertUnauthorized();
    }

    private function isian(array $ganti = []): array
    {
        return [
            'owner_name' => 'Siti Aminah',
            'store_name' => 'Warung Bu Siti',
            'category' => 'resto',
            'address' => 'Jl. Kemanggisan Utama No. 12, Jakarta Barat',
            'latitude' => -6.2010,
            'longitude' => 106.7830,
            'open_time' => '08:00',
            'close_time' => '21:00',
            ...$ganti,
        ];
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }
}
