<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Support\NomorHp;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * M15 Pengaturan toko dan kasir (F-16). Semua aksi hanya untuk pemilik:
 * kasir ditolak di kelola anggota (peta layar, catatan M15).
 */
class AnggotaTokoController extends Controller
{
    use AksesToko;

    /** GET /api/partner/stores/{store}/members */
    public function daftar(Request $request, int $store): JsonResponse
    {
        $toko = $this->tokoMilik($request, $store);
        $pemilik = DB::table('users')->where('id', $toko->owner_user_id)->first(['id', 'name', 'phone']);

        $anggota = DB::table('store_members as m')->join('users as u', 'u.id', '=', 'm.user_id')
            ->where('m.store_id', $store)->whereNull('m.revoked_at')->where('m.user_id', '!=', $toko->owner_user_id)
            ->orderByRaw("m.role = 'owner' DESC")->orderBy('m.id')
            ->get(['m.id', 'm.role', 'm.invited_at', 'u.id as user_id', 'u.name', 'u.phone']);

        $baris = collect([[
            'id' => null, 'user_id' => $pemilik->id, 'name' => $pemilik->name, 'phone' => $pemilik->phone,
            'role' => 'owner', 'is_store_owner' => true, 'invited_at' => null,
        ]])->concat($anggota->map(fn (object $m) => $this->bentuk($m)));

        return response()->json(['data' => $baris->values()]);
    }

    /**
     * POST /api/partner/stores/{store}/members  { phone, name }
     * "Undang kasir lewat WhatsApp": akun kasir dibuat di sini, lalu Android
     * membagikan invite_message lewat intent WhatsApp. Kasir masuk dengan OTP
     * di M01 memakai nomor itu.
     */
    public function undang(Request $request, int $store): JsonResponse
    {
        $toko = $this->tokoMilik($request, $store);
        $f = $request->validate([
            'phone' => ['required', 'string', 'max:20'],
            'name' => ['required', 'string', 'min:2', 'max:120'],
        ]);
        $phone = NomorHp::normalisasi($f['phone'])
            ?? throw ValidationException::withMessages(['phone' => 'Nomor HP tidak valid.']);

        $id = DB::transaction(function () use ($request, $toko, $f, $phone) {
            $user = User::where('phone', $phone)->lockForUpdate()->first();

            // Satu akun satu peran: nomor konsumen tidak diubah diam-diam jadi mitra.
            abort_if($user !== null && $user->role !== 'partner', 409, 'Nomor ini sudah dipakai akun konsumen. Pakai nomor lain untuk kasir.');
            abort_if($user !== null && ! $user->is_active, 409, 'Akun dengan nomor ini sedang dinonaktifkan.');
            abort_if($user !== null && $user->id === (int) $toko->owner_user_id, 409, 'Nomor ini milik pemilik toko.');

            $user ??= User::forceCreate(['phone' => $phone, 'name' => $f['name'], 'role' => 'partner']);

            $lama = DB::table('store_members')->where('store_id', $toko->id)->where('user_id', $user->id)->first();
            abort_if($lama !== null && $lama->revoked_at === null, 409, 'Nomor ini sudah jadi anggota toko.');

            if ($lama !== null) {
                // Kasir yang pernah dicabut diundang ulang: selalu kembali sebagai kasir.
                DB::table('store_members')->where('id', $lama->id)
                    ->update(['role' => 'cashier', 'revoked_at' => null, 'invited_at' => now(), 'updated_at' => now()]);
                $id = $lama->id;
            } else {
                $id = DB::table('store_members')->insertGetId([
                    'store_id' => $toko->id, 'user_id' => $user->id, 'role' => 'cashier',
                    'invited_at' => now(), 'created_at' => now(), 'updated_at' => now(),
                ]);
            }

            $this->catatAudit($request, $toko->id, 'store_member.invite', $id);

            return $id;
        });

        $m = $this->ambil($id);

        return response()->json(['data' => $this->bentuk($m) + [
            'invite_message' => "Halo {$m->name}, kamu diundang jadi kasir {$toko->name} di Life of Foods. "
                .'Unduh aplikasinya, pilih Masuk mitra, lalu masuk dengan nomor ini.',
        ]], 201);
    }

    /** DELETE /api/partner/stores/{store}/members/{member} */
    public function cabut(Request $request, int $store, int $member): Response
    {
        $toko = $this->tokoMilik($request, $store);
        $m = DB::table('store_members')->where('id', $member)->where('store_id', $store)->whereNull('revoked_at')->first();

        abort_if($m === null, 404, 'Anggota tidak ditemukan.');
        abort_if($m->user_id === $request->user()->id || (int) $m->user_id === (int) $toko->owner_user_id, 409, 'Pemilik toko tidak bisa dicabut dari sini.');

        DB::transaction(function () use ($request, $store, $member) {
            DB::table('store_members')->where('id', $member)->update(['revoked_at' => now(), 'updated_at' => now()]);
            $this->catatAudit($request, $store, 'store_member.revoke', $member);
        });

        // Token kasir tidak dicabut: bisa jadi ia kasir di toko lain. Akses ke toko ini
        // langsung hilang karena AksesToko memeriksa revoked_at di setiap permintaan.
        return response()->noContent();
    }

    private function ambil(int $id): object
    {
        return DB::table('store_members as m')->join('users as u', 'u.id', '=', 'm.user_id')->where('m.id', $id)
            ->first(['m.id', 'm.role', 'm.invited_at', 'u.id as user_id', 'u.name', 'u.phone']);
    }

    private function bentuk(object $m): array
    {
        return [
            'id' => $m->id, 'user_id' => $m->user_id, 'name' => $m->name, 'phone' => $m->phone,
            'role' => $m->role, 'is_store_owner' => false,
            'invited_at' => $m->invited_at === null ? null : Carbon::parse($m->invited_at)->toIso8601String(),
        ];
    }

    private function catatAudit(Request $request, int $store, string $aksi, int $member): void
    {
        // Nomor HP tidak disimpan di audit, cukup id anggota.
        DB::table('audit_logs')->insert([
            'user_id' => $request->user()->id, 'store_id' => $store, 'action' => $aksi,
            'subject_type' => 'store_member', 'subject_id' => $member,
            'ip' => $request->ip(), 'created_at' => now(), 'updated_at' => now(),
        ]);
    }
}
