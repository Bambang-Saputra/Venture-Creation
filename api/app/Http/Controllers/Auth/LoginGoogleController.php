<?php

namespace App\Http\Controllers\Auth;

use App\Exceptions\LoginGoogleDitolak;
use App\Http\Controllers\Controller;
use App\Models\User;
use App\Services\VerifikasiTokenGoogle;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

/**
 * POST /api/auth/google (F-25, ADR-0006).
 *
 * Android mengirim ID token dari Credential Manager. Server yang
 * memverifikasinya, lalu menerbitkan token Sanctum.
 */
class LoginGoogleController extends Controller
{
    private const NAMA_PERAN = ['consumer' => 'konsumen', 'partner' => 'mitra', 'admin' => 'admin'];

    public function __invoke(Request $request, VerifikasiTokenGoogle $verifikasi): JsonResponse
    {
        $data = $request->validate([
            'id_token' => ['required', 'string', 'max:4096'],
            'role' => ['required', Rule::in(['consumer', 'partner'])],
        ]);

        $klaim = $verifikasi->verifikasi($data['id_token']);
        $user = User::where('google_sub', $klaim['sub'])->first();
        $akunBaru = false;

        if ($user === null && $data['role'] === 'partner') {
            $user = $this->tautkanMitra($klaim);
        } elseif ($user === null) {
            $user = $this->buatKonsumen($klaim);
            $akunBaru = true;
        }

        if ($user->role !== $data['role']) {
            throw new LoginGoogleDitolak(
                'Akun Google ini terdaftar sebagai '.self::NAMA_PERAN[$user->role].'. Masuk lewat halaman '.self::NAMA_PERAN[$user->role].'.',
                403,
                'wrong_role',
                ['registered_role' => $user->role],
            );
        }

        if (! $user->is_active) {
            throw new LoginGoogleDitolak('Akun ini dinonaktifkan. Hubungi tim Life of Foods.', 403, 'account_inactive');
        }

        return response()->json([
            'token' => $user->createToken('android-google')->plainTextToken,
            // true berarti Android membuka K04 (Lengkapi profil) sebelum beranda.
            'is_new_user' => $akunBaru,
            'user' => $user->only(['id', 'name', 'email', 'phone', 'role']),
        ], $akunBaru ? 201 : 200);
    }

    /**
     * Mitra tidak pernah dibuat lewat Google (M03 berstatus WON'T). Hanya akun
     * mitra dari SeederPilot yang emailnya cocok dan belum tertaut yang diterima.
     *
     * @param  array{sub: string, email: string, name: ?string}  $klaim
     */
    private function tautkanMitra(array $klaim): User
    {
        $mitra = User::where('role', 'partner')
            ->whereNull('google_sub')
            ->where('email', $klaim['email'])
            ->first();

        if ($mitra === null) {
            throw new LoginGoogleDitolak(
                'Email ini belum terdaftar sebagai mitra. Hubungi tim Life of Foods.',
                403,
            );
        }

        $mitra->forceFill(['google_sub' => $klaim['sub']])->save();

        return $mitra;
    }

    /**
     * Tidak pernah menautkan ke akun lama lewat email: email di K04 diisi bebas
     * dan tidak diverifikasi, jadi kecocokan email bukan bukti kepemilikan.
     * Kalau email itu sudah dipakai akun lain, akun baru dibuat tanpa email.
     *
     * @param  array{sub: string, email: string, name: ?string}  $klaim
     */
    private function buatKonsumen(array $klaim): User
    {
        $emailBebas = ! User::where('email', $klaim['email'])->exists();

        return User::forceCreate([
            'google_sub' => $klaim['sub'],
            'name' => $klaim['name'],
            'email' => $emailBebas ? $klaim['email'] : null,
            'role' => 'consumer',
        ])->refresh();
    }
}
