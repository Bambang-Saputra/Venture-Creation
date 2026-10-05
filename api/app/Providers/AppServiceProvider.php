<?php

namespace App\Providers;

use App\Support\NomorHp;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        //
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        // Batas utama dihitung per nomor HP. Penguji di satu Wi-Fi/hotspot keluar lewat
        // satu IP publik, jadi batas per IP saja (throttle:5,1) membuat mereka saling
        // menghabiskan jatah. Batas per IP tetap ada, tapi longgar, sebagai pengaman.
        RateLimiter::for('otp-minta', fn (Request $request) => [
            Limit::perMinute(3)->by('otp-minta:nomor:'.$this->kunciNomor($request)),
            Limit::perMinute(30)->by('otp-minta:ip:'.$request->ip()),
        ]);
        RateLimiter::for('otp-verifikasi', fn (Request $request) => [
            Limit::perMinute(10)->by('otp-verifikasi:nomor:'.$this->kunciNomor($request)),
            Limit::perMinute(60)->by('otp-verifikasi:ip:'.$request->ip()),
        ]);
    }

    /** Nomor yang sudah dinormalisasi, supaya 0812… dan 62812… berbagi satu jatah. */
    private function kunciNomor(Request $request): string
    {
        $masukan = $request->input('phone');

        return (is_string($masukan) ? NomorHp::normalisasi($masukan) : null) ?? 'ip:'.$request->ip();
    }
}
