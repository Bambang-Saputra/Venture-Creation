<?php

namespace Database\Seeders;

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
        }
    }
}
