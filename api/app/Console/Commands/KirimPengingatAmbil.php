<?php

namespace App\Console\Commands;

use App\Services\Notifikasi;
use Illuminate\Console\Command;

/**
 * Dijalankan scheduler tiap 5 menit (routes/console.php). Mengirim
 * "Pesananmu siap diambil" 30 menit sebelum jam ambil dimulai (K20
 * "Pengingat jam ambil"). Satu pengingat per pesanan, aman dijalankan berulang.
 */
class KirimPengingatAmbil extends Command
{
    protected $signature = 'notifikasi:pengingat-ambil';

    protected $description = 'Kirim pengingat 30 menit sebelum jam ambil pesanan dimulai';

    public function handle(Notifikasi $notifikasi): int
    {
        $this->info("Pengingat terkirim: {$notifikasi->pengingatAmbil()}");

        return self::SUCCESS;
    }
}
