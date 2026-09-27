<?php

use App\Http\Controllers\Auth\LoginGoogleController;
use App\Http\Controllers\Auth\OtpController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

Route::post('/auth/otp/request', [OtpController::class, 'minta'])->middleware('throttle:5,1');
Route::post('/auth/otp/verify', [OtpController::class, 'verifikasi'])->middleware('throttle:10,1');
Route::post('/auth/google', LoginGoogleController::class)->middleware('throttle:10,1');

Route::get('/user', function (Request $request) {
    return $request->user();
})->middleware('auth:sanctum');
