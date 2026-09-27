<?php

use App\Http\Controllers\AlergenController;
use App\Http\Controllers\Auth\KeluarController;
use App\Http\Controllers\Auth\LoginGoogleController;
use App\Http\Controllers\Auth\OtpController;
use App\Http\Controllers\ProfilController;
use Illuminate\Support\Facades\Route;

Route::post('/auth/otp/request', [OtpController::class, 'minta'])->middleware('throttle:5,1');
Route::post('/auth/otp/verify', [OtpController::class, 'verifikasi'])->middleware('throttle:10,1');
Route::post('/auth/google', LoginGoogleController::class)->middleware('throttle:10,1');

// Data rujukan tanpa data pribadi, jadi tidak butuh token.
Route::get('/allergens', [AlergenController::class, 'daftar']);

Route::middleware(['auth:sanctum', 'aktif'])->group(function () {
    Route::post('/auth/logout', KeluarController::class);
    Route::get('/me', [ProfilController::class, 'tampil']);
    Route::patch('/me', [ProfilController::class, 'ubah']);
    Route::put('/me/allergens', [AlergenController::class, 'gantiMilikSaya']);
});
