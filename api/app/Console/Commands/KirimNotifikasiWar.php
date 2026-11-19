<?php

namespace App\Console\Commands;

use App\Services\Notifikasi;
use Illuminate\Console\Command;

/**
 * Dijalankan scheduler tiap 15 menit dari jam 10.00 sampai 22.00 (routes/console.php). Mengirim
 * "War dimulai!" untuk jualan dengan diskon terbesar yang sebentar lagi berakhir, hanya ke
 * pembeli yang menyalakan notifikasi promo. Paling banyak dua per orang per hari.
 */
class KirimNotifikasiWar extends Command
{
    protected $signature = 'notifikasi:war';

    protected $description = 'Kirim notifikasi "War dimulai!" untuk jualan diskon terbesar yang sebentar lagi berakhir';

    public function handle(Notifikasi $notifikasi): int
    {
        $this->info("Notifikasi war terkirim: {$notifikasi->warDimulai()}");

        return self::SUCCESS;
    }
}
