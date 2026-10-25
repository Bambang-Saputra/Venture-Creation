<?php

namespace Database\Seeders;

use App\Services\DataDemo;
use Illuminate\Database\Seeder;

/**
 * Dijalankan oleh php artisan db:seed.
 *
 * SeederPilot sengaja tidak dipanggil di sini. Data mitra sungguhan
 * hanya masuk kalau seseorang mengetik perintahnya dengan sadar:
 *   php artisan db:seed --class=SeederPilot
 */
class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call(SeederAlergen::class);

        if (! app()->environment('production')) {
            $this->call(SeederDemo::class);
            // Ulasan dan sisa data aktif lain yang belum dibuat SeederDemo. Hari berikutnya
            // cukup jalankan php artisan demo:segarkan, tanpa menghapus database.
            app(DataDemo::class)->segarkan();
        }
    }
}
