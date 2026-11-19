<?php

namespace App\Http\Controllers;

use App\Services\FotoUnggahan;
use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\View\View;

/**
 * Halaman tautan bagikan /j/{id}. Dibuka dari pesan WhatsApp: tag Open Graph membuat pratinjau
 * berfoto, dan tombol "Buka di aplikasi" membuka K10/K11 lewat intent-filter Android. Tanpa login,
 * karena penerimanya belum tentu punya akun.
 */
class BagikanController extends Controller
{
    public function jualan(int $id): View
    {
        $l = DB::table('listings')
            ->join('stores', 'stores.id', '=', 'listings.store_id')
            ->where('listings.id', $id)
            ->whereIn('listings.status', ['active', 'paused', 'sold_out', 'expired'])
            ->first([
                'listings.id', 'listings.title', 'listings.price_rupiah', 'listings.original_value_rupiah',
                'listings.photo_path', 'listings.pickup_start', 'listings.pickup_end', 'listings.status',
                'stores.name as store_name', 'stores.address as store_address', 'stores.photo_path as store_photo_path',
            ]);

        abort_if($l === null, 404, 'Jualan ini tidak ditemukan.');

        $hemat = $l->original_value_rupiah > $l->price_rupiah
            ? (int) round(($l->original_value_rupiah - $l->price_rupiah) * 100 / $l->original_value_rupiah)
            : null;
        $jam = Carbon::parse($l->pickup_start)->format('H.i').' sampai '.Carbon::parse($l->pickup_end)->format('H.i');

        return view('bagikan', [
            'l' => $l,
            'foto' => FotoUnggahan::url($l->photo_path ?? $l->store_photo_path),
            'harga' => self::rupiah($l->price_rupiah),
            'hargaAsli' => $l->original_value_rupiah > $l->price_rupiah ? self::rupiah($l->original_value_rupiah) : null,
            'hemat' => $hemat,
            'jam' => $jam,
            'masihAda' => $l->status === 'active' && Carbon::parse($l->pickup_end)->isFuture(),
            'tautanApp' => 'intent://'.request()->getHttpHost().'/j/'.$l->id
                .'#Intent;scheme=https;package=id.lifeoffoods;end',
        ]);
    }

    private static function rupiah(int $n): string
    {
        return 'Rp'.number_format($n, 0, ',', '.');
    }
}
