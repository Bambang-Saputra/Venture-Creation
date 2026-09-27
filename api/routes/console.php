<?php

use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Schedule;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote');

// Lokal: jalankan `php artisan schedule:work` di terminal terpisah.
Schedule::command('pesanan:tutup-yang-lewat')->everyFiveMinutes()->withoutOverlapping();
Schedule::command('notifikasi:pengingat-ambil')->everyFiveMinutes()->withoutOverlapping();
