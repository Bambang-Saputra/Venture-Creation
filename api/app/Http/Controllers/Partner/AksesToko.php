<?php

namespace App\Http\Controllers\Partner;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * Akses toko untuk rute /partner/stores/{store}/...
 *
 * Pemilik: stores.owner_user_id, atau anggota berperan owner yang belum
 * dicabut. Kasir hanya boleh mencatat sisa, melihat pesanan masuk, dan
 * mencocokkan kode pickup. Toko orang lain dijawab 404, bukan 403, supaya
 * id toko tidak bisa ditebak.
 */
trait AksesToko
{
    protected function tokoMilik(Request $request, int $store, bool $pemilikSaja = true): object
    {
        $user = $request->user();
        $toko = DB::table('stores')->where('id', $store)->first();

        $pemilik = $toko !== null && (int) $toko->owner_user_id === $user->id;
        $anggota = $toko === null ? null : DB::table('store_members')
            ->where('store_id', $store)->where('user_id', $user->id)->whereNull('revoked_at')->value('role');

        abort_if($toko === null || (! $pemilik && $anggota === null), 404, 'Toko tidak ditemukan.');
        abort_if($pemilikSaja && ! $pemilik && $anggota !== 'owner', 403, 'Hanya pemilik toko yang bisa melakukan ini.');

        return $toko;
    }
}
