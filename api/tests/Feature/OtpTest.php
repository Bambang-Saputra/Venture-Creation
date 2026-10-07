<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * POST /api/auth/otp/request dan /api/auth/otp/verify (F-01).
 */
class OtpTest extends TestCase
{
    use RefreshDatabase;

    private const NOMOR = '6281234567890';

    protected function setUp(): void
    {
        parent::setUp();

        config(['lof.pilot_mode' => true]);
    }

    public function test_nomor_dinormalisasi_dan_hanya_hash_yang_disimpan(): void
    {
        $kode = $this->minta('0812-3456-7890')
            ->assertStatus(202)
            ->assertJsonPath('expires_in', 300)
            ->json('pilot_code');

        $this->assertMatchesRegularExpression('/^[0-9]{6}$/', $kode);
        $otp = DB::table('otp_codes')->where('phone', self::NOMOR)->sole();
        $this->assertNotSame($kode, $otp->code_hash);
        $this->assertTrue(Hash::check($kode, $otp->code_hash));
    }

    public function test_tanpa_mode_pilot_permintaan_ditolak_503(): void
    {
        config(['lof.pilot_mode' => false]);

        $this->minta()->assertStatus(503)->assertJsonMissingPath('pilot_code');
        $this->assertDatabaseCount('otp_codes', 0);
    }

    public function test_nomor_bukan_seluler_indonesia_ditolak(): void
    {
        $this->minta('021-555-1234')->assertUnprocessable()->assertJsonValidationErrors('phone');
    }

    public function test_nomor_baru_di_halaman_mitra_dibuatkan_akun_mitra_tanpa_toko(): void
    {
        $kode = $this->minta(self::NOMOR, 'partner')->assertStatus(202)->json('pilot_code');

        $this->verifikasi($kode, 'partner')->assertCreated()
            ->assertJsonPath('is_new_user', true)
            ->assertJsonPath('user.role', 'partner');
        $this->assertDatabaseCount('stores', 0);
    }

    public function test_kirim_ulang_menunggu_jeda_dan_kode_lama_gugur(): void
    {
        $lama = $this->minta()->json('pilot_code');
        $this->minta()->assertStatus(429);

        $this->travel(61)->seconds();
        $baru = $this->minta()->assertStatus(202)->json('pilot_code');

        $this->assertSame(1, DB::table('otp_codes')->where('phone', self::NOMOR)->count());
        if ($lama !== $baru) {
            $this->verifikasi($lama)->assertUnprocessable();
        }
        $this->verifikasi($baru)->assertCreated();
    }

    public function test_konsumen_baru_dibuatkan_akun(): void
    {
        $kode = $this->minta()->json('pilot_code');

        $this->verifikasi($kode)
            ->assertCreated()
            ->assertJsonPath('is_new_user', true)
            ->assertJsonPath('user.phone', self::NOMOR)
            ->assertJsonPath('user.role', 'consumer')
            ->assertJsonStructure(['token']);

        $this->assertNotNull(User::where('phone', self::NOMOR)->sole()->phone_verified_at);
    }

    public function test_kode_hanya_bisa_dipakai_sekali(): void
    {
        $kode = $this->minta()->json('pilot_code');

        $this->verifikasi($kode)->assertCreated();
        $this->verifikasi($kode)->assertUnprocessable();
    }

    public function test_kode_kedaluwarsa_setelah_lima_menit(): void
    {
        $kode = $this->minta()->json('pilot_code');

        $this->travel(301)->seconds();
        $this->verifikasi($kode)->assertUnprocessable();
    }

    public function test_lima_kode_salah_mengunci_kode_itu(): void
    {
        $kode = $this->minta()->json('pilot_code');
        $salah = $kode === '000000' ? '111111' : '000000';

        $this->verifikasi($salah)->assertUnprocessable()->assertJsonPath('message', 'Kode salah. Sisa 4 percobaan.');
        for ($i = 0; $i < 4; $i++) {
            $this->verifikasi($salah)->assertUnprocessable();
        }

        $this->verifikasi($kode)->assertStatus(429);
        $this->assertDatabaseCount('personal_access_tokens', 0);
    }

    public function test_mitra_terdaftar_masuk_tanpa_akun_baru(): void
    {
        User::forceCreate(['phone' => self::NOMOR, 'role' => 'partner']);
        $kode = $this->minta(self::NOMOR, 'partner')->assertStatus(202)->json('pilot_code');

        $this->verifikasi($kode, 'partner')
            ->assertOk()
            ->assertJsonPath('is_new_user', false)
            ->assertJsonPath('user.role', 'partner');

        $this->assertDatabaseCount('users', 1);
    }

    public function test_nomor_mitra_ditolak_di_halaman_konsumen(): void
    {
        User::forceCreate(['phone' => self::NOMOR, 'role' => 'partner']);
        $kode = $this->minta()->json('pilot_code');

        $this->verifikasi($kode)->assertForbidden()
            ->assertJsonPath('code', 'wrong_role')->assertJsonPath('registered_role', 'partner');
    }

    public function test_nomor_konsumen_tetap_ditolak_di_halaman_mitra(): void
    {
        User::forceCreate(['phone' => self::NOMOR, 'role' => 'consumer']);
        $kode = $this->minta(self::NOMOR, 'partner')->json('pilot_code');

        $this->verifikasi($kode, 'partner')->assertForbidden()->assertJsonPath('code', 'wrong_role');
    }

    public function test_akun_nonaktif_ditolak(): void
    {
        User::forceCreate(['phone' => self::NOMOR, 'role' => 'consumer', 'is_active' => false]);
        $kode = $this->minta()->json('pilot_code');

        $this->verifikasi($kode)->assertForbidden()->assertJsonPath('code', 'account_inactive');
        $this->assertDatabaseCount('personal_access_tokens', 0);
    }

    public function test_banyak_penguji_di_satu_ip_tidak_saling_menghabiskan_jatah(): void
    {
        // Dulu throttle:5,1 per IP: permintaan keenam dari satu Wi-Fi langsung 429.
        foreach (range(1, 8) as $i) {
            $this->minta('62812345678'.str_pad((string) $i, 2, '0', STR_PAD_LEFT))->assertStatus(202);
        }
    }

    public function test_terlalu_sering_verifikasi_dibalas_pesan_indonesia(): void
    {
        $this->minta()->assertStatus(202);
        foreach (range(1, 10) as $i) {
            $this->verifikasi('000000');
        }

        $this->verifikasi('000000')
            ->assertStatus(429)
            ->assertJsonPath('code', 'too_many_requests')
            ->assertJsonPath('message', fn (string $pesan) => str_starts_with($pesan, 'Terlalu sering mencoba. Tunggu '));
    }

    private function minta(string $phone = self::NOMOR, string $role = 'consumer'): TestResponse
    {
        return $this->postJson('/api/auth/otp/request', ['phone' => $phone, 'role' => $role]);
    }

    private function verifikasi(string $kode, string $role = 'consumer'): TestResponse
    {
        return $this->postJson('/api/auth/otp/verify', ['phone' => self::NOMOR, 'code' => $kode, 'role' => $role]);
    }
}
