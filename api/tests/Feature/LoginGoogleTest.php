<?php

namespace Tests\Feature;

use Firebase\JWT\JWT;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

/**
 * POST /api/auth/google (ADR-0006). Kunci Google diganti pasangan RSA buatan
 * uji, dan JWKS-nya disajikan lewat Http::fake, jadi tidak ada jaringan.
 */
class LoginGoogleTest extends TestCase
{
    use RefreshDatabase;

    private const CLIENT_ID = 'uji-client.apps.googleusercontent.com';

    private static string $kunciPrivat;

    private static array $jwk;

    public static function setUpBeforeClass(): void
    {
        parent::setUpBeforeClass();

        $kunci = self::kunciRsaBaru($privat);
        self::$kunciPrivat = $privat;
        $rsa = openssl_pkey_get_details($kunci)['rsa'];

        self::$jwk = [
            'kty' => 'RSA', 'alg' => 'RS256', 'use' => 'sig', 'kid' => 'kunci-uji',
            'n' => JWT::urlsafeB64Encode($rsa['n']),
            'e' => JWT::urlsafeB64Encode($rsa['e']),
        ];
    }

    protected function setUp(): void
    {
        parent::setUp();

        config(['services.google.client_id' => self::CLIENT_ID]);
        Http::fake(['www.googleapis.com/*' => Http::response(['keys' => [self::$jwk]], 200, ['Cache-Control' => 'max-age=600'])]);
    }

    public function test_konsumen_baru_dibuatkan_akun(): void
    {
        $this->masuk($this->token())
            ->assertCreated()
            ->assertJsonPath('is_new_user', true)
            ->assertJsonPath('user.role', 'consumer')
            ->assertJsonPath('user.phone', null)
            ->assertJsonStructure(['token']);

        $this->assertDatabaseHas('users', ['google_sub' => 'sub-123', 'email' => 'budi@gmail.com']);
    }

    public function test_masuk_ulang_tidak_membuat_akun_ganda(): void
    {
        $this->masuk($this->token())->assertCreated();
        $this->masuk($this->token())->assertOk()->assertJsonPath('is_new_user', false);

        $this->assertDatabaseCount('users', 1);
    }

    public function test_email_akun_lama_tidak_ditautkan_otomatis(): void
    {
        $lamaId = $this->buatUser(['phone' => '6281200000001', 'email' => 'budi@gmail.com', 'role' => 'consumer']);

        $this->masuk($this->token())->assertCreated()->assertJsonPath('user.email', null);

        $this->assertDatabaseHas('users', ['id' => $lamaId, 'google_sub' => null]);
        $this->assertDatabaseCount('users', 2);
    }

    public function test_tanda_tangan_palsu_ditolak(): void
    {
        self::kunciRsaBaru($privatLain);

        $this->masuk($this->token([], $privatLain))->assertUnauthorized();
        $this->assertDatabaseCount('users', 0);
    }

    public function test_aud_salah_ditolak(): void
    {
        $this->masuk($this->token(['aud' => 'aplikasi-lain.apps.googleusercontent.com']))->assertUnauthorized();
    }

    public function test_penerbit_salah_ditolak(): void
    {
        $this->masuk($this->token(['iss' => 'https://penipu.example']))->assertUnauthorized();
    }

    public function test_token_kedaluwarsa_ditolak(): void
    {
        $this->masuk($this->token(['iat' => time() - 7200, 'exp' => time() - 3600]))->assertUnauthorized();
    }

    public function test_email_belum_diverifikasi_ditolak(): void
    {
        $this->masuk($this->token(['email_verified' => false]))->assertUnauthorized();
    }

    public function test_mitra_dengan_email_tak_dikenal_ditolak(): void
    {
        $this->masuk($this->token(), 'partner')
            ->assertForbidden()
            ->assertJsonPath('message', 'Email ini belum terdaftar sebagai mitra. Daftarkan tokomu lewat nomor HP di halaman ini.');

        $this->assertDatabaseCount('users', 0);
    }

    public function test_mitra_terdaftar_ditautkan_saat_masuk_pertama(): void
    {
        $mitraId = $this->buatUser(['phone' => '6281200000002', 'email' => 'budi@gmail.com', 'role' => 'partner']);

        $this->masuk($this->token(), 'partner')->assertOk()->assertJsonPath('user.id', $mitraId);
        $this->masuk($this->token(), 'partner')->assertOk()->assertJsonPath('user.id', $mitraId);

        $this->assertDatabaseHas('users', ['id' => $mitraId, 'google_sub' => 'sub-123']);
        $this->assertDatabaseCount('users', 1);
    }

    public function test_akun_konsumen_tidak_bisa_masuk_sebagai_mitra(): void
    {
        $this->masuk($this->token())->assertCreated();

        $this->masuk($this->token(), 'partner')->assertForbidden();
    }

    public function test_badan_request_divalidasi(): void
    {
        $this->postJson('/api/auth/google', ['role' => 'admin'])
            ->assertUnprocessable()
            ->assertJsonValidationErrors(['id_token', 'role']);
    }

    private function masuk(string $idToken, string $peran = 'consumer')
    {
        return $this->postJson('/api/auth/google', ['id_token' => $idToken, 'role' => $peran]);
    }

    private function token(array $klaim = [], ?string $kunciPrivat = null): string
    {
        return JWT::encode(array_merge([
            'iss' => 'https://accounts.google.com',
            'aud' => self::CLIENT_ID,
            'sub' => 'sub-123',
            'email' => 'Budi@gmail.com',
            'email_verified' => true,
            'name' => 'Budi',
            'iat' => time(),
            'exp' => time() + 3600,
        ], $klaim), $kunciPrivat ?? self::$kunciPrivat, 'RS256', 'kunci-uji');
    }

    /**
     * Kunci dibuat saat uji, tidak disimpan di repo (gitleaks menolak kunci privat).
     * PHP di Windows (Laragon) butuh openssl.cnf bawaannya ditunjuk langsung.
     */
    private static function kunciRsaBaru(?string &$privat): \OpenSSLAsymmetricKey
    {
        $opsi = ['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA];
        $cnf = dirname(PHP_BINARY).'/extras/ssl/openssl.cnf';
        if (PHP_OS_FAMILY === 'Windows' && getenv('OPENSSL_CONF') === false && is_file($cnf)) {
            $opsi['config'] = $cnf;
        }

        $kunci = openssl_pkey_new($opsi);
        openssl_pkey_export($kunci, $privat, null, $opsi);

        return $kunci;
    }

    private function buatUser(array $kolom): int
    {
        return DB::table('users')->insertGetId($kolom + ['created_at' => now(), 'updated_at' => now()]);
    }
}
