<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

/**
 * Dijalankan scheduler tiap 5 menit (routes/console.php).
 *
 * - Pesanan yang belum diambil sampai jam ambil + toleransi lewat jadi
 *   no_show: kodenya kedaluwarsa, stok dipesan dilepas, saldo tertunda dikurangi.
 * - Listing aktif, jeda, atau habis yang jam ambilnya sudah lewat jadi expired.
 *
 * Aman dijalankan berulang: tiap pesanan diproses di transaksinya sendiri
 * dan diperiksa ulang setelah dikunci.
 */
class TutupPesananLewat extends Command
{
    protected $signature = 'pesanan:tutup-yang-lewat';

    protected $description = 'Tandai pesanan tak diambil sebagai no_show dan listing lewat jam ambil sebagai expired';

    // Sama dengan toleransi penukaran kode di PesananMitraController.
    public const TOLERANSI_MENIT = 30;

    public function handle(): int
    {
        $batas = now()->subMinutes(self::TOLERANSI_MENIT);
        $jumlah = 0;

        DB::table('orders')->where('status', 'pending_pickup')->where('pickup_end', '<', $batas)
            ->orderBy('id')->pluck('id')
            ->each(function (int $id) use ($batas, &$jumlah) {
                DB::transaction(function () use ($id, $batas, &$jumlah) {
                    $o = DB::table('orders')->where('id', $id)->lockForUpdate()->first();
                    if ($o->status !== 'pending_pickup' || $o->pickup_end >= $batas->toDateTimeString()) {
                        return;
                    }

                    $item = DB::table('order_items')->where('order_id', $o->id)->get();
                    DB::table('listings')->whereIn('id', $item->pluck('listing_id'))->orderBy('id')->lockForUpdate()->get();
                    foreach ($item as $i) {
                        DB::table('listings')->where('id', $i->listing_id)
                            ->update(['qty_reserved' => DB::raw('GREATEST(0, CAST(qty_reserved AS SIGNED) - '.(int) $i->qty.')')]);
                    }

                    DB::table('orders')->where('id', $o->id)->update(['status' => 'no_show', 'updated_at' => now()]);
                    DB::table('pickup_codes')->where('order_id', $o->id)->where('status', 'active')
                        ->update(['status' => 'expired', 'updated_at' => now()]);
                    DB::table('store_balances')->where('store_id', $o->store_id)->update([
                        'pending_rupiah' => DB::raw('GREATEST(0, CAST(pending_rupiah AS SIGNED) - '.(int) $o->total_rupiah.')'),
                        'updated_at' => now(),
                    ]);
                    $jumlah++;
                });
            });

        $kedaluwarsa = DB::table('listings')
            ->whereIn('status', ['active', 'paused', 'sold_out'])
            ->where('pickup_end', '<', now())
            ->update(['status' => 'expired', 'updated_at' => now()]);

        $this->info("{$jumlah} pesanan jadi no_show, {$kedaluwarsa} listing jadi expired.");

        return self::SUCCESS;
    }
}
