<?php

use App\Http\Controllers\AlergenController;
use App\Http\Controllers\Auth\KeluarController;
use App\Http\Controllers\Auth\LoginGoogleController;
use App\Http\Controllers\Auth\OtpController;
use App\Http\Controllers\FavoritController;
use App\Http\Controllers\FotoController;
use App\Http\Controllers\HapusAkunController;
use App\Http\Controllers\ListingController;
use App\Http\Controllers\NotifikasiController;
use App\Http\Controllers\Partner\AnggotaTokoController;
use App\Http\Controllers\Partner\CatatSisaController;
use App\Http\Controllers\Partner\DasborMitraController;
use App\Http\Controllers\Partner\ListingMitraController;
use App\Http\Controllers\Partner\PendaftaranMitraController;
use App\Http\Controllers\Partner\PesananMitraController;
use App\Http\Controllers\Partner\PromosiMitraController;
use App\Http\Controllers\Partner\TokoController;
use App\Http\Controllers\PesananController;
use App\Http\Controllers\ProfilController;
use App\Http\Controllers\UlasanController;
use Illuminate\Support\Facades\Route;

Route::post('/auth/otp/request', [OtpController::class, 'minta'])->middleware('throttle:otp-minta');
Route::post('/auth/otp/verify', [OtpController::class, 'verifikasi'])->middleware('throttle:otp-verifikasi');
Route::post('/auth/google', LoginGoogleController::class)->middleware('throttle:10,1');

// Data rujukan tanpa data pribadi, jadi tidak butuh token.
Route::get('/allergens', [AlergenController::class, 'daftar']);
Route::get('/listings', [ListingController::class, 'daftar'])->middleware('throttle:60,1');
Route::get('/listings/{id}', [ListingController::class, 'detail'])->whereNumber('id')->middleware('throttle:60,1');
Route::get('/promotions/banners', [ListingController::class, 'banner'])->middleware('throttle:60,1');

Route::middleware(['auth:sanctum', 'aktif'])->group(function () {
    Route::post('/auth/logout', KeluarController::class);
    Route::get('/me', [ProfilController::class, 'tampil']);
    Route::patch('/me', [ProfilController::class, 'ubah']);
    Route::get('/me/impact', [ProfilController::class, 'dampak']);
    Route::put('/me/allergens', [AlergenController::class, 'gantiMilikSaya']);
    Route::delete('/me', HapusAkunController::class)->middleware('throttle:3,1');
    Route::post('/me/photo', [FotoController::class, 'profil'])->middleware('throttle:10,1');
    Route::delete('/me/photo', [FotoController::class, 'hapusProfil']);

    Route::get('/notifications', [NotifikasiController::class, 'daftar']);
    Route::post('/notifications/read-all', [NotifikasiController::class, 'bacaSemua']);
    Route::post('/notifications/{notification}/read', [NotifikasiController::class, 'baca'])->whereNumber('notification');

    Route::get('/favorites', [FavoritController::class, 'daftar']);
    Route::post('/favorites', [FavoritController::class, 'tambah'])->middleware('throttle:30,1');
    Route::delete('/favorites/{store}', [FavoritController::class, 'hapus'])->whereNumber('store');

    Route::post('/orders/preview', [PesananController::class, 'pratinjau'])->middleware('throttle:60,1');
    Route::post('/orders', [PesananController::class, 'buat'])->middleware('throttle:10,1');
    Route::get('/orders', [PesananController::class, 'daftar']);
    Route::get('/orders/{order}', [PesananController::class, 'detail'])->whereNumber('order');
    Route::post('/orders/{order}/cancel', [PesananController::class, 'batalkan'])->whereNumber('order');
    Route::post('/orders/{order}/review', [UlasanController::class, 'simpan'])->whereNumber('order')->middleware('throttle:20,1');

    Route::get('/partner/stores', [TokoController::class, 'daftar']);
    // M03/M04: akun mitra baru mendaftarkan toko lalu menunggu verifikasi tim.
    Route::get('/partner/application', [PendaftaranMitraController::class, 'tampil']);
    Route::post('/partner/application', [PendaftaranMitraController::class, 'kirim'])->middleware('throttle:5,1');
    Route::prefix('/partner/stores/{store}')->whereNumber(['store', 'listing'])->group(function () {
        Route::get('/', [TokoController::class, 'tampil']);
        Route::match(['put', 'patch'], '/', [TokoController::class, 'ubah'])->middleware('throttle:30,1');
        Route::post('/photo', [FotoController::class, 'toko'])->middleware('throttle:10,1');
        Route::post('/templates/{template}/photo', [FotoController::class, 'template'])->whereNumber('template')->middleware('throttle:10,1');
        Route::post('/listings/{listing}/photo', [FotoController::class, 'listing'])->middleware('throttle:10,1');
        Route::get('/balance', [TokoController::class, 'saldo']);
        Route::get('/balance/transactions', [TokoController::class, 'transaksi']);
        Route::get('/promotions', [PromosiMitraController::class, 'daftar']);
        Route::post('/promotions', [PromosiMitraController::class, 'buat'])->middleware('throttle:10,1');
        Route::get('/reviews', [UlasanController::class, 'daftarToko']);
        Route::get('/members', [AnggotaTokoController::class, 'daftar']);
        Route::post('/members', [AnggotaTokoController::class, 'undang'])->middleware('throttle:10,1');
        Route::delete('/members/{member}', [AnggotaTokoController::class, 'cabut'])->whereNumber('member');
        Route::get('/templates', [ListingMitraController::class, 'template']);
        Route::get('/products', [ListingMitraController::class, 'produk']);
        Route::post('/products', [ListingMitraController::class, 'tambahProduk'])->middleware('throttle:30,1');
        Route::get('/listings', [ListingMitraController::class, 'daftar']);
        Route::post('/listings', [ListingMitraController::class, 'buat'])->middleware('throttle:30,1');
        Route::post('/listings/{listing}/publish', [ListingMitraController::class, 'terbitkan']);
        Route::post('/listings/{listing}/pause', [ListingMitraController::class, 'jeda']);
        Route::get('/orders', [PesananMitraController::class, 'daftar']);
        Route::get('/waste-logs', [CatatSisaController::class, 'tampil']);
        Route::post('/waste-logs', [CatatSisaController::class, 'simpan'])->middleware('throttle:30,1');
        Route::get('/reports/weekly', [CatatSisaController::class, 'laporanMingguan']);
        Route::get('/summary', [DasborMitraController::class, 'ringkasan']);
        Route::get('/suggestions', [DasborMitraController::class, 'saran']);
        Route::post('/suggestions/{suggestion}/{aksi}', [DasborMitraController::class, 'tanggapi'])
            ->whereNumber('suggestion')->whereIn('aksi', ['accept', 'dismiss']);
        Route::patch('/products/{product}', [DasborMitraController::class, 'ubahProduksi'])->whereNumber('product');
    });

    Route::post('/pickup-codes/redeem', [PesananMitraController::class, 'tukar'])->middleware('throttle:20,1');
});
