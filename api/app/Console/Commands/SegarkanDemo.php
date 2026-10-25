<?php

namespace App\Console\Commands;

use App\Services\DataDemo;
use Illuminate\Console\Command;

/**
 * Bikin data demo terasa seperti aplikasi aktif, tanpa menghapus apa pun:
 *
 *   php artisan demo:segarkan
 *
 * Jalankan sebelum tiap sesi uji coba atau demo. Isinya jualan hari ini dan besok, iklan,
 * pesanan hari ini ("Populer hari ini"), dan ulasan (rating toko), hanya untuk akun dan
 * toko demo dari SeederDemo. Aman dijalankan berulang kali.
 */
class SegarkanDemo extends Command
{
    protected $signature = 'demo:segarkan {--force : tetap jalan di production}';

    protected $description = 'Segarkan data demo: jualan hari ini, iklan, pesanan, dan ulasan';

    public function handle(DataDemo $demo): int
    {
        if ($this->laravel->environment('production') && ! $this->option('force')) {
            $this->error('Ini server production. Data demo tidak diisi kecuali memakai --force.');

            return self::FAILURE;
        }

        $hasil = $demo->segarkan();
        $this->table(['Bagian', 'Dibuat'], collect($hasil)->map(fn ($n, $k) => [str_replace('_', ' ', $k), $n])->values()->all());

        if (array_sum($hasil) === 0) {
            $this->info('Data demo hari ini sudah lengkap, tidak ada yang perlu ditambah.');
        }

        return self::SUCCESS;
    }
}
