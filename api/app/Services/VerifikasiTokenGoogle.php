<?php

namespace App\Services;

use App\Exceptions\LoginGoogleDitolak;
use Firebase\JWT\JWK;
use Firebase\JWT\JWT;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Throwable;

/**
 * Memverifikasi ID token Google di server (ADR-0006).
 *
 * Tanda tangan dicek terhadap kunci publik Google (JWKS) yang di-cache
 * selama max-age dari header Cache-Control. Endpoint tokeninfo sengaja
 * tidak dipakai: satu panggilan jaringan tambahan di setiap login.
 */
class VerifikasiTokenGoogle
{
    private const PENERBIT_SAH = ['accounts.google.com', 'https://accounts.google.com'];

    private const KUNCI_CACHE = 'google_jwks';

    /**
     * @return array{sub: string, email: string, name: ?string}
     */
    public function verifikasi(string $idToken): array
    {
        $clientId = config('services.google.client_id');

        if (! is_string($clientId) || $clientId === '') {
            throw new LoginGoogleDitolak('Login Google belum diaktifkan di server.', 503);
        }

        try {
            $kid = $this->kidDariHeader($idToken);
            $jwks = $this->jwks(segarkan: false);

            // Google memutar kuncinya berkala. Kid yang belum dikenal berarti
            // cache kita basi, jadi ambil ulang sekali sebelum menolak. Paling
            // sering sekali per menit, supaya kid acak tidak jadi cara membanjiri Google.
            if (! $this->punyaKid($jwks, $kid) && Cache::add(self::KUNCI_CACHE.'_disegarkan', true, 60)) {
                $jwks = $this->jwks(segarkan: true);
            }

            $klaim = (array) JWT::decode($idToken, JWK::parseKeySet($jwks, 'RS256'));
        } catch (LoginGoogleDitolak $e) {
            throw $e;
        } catch (Throwable) {
            // Tanda tangan salah, kedaluwarsa, format rusak: semua dijawab sama.
            throw new LoginGoogleDitolak('Token Google tidak sah atau sudah kedaluwarsa.');
        }

        if (($klaim['aud'] ?? null) !== $clientId
            || ! in_array($klaim['iss'] ?? null, self::PENERBIT_SAH, true)) {
            throw new LoginGoogleDitolak('Token Google tidak diterbitkan untuk aplikasi ini.');
        }

        $emailTerverifikasi = ($klaim['email_verified'] ?? false);
        if ($emailTerverifikasi !== true && $emailTerverifikasi !== 'true') {
            throw new LoginGoogleDitolak('Email akun Google ini belum diverifikasi Google.');
        }

        if (! is_string($klaim['sub'] ?? null) || $klaim['sub'] === ''
            || ! is_string($klaim['email'] ?? null)) {
            throw new LoginGoogleDitolak('Token Google tidak lengkap.');
        }

        return [
            'sub' => $klaim['sub'],
            'email' => mb_strtolower($klaim['email']),
            'name' => is_string($klaim['name'] ?? null) ? $klaim['name'] : null,
        ];
    }

    private function kidDariHeader(string $idToken): string
    {
        $bagian = explode('.', $idToken);
        $header = count($bagian) === 3 ? json_decode(JWT::urlsafeB64Decode($bagian[0]), true) : null;

        if (! is_array($header) || ! is_string($header['kid'] ?? null)) {
            throw new LoginGoogleDitolak('Token Google tidak sah atau sudah kedaluwarsa.');
        }

        return $header['kid'];
    }

    /**
     * @param  array{keys?: array<int, array<string, mixed>>}  $jwks
     */
    private function punyaKid(array $jwks, string $kid): bool
    {
        foreach ($jwks['keys'] ?? [] as $kunci) {
            if (($kunci['kid'] ?? null) === $kid) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return array{keys: array<int, array<string, mixed>>}
     */
    private function jwks(bool $segarkan): array
    {
        if (! $segarkan && is_array($tersimpan = Cache::get(self::KUNCI_CACHE))) {
            return $tersimpan;
        }

        $respons = Http::timeout(5)->retry(2, 200, throw: false)->get(config('services.google.jwks_url'));

        if (! $respons->successful() || ! is_array($respons->json('keys'))) {
            throw new LoginGoogleDitolak('Server Google sedang tidak bisa dihubungi. Coba lagi sebentar.', 503);
        }

        preg_match('/max-age=(\d+)/', (string) $respons->header('Cache-Control'), $cocok);
        $detik = max(60, (int) ($cocok[1] ?? 3600));

        $jwks = ['keys' => $respons->json('keys')];
        Cache::put(self::KUNCI_CACHE, $jwks, $detik);

        return $jwks;
    }
}
