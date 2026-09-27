<?php

use App\Http\Controllers\AlergenController;
use App\Http\Controllers\Auth\KeluarController;
use App\Http\Controllers\Auth\LoginGoogleController;
use App\Http\Controllers\Auth\OtpController;
use App\Http\Controllers\ListingController;
use App\Http\Controllers\Partner\ListingMitraController;
use App\Http\Controllers\Partner\PesananMitraController;
use App\Http\Controllers\PesananController;
use App\Http\Controllers\ProfilController;
use Illuminate\Support\Facades\Route;

Route::post('/auth/otp/request', [OtpController::class, 'minta'])->middleware('throttle:5,1');
Route::post('/auth/otp/verify', [OtpController::class, 'verifikasi'])->middleware('throttle:10,1');
Route::post('/auth/google', LoginGoogleController::class)->middleware('throttle:10,1');

// Data rujukan tanpa data pribadi, jadi tidak butuh token.
Route::get('/allergens', [AlergenController::class, 'daftar']);
Route::get('/listings', [ListingController::class, 'daftar'])->middleware('throttle:60,1');
Route::get('/listings/{id}', [ListingController::class, 'detail'])->whereNumber('id')->middleware('throttle:60,1');

Route::middleware(['auth:sanctum', 'aktif'])->group(function () {
    Route::post('/auth/logout', KeluarController::class);
    Route::get('/me', [ProfilController::class, 'tampil']);
    Route::patch('/me', [ProfilController::class, 'ubah']);
    Route::put('/me/allergens', [AlergenController::class, 'gantiMilikSaya']);

    Route::post('/orders/preview', [PesananController::class, 'pratinjau'])->middleware('throttle:60,1');
    Route::post('/orders', [PesananController::class, 'buat'])->middleware('throttle:10,1');
    Route::get('/orders', [PesananController::class, 'daftar']);
    Route::get('/orders/{order}', [PesananController::class, 'detail'])->whereNumber('order');
    Route::post('/orders/{order}/cancel', [PesananController::class, 'batalkan'])->whereNumber('order');

    Route::prefix('/partner/stores/{store}')->whereNumber(['store', 'listing'])->group(function () {
        Route::get('/templates', [ListingMitraController::class, 'template']);
        Route::get('/products', [ListingMitraController::class, 'produk']);
        Route::get('/listings', [ListingMitraController::class, 'daftar']);
        Route::post('/listings', [ListingMitraController::class, 'buat'])->middleware('throttle:30,1');
        Route::post('/listings/{listing}/publish', [ListingMitraController::class, 'terbitkan']);
        Route::post('/listings/{listing}/pause', [ListingMitraController::class, 'jeda']);
        Route::get('/orders', [PesananMitraController::class, 'daftar']);
    });

    Route::post('/pickup-codes/redeem', [PesananMitraController::class, 'tukar'])->middleware('throttle:20,1');
});
