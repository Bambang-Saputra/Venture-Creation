<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * GET /api/me (K18 Profil, dan pemeriksaan sesi saat aplikasi dibuka).
 */
class ProfilController extends Controller
{
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
        ]);
    }
}
