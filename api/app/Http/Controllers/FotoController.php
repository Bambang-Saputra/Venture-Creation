<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Partner\AksesToko;
use App\Services\FotoUnggahan;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * Unggah foto (F-20), multipart dengan field `photo`. Setiap unggahan
 * mengganti foto sebelumnya dan menghapus berkas lamanya.
 *
 * - POST/DELETE /me/photo: foto profil (K04, K19), semua peran.
 * - POST /partner/stores/{store}/photo: foto toko (M14), pemilik saja.
 * - POST /partner/stores/{store}/templates/{template}/photo: foto tas (M09),
 *   dipakai lagi setiap kali tas dari template itu dipasang.
 * - POST /partner/stores/{store}/listings/{listing}/photo: foto satu jualan.
 */
class FotoController extends Controller
{
    use AksesToko;

    public function profil(Request $request): JsonResponse
    {
        $request->validate(['photo' => FotoUnggahan::ATURAN]);
        $user = $request->user();

        return response()->json(['photo_url' => FotoUnggahan::url(
            $this->ganti('users', $user->id, "users/{$user->id}", $request),
        )]);
    }

    public function hapusProfil(Request $request): JsonResponse
    {
        $user = $request->user();
        FotoUnggahan::hapus(DB::table('users')->where('id', $user->id)->value('photo_path'), "users/{$user->id}");
        DB::table('users')->where('id', $user->id)->update(['photo_path' => null, 'updated_at' => now()]);

        return response()->json(['photo_url' => null]);
    }

    public function toko(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);
        $request->validate(['photo' => FotoUnggahan::ATURAN]);

        return response()->json(['photo_url' => FotoUnggahan::url(
            $this->ganti('stores', $store, "stores/{$store}", $request),
        )]);
    }

    public function template(Request $request, int $store, int $template): JsonResponse
    {
        $this->tokoMilik($request, $store);
        abort_unless(DB::table('surprise_bag_templates')->where('id', $template)->where('store_id', $store)->exists(),
            404, 'Template tidak ditemukan.');
        $request->validate(['photo' => FotoUnggahan::ATURAN]);

        return response()->json(['photo_url' => FotoUnggahan::url(
            $this->ganti('surprise_bag_templates', $template, "templates/{$template}", $request),
        )]);
    }

    public function listing(Request $request, int $store, int $listing): JsonResponse
    {
        $this->tokoMilik($request, $store);
        abort_unless(DB::table('listings')->where('id', $listing)->where('store_id', $store)->exists(),
            404, 'Jualan tidak ditemukan.');
        $request->validate(['photo' => FotoUnggahan::ATURAN]);

        return response()->json(['photo_url' => FotoUnggahan::url(
            $this->ganti('listings', $listing, "listings/{$listing}", $request),
        )]);
    }

    /** Simpan berkas baru dulu, baru hapus yang lama: kalau gagal di tengah, foto lama tetap ada. */
    private function ganti(string $tabel, int $id, string $folder, Request $request): string
    {
        $lama = DB::table($tabel)->where('id', $id)->value('photo_path');
        $baru = FotoUnggahan::simpan($request->file('photo'), $folder);
        DB::table($tabel)->where('id', $id)->update(['photo_path' => $baru, 'updated_at' => now()]);
        FotoUnggahan::hapus($lama, $folder);

        return $baru;
    }
}
