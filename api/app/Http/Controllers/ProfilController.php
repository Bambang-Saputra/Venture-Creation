<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * GET /api/me (K18 Profil, dan pemeriksaan sesi saat aplikasi dibuka).
 */
class ProfilController extends Controller
{
    /**
     * PATCH /api/me (K04 Lengkapi profil, K19 Edit profil, K20 Pengaturan).
     *
     * Semua kolom `sometimes`: K20 cukup mengirim satu sakelar notifikasi.
     * Kolom consumer_profiles hanya untuk konsumen.
     */
    public function ubah(Request $request): JsonResponse
    {
        $user = $request->user();
        $konsumen = $user->role === 'consumer';
        $kolomProfil = ['area_label', 'latitude', 'longitude', 'notify_favorite_store', 'notify_pickup_reminder', 'notify_promo'];

        // Mitra tidak punya consumer_profiles: kolomnya ditolak, bukan diabaikan diam-diam.
        $khususKonsumen = fn (array $aturan) => $konsumen ? $aturan : ['prohibited'];

        $data = $request->validate([
            'name' => ['sometimes', 'required', 'string', 'min:2', 'max:120'],
            'email' => ['sometimes', 'nullable', 'email', 'max:160', Rule::unique('users', 'email')->ignore($user->id)],
            'area_label' => $khususKonsumen(['sometimes', 'required', 'string', 'max:120']),
            // Tanpa `sometimes`: required_with harus tetap diperiksa saat pasangannya tidak dikirim.
            'latitude' => $khususKonsumen(['nullable', 'numeric', 'between:-90,90', 'required_with:longitude']),
            'longitude' => $khususKonsumen(['nullable', 'numeric', 'between:-180,180', 'required_with:latitude']),
            'notify_favorite_store' => $khususKonsumen(['sometimes', 'boolean']),
            'notify_pickup_reminder' => $khususKonsumen(['sometimes', 'boolean']),
            'notify_promo' => $khususKonsumen(['sometimes', 'boolean']),
        ]);

        $profil = array_intersect_key($data, array_flip($kolomProfil));

        DB::transaction(function () use ($user, $data, $profil) {
            $user->fill(array_intersect_key($data, array_flip(['name', 'email'])))->save();

            if ($profil !== []) {
                DB::table('consumer_profiles')->updateOrInsert(
                    ['user_id' => $user->id],
                    [...$profil, 'updated_at' => now()],
                );
                DB::table('consumer_profiles')->where('user_id', $user->id)->whereNull('created_at')->update(['created_at' => now()]);
            }
        });

        return $this->tampil($request);
    }

    public function tampil(Request $request): JsonResponse
    {
        $user = $request->user();

        $profil = $user->role === 'consumer'
            ? DB::table('consumer_profiles')->where('user_id', $user->id)->first()
            : null;

        return response()->json([
            'user' => [
                ...$user->only(['id', 'name', 'email', 'phone', 'role']),
                'has_google' => $user->google_sub !== null,
            ],
            // false berarti user menutup aplikasi sebelum K04 selesai:
            // Android membuka K04 lagi, bukan beranda.
            'is_profile_complete' => $user->name !== null,
            'consumer_profile' => $profil === null ? null : [
                'area_label' => $profil->area_label,
                'latitude' => $profil->latitude === null ? null : (float) $profil->latitude,
                'longitude' => $profil->longitude === null ? null : (float) $profil->longitude,
                'notify_favorite_store' => (bool) $profil->notify_favorite_store,
                'notify_pickup_reminder' => (bool) $profil->notify_pickup_reminder,
                'notify_promo' => (bool) $profil->notify_promo,
            ],
            'allergens' => $user->role === 'consumer' ? AlergenController::milik($user->id) : [],
        ]);
    }
}
