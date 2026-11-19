<?php

use App\Http\Controllers\BagikanController;
use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return view('welcome');
});

// Tautan bagikan jualan (K10/K11 → WhatsApp). Pratinjau Open Graph dan tombol buka aplikasi.
Route::get('/j/{id}', [BagikanController::class, 'jualan'])->whereNumber('id');
