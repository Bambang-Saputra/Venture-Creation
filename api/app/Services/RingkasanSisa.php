<?php

namespace App\Services;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Angka sisa dan penyelamatan per rentang tanggal. Dipakai M05 Dashboard
 * dan M07 Laporan mingguan supaya keduanya selalu menampilkan angka yang sama.
 */
class RingkasanSisa
{
    /**
     * @return array{wasted_value_rupiah: int, wasted_weight_gram: int, logged_days: int, rescued_value_rupiah: int, orders_count: int, items_sold: int}
     */
    public function rentang(int $store, Carbon $mulai, Carbon $akhir): array
    {
        $sisa = DB::table('waste_logs')->where('store_id', $store)
            ->whereBetween('log_date', [$mulai->toDateString(), $akhir->toDateString()])
            ->selectRaw('COALESCE(SUM(total_value_rupiah), 0) v, COALESCE(SUM(total_weight_gram), 0) w, COUNT(*) d')->first();

        $pesanan = DB::table('orders')->where('store_id', $store)->where('status', 'completed')
            ->whereBetween('completed_at', [$mulai->copy()->startOfDay(), $akhir->copy()->endOfDay()]);

        return [
            'wasted_value_rupiah' => (int) $sisa->v,
            'wasted_weight_gram' => (int) $sisa->w,
            'logged_days' => (int) $sisa->d,
            'rescued_value_rupiah' => (int) (clone $pesanan)->sum('total_rupiah'),
            'orders_count' => (clone $pesanan)->count(),
            'items_sold' => (int) DB::table('order_items')->whereIn('order_id', (clone $pesanan)->select('id'))->sum('qty'),
        ];
    }

    /**
     * Nilai sisa per hari, Senin sampai Minggu. null = hari itu tidak dicatat.
     *
     * @return array<int, array{date: string, wasted_value_rupiah: ?int}>
     */
    public function harian(int $store, Carbon $senin): array
    {
        $nilai = DB::table('waste_logs')->where('store_id', $store)
            ->whereBetween('log_date', [$senin->toDateString(), $senin->copy()->addDays(6)->toDateString()])
            ->pluck('total_value_rupiah', 'log_date');

        return array_map(function (int $h) use ($senin, $nilai) {
            $tgl = $senin->copy()->addDays($h)->toDateString();

            return ['date' => $tgl, 'wasted_value_rupiah' => isset($nilai[$tgl]) ? (int) $nilai[$tgl] : null];
        }, range(0, 6));
    }
}
