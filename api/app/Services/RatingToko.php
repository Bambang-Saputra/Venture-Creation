<?php

namespace App\Services;

use Illuminate\Database\Query\Builder;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Rating toko dari tabel reviews. Dipakai listing (K07, K10, K11), profil
 * toko (M14), dan riwayat pesanan (K15).
 */
final class RatingToko
{
    /** Ulasan bisa diberikan atau diubah sampai sekian hari setelah pesanan diambil. */
    public const BATAS_HARI = 7;

    /** Subquery store_id, rating_avg, rating_count untuk leftJoinSub(..., 'rating', ...). */
    public static function subquery(): Builder
    {
        return DB::table('reviews')
            ->selectRaw('store_id, AVG(rating) AS rating_avg, COUNT(*) AS rating_count')
            ->groupBy('store_id');
    }

    /**
     * @return array{rating_average: float|null, rating_count: int}
     */
    public static function format(mixed $rataRata, mixed $jumlah): array
    {
        $jumlah = (int) ($jumlah ?? 0);

        return [
            // Satu angka di belakang koma, seperti "4,8 (180)" di Figma M14.
            'rating_average' => $jumlah === 0 ? null : round((float) $rataRata, 1),
            'rating_count' => $jumlah,
        ];
    }

    public static function untukToko(int $idToko): array
    {
        $r = DB::table('reviews')->where('store_id', $idToko)
            ->selectRaw('AVG(rating) AS rata, COUNT(*) AS jumlah')->first();

        return self::format($r->rata, $r->jumlah);
    }

    /** Pesanan yang sudah selesai dan belum lewat BATAS_HARI. */
    public static function masihBisaDiulas(string $status, mixed $selesaiPada): bool
    {
        return $status === 'completed' && $selesaiPada !== null
            && Carbon::parse($selesaiPada)->addDays(self::BATAS_HARI)->isFuture();
    }
}
