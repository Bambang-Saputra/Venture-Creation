<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * M03 Daftar jadi mitra dan M04 Menunggu verifikasi.
 *
 * Akun mitra baru (dibuat saat verifikasi OTP di halaman mitra) belum punya toko. Android memanggil
 * GET /partner/application setiap kali GET /partner/stores kosong untuk menentukan layar mana yang
 * dibuka: M03 (belum mendaftar atau ditolak), M04 (menunggu), atau M05 (sudah disetujui).
 */
class PendaftaranMitraController extends Controller
{
    /** GET /api/partner/application: pendaftaran terakhir akun ini, atau data null. */
    public function tampil(Request $request): JsonResponse
    {
        $p = DB::table('partner_applications')->where('user_id', $request->user()->id)->latest('id')->first();

        return response()->json(['data' => $p === null ? null : $this->bentuk($p)]);
    }

    /**
     * POST /api/partner/application. Satu akun hanya boleh punya satu pendaftaran yang menunggu,
     * dan akun yang sudah punya toko tidak perlu mendaftar lagi (409).
     */
    public function kirim(Request $request): JsonResponse
    {
        $user = $request->user();
        abort_if($user->role !== 'partner', 403, 'Hanya akun mitra yang bisa mendaftarkan toko.');

        $f = $request->validate([
            'owner_name' => ['required', 'string', 'min:2', 'max:120'],
            'store_name' => ['required', 'string', 'min:2', 'max:140'],
            'category' => ['required', Rule::in(['cafe', 'bakery', 'resto', 'catering', 'grocery'])],
            'address' => ['required', 'string', 'min:10', 'max:255'],
            'latitude' => ['required_with:longitude', 'nullable', 'numeric', 'between:-90,90'],
            'longitude' => ['required_with:latitude', 'nullable', 'numeric', 'between:-180,180'],
            'open_time' => ['required', 'date_format:H:i'],
            'close_time' => ['required', 'date_format:H:i', 'after:open_time'],
            // NIB dari OSS selalu 13 digit angka. Opsional.
            'nib' => ['nullable', 'string', 'regex:/^[0-9]{13}$/'],
            'halal_certificate_no' => ['nullable', 'string', 'max:80'],
        ], [
            'nib.regex' => 'NIB terdiri dari 13 angka. Kosongkan kalau belum punya.',
            'close_time.after' => 'Jam tutup harus setelah jam buka.',
            'address.min' => 'Tulis alamat lengkap, termasuk nama jalan dan nomor.',
        ]);

        $punyaToko = DB::table('stores')->where('owner_user_id', $user->id)->exists()
            || DB::table('store_members')->where('user_id', $user->id)->whereNull('revoked_at')->exists();
        abort_if($punyaToko, 409, 'Akun ini sudah terhubung ke toko.');

        abort_if(
            DB::table('partner_applications')->where('user_id', $user->id)->where('status', 'pending')->exists(),
            409,
            'Pendaftaranmu masih menunggu verifikasi tim.',
        );

        $id = DB::table('partner_applications')->insertGetId([
            ...collect($f)->map(fn ($v) => is_string($v) ? trim($v) : $v)->all(),
            'nib' => blank($f['nib'] ?? null) ? null : $f['nib'],
            'halal_certificate_no' => blank($f['halal_certificate_no'] ?? null) ? null : trim($f['halal_certificate_no']),
            'user_id' => $user->id,
            'status' => 'pending',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return response()->json(['data' => $this->bentuk(DB::table('partner_applications')->find($id))], 201);
    }

    private function bentuk(object $p): array
    {
        return [
            'id' => $p->id,
            'status' => $p->status,
            'owner_name' => $p->owner_name,
            'store_name' => $p->store_name,
            'category' => $p->category,
            'address' => $p->address,
            'latitude' => $p->latitude === null ? null : (float) $p->latitude,
            'longitude' => $p->longitude === null ? null : (float) $p->longitude,
            'open_time' => substr((string) $p->open_time, 0, 5),
            'close_time' => substr((string) $p->close_time, 0, 5),
            'nib' => $p->nib,
            'halal_certificate_no' => $p->halal_certificate_no,
            'rejection_reason' => $p->rejection_reason,
            'store_id' => $p->store_id,
            'submitted_at' => Carbon::parse($p->created_at)->toIso8601String(),
        ];
    }
}
