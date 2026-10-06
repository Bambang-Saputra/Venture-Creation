<?php

namespace App\Services;

use Illuminate\Database\Query\Builder;
use Illuminate\Support\Facades\DB;

/**
 * Paket iklan mitra dan aturan kapan sebuah iklan dianggap tayang.
 *
 * "Populer hari ini" tidak pernah memakai data ini: urutan populer hanya dari jumlah pesanan.
 */
class Promosi
{
    public const PRIORITAS = 'search_priority';

    public const BANNER = 'home_banner';

    /** Tarif per hari tayang (keputusan tim, 6 Okt 2026). */
    public const TARIF = [
        self::PRIORITAS => 5000,
        self::BANNER => 10000,
    ];

    public const MAKS_HARI = 30;

    /** Jumlah jualan berlabel "Iklan" paling banyak di satu halaman, supaya hasil biasa tetap terlihat. */
    public const MAKS_SLOT_PRIORITAS = 2;

    public const MAKS_BANNER = 5;

    /** Subquery WHERE EXISTS: toko ini punya iklan paket $paket yang tayang hari ini. */
    public static function tayangHariIni(Builder $q, string $paket, string $kolomToko = 'stores.id'): Builder
    {
        $hariIni = today()->toDateString();

        return $q->whereExists(fn (Builder $sub) => $sub
            ->from('store_promotions')
            ->whereColumn('store_promotions.store_id', $kolomToko)
            ->where('store_promotions.package', $paket)
            ->where('store_promotions.starts_on', '<=', $hariIni)
            ->where('store_promotions.ends_on', '>=', $hariIni));
    }

    /** Tanggal terakhir iklan paket ini milik toko masih tayang, atau null kalau tidak ada. */
    public static function berakhirTerakhir(int $store, string $paket): ?string
    {
        return DB::table('store_promotions')
            ->where('store_id', $store)
            ->where('package', $paket)
            ->where('ends_on', '>=', today()->toDateString())
            ->max('ends_on');
    }
}
