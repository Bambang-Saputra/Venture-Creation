<?php

namespace App\Http\Controllers\Auth;

use App\Exceptions\OtpDitolak;
use App\Http\Controllers\Controller;
use App\Models\User;
use App\Support\NomorHp;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

/**
 * POST /api/auth/otp/request dan /api/auth/otp/verify (F-01, K02-K03, M01-M02).
 *
 * Kode mentah tidak pernah disimpan atau dicatat di log: otp_codes hanya
 * menyimpan hash. Satu nomor hanya punya satu kode aktif.
 */
class OtpController extends Controller
{
    private const NAMA_PERAN = ['consumer' => 'konsumen', 'partner' => 'mitra', 'admin' => 'admin'];

    public function minta(Request $request): JsonResponse
    {
        $data = $request->validate([
            'phone' => ['required', 'string', 'max:20'],
            'role' => ['required', Rule::in(['consumer', 'partner'])],
        ]);
        $phone = $this->nomor($data['phone']);
        $aturan = config('lof.otp');

        // Tempat gateway WhatsApp nanti. Sampai itu ada, hanya mode pilot yang
        // bisa menyerahkan kode, jadi lebih jujur menolak daripada diam.
        if (! config('lof.pilot_mode')) {
            throw new OtpDitolak('Pengiriman kode lewat WhatsApp belum tersedia. Coba lagi nanti.', 503);
        }

        // Nomor baru di halaman mitra boleh meminta kode: setelah masuk, akunnya diarahkan ke
        // M03 Daftar jadi mitra. Tanpa toko yang disetujui tim, akun itu tidak bisa membuka
        // satu pun rute /partner/stores.

        $terakhir = DB::table('otp_codes')->where('phone', $phone)->whereNull('consumed_at')->latest('id')->first();
        if ($terakhir !== null) {
            $bolehLagi = Carbon::parse($terakhir->created_at)->addSeconds($aturan['jeda_kirim_ulang_detik']);
            if (now()->lt($bolehLagi)) {
                $sisa = (int) ceil(now()->diffInSeconds($bolehLagi));
                throw new OtpDitolak("Tunggu {$sisa} detik sebelum meminta kode baru.", 429);
            }
        }

        $kode = str_pad((string) random_int(0, 10 ** $aturan['panjang'] - 1), $aturan['panjang'], '0', STR_PAD_LEFT);

        DB::transaction(function () use ($phone, $kode, $aturan, $request) {
            // Kode lama yang belum dipakai langsung gugur.
            DB::table('otp_codes')->where('phone', $phone)->whereNull('consumed_at')->delete();
            DB::table('otp_codes')->insert([
                'phone' => $phone,
                'code_hash' => Hash::make($kode),
                'purpose' => 'login',
                'expires_at' => now()->addSeconds($aturan['berlaku_detik']),
                'request_ip' => $request->ip(),
                'created_at' => now(),
                'updated_at' => now(),
            ]);
        });

        return response()->json([
            'message' => 'Mode uji coba: kode ditampilkan di layar, tidak dikirim lewat WhatsApp.',
            'expires_in' => $aturan['berlaku_detik'],
            'resend_in' => $aturan['jeda_kirim_ulang_detik'],
            'pilot_code' => $kode,
        ], 202);
    }

    public function verifikasi(Request $request): JsonResponse
    {
        $aturan = config('lof.otp');
        $data = $request->validate([
            'phone' => ['required', 'string', 'max:20'],
            'code' => ['required', 'string', 'regex:/^[0-9]{'.$aturan['panjang'].'}$/'],
            'role' => ['required', Rule::in(['consumer', 'partner'])],
        ]);
        $phone = $this->nomor($data['phone']);

        // Dikunci per baris supaya tebakan paralel tidak melewati batas percobaan.
        // Hasilnya dikembalikan, bukan dilempar, agar penambahan attempts tidak ikut di-rollback.
        [$hasil, $sisa] = DB::transaction(function () use ($phone, $data, $aturan) {
            $otp = DB::table('otp_codes')
                ->where('phone', $phone)
                ->whereNull('consumed_at')
                ->where('expires_at', '>', now())
                ->latest('id')
                ->lockForUpdate()
                ->first();

            if ($otp === null) {
                return ['tidak_ada', 0];
            }
            if ($otp->attempts >= $aturan['maks_percobaan']) {
                return ['habis', 0];
            }
            if (! Hash::check($data['code'], $otp->code_hash)) {
                DB::table('otp_codes')->where('id', $otp->id)->increment('attempts');

                return ['salah', $aturan['maks_percobaan'] - $otp->attempts - 1];
            }

            DB::table('otp_codes')->where('id', $otp->id)->update(['consumed_at' => now(), 'updated_at' => now()]);

            return ['ok', 0];
        });

        match ($hasil) {
            'tidak_ada' => throw new OtpDitolak('Kode sudah kedaluwarsa atau belum diminta. Minta kode baru.'),
            'habis' => throw new OtpDitolak('Terlalu banyak percobaan. Minta kode baru.', 429),
            'salah' => throw new OtpDitolak($sisa > 0 ? "Kode salah. Sisa {$sisa} percobaan." : 'Kode salah. Minta kode baru.'),
            'ok' => null,
        };

        $user = User::where('phone', $phone)->first();
        $akunBaru = false;

        if ($user === null) {
            // Mitra baru belum punya toko: Android membuka M03 karena GET /partner/stores kosong.
            $user = User::forceCreate(['phone' => $phone, 'phone_verified_at' => now(), 'role' => $data['role']])->refresh();
            $akunBaru = true;
        }

        if ($user->role !== $data['role']) {
            throw new OtpDitolak(
                'Nomor ini terdaftar sebagai '.self::NAMA_PERAN[$user->role].'. Masuk lewat halaman '.self::NAMA_PERAN[$user->role].'.',
                403,
                'wrong_role',
                ['registered_role' => $user->role],
            );
        }

        if (! $user->is_active) {
            throw new OtpDitolak('Akun ini dinonaktifkan. Hubungi tim Life of Foods.', 403, 'account_inactive');
        }

        if ($user->phone_verified_at === null) {
            $user->forceFill(['phone_verified_at' => now()])->save();
        }

        // Bentuk respons sama dengan POST /auth/google (ADR-0006).
        return response()->json([
            'token' => $user->createToken('android-otp')->plainTextToken,
            // true berarti Android membuka K04 (Lengkapi profil) sebelum beranda.
            'is_new_user' => $akunBaru,
            'user' => $user->only(['id', 'name', 'email', 'phone', 'role']),
        ], $akunBaru ? 201 : 200);
    }

    private function nomor(string $masukan): string
    {
        return NomorHp::normalisasi($masukan) ?? throw ValidationException::withMessages([
            'phone' => 'Nomor HP tidak valid. Gunakan nomor seluler Indonesia, contoh 081234567890.',
        ]);
    }
}
