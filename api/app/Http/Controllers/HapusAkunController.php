<?php

namespace App\Http\Controllers;

use App\Services\FotoUnggahan;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\DB;

/**
 * DELETE /api/me: "Hapus akun" di K20 (F-19).
 *
 * Akun tanpa riwayat pesanan dihapus penuh. Akun yang pernah memesan
 * dianonimkan: pesanan tetap ada karena menjadi catatan penjualan dan
 * laporan mitra (orders.user_id restrictOnDelete), tapi nomor HP, nama,
 * email, tautan Google, catatan pesanan, dan salinan alergi dikosongkan.
 * Nomor yang sama bisa mendaftar lagi sebagai akun baru.
 */
class HapusAkunController extends Controller
{
    public function __invoke(Request $request): Response
    {
        $user = $request->user();
        // Akun mitra terikat pada toko dan laporan; penutupannya lewat tim, bukan dari aplikasi.
        abort_if($user->role !== 'consumer', 403, 'Akun mitra hanya bisa ditutup lewat tim Life of Foods.');
        // Android menampilkan dialog konfirmasi dulu; ini penjaga kalau endpoint terpanggil tak sengaja.
        $request->validate(['confirm' => ['required', 'accepted']]);

        DB::transaction(function () use ($request, $user) {
            $u = DB::table('users')->where('id', $user->id)->lockForUpdate()->first();
            abort_if(DB::table('orders')->where('user_id', $u->id)->where('status', 'pending_pickup')->exists(),
                409, 'Masih ada pesanan yang belum diambil. Ambil atau batalkan dulu sebelum menghapus akun.');

            if ($u->phone !== null) {
                DB::table('otp_codes')->where('phone', $u->phone)->delete();
            }
            DB::table('personal_access_tokens')->where('tokenable_type', $user::class)->where('tokenable_id', $u->id)->delete();
            // Foto profil adalah wajah pengguna: selalu dihapus, apa pun jalurnya.
            FotoUnggahan::hapus($u->photo_path, "users/{$u->id}");

            // Tanpa nomor HP atau id pengguna: yang tersisa hanya bahwa sebuah akun dihapus.
            DB::table('audit_logs')->insert([
                'user_id' => null, 'action' => 'user.delete', 'subject_type' => 'user',
                'ip' => $request->ip(), 'created_at' => now(), 'updated_at' => now(),
            ]);

            if (! DB::table('orders')->where('user_id', $u->id)->exists()) {
                // consumer_profiles, user_allergens, favorites, notifications ikut terhapus (cascade).
                DB::table('users')->where('id', $u->id)->delete();

                return;
            }

            DB::table('orders')->where('user_id', $u->id)->update(['note' => null, 'allergen_snapshot' => null, 'updated_at' => now()]);
            foreach (['consumer_profiles', 'user_allergens', 'favorites', 'notifications'] as $tabel) {
                DB::table($tabel)->where('user_id', $u->id)->delete();
            }
            DB::table('users')->where('id', $u->id)->update([
                'phone' => null, 'name' => null, 'email' => null, 'google_sub' => null, 'phone_verified_at' => null,
                'photo_path' => null,
                'password' => null, 'remember_token' => null, 'is_active' => false, 'updated_at' => now(),
            ]);
        });

        return response()->noContent();
    }
}
