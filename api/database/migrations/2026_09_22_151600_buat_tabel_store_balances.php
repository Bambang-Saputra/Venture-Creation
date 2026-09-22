<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Ringkasan saldo per toko, isinya selalu hasil penjumlahan balance_transactions.
     * Selama uji coba angka ini hanya bisa dibaca: tim tidak memegang uang mitra,
     * dan tombol cairkan sengaja nonaktif.
     */
    public function up(): void
    {
        Schema::create('store_balances', function (Blueprint $table) {
            $table->unsignedBigInteger('store_id')->primary();
            // Sudah ditukar kodenya, pesanan selesai
            $table->unsignedInteger('available_rupiah')->default(0);
            // Sudah dipesan tapi belum diambil
            $table->unsignedInteger('pending_rupiah')->default(0);
            $table->unsignedInteger('lifetime_rupiah')->default(0);
            $table->timestamps();

            $table->foreign('store_id')->references('id')->on('stores')->cascadeOnDelete();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('store_balances');
    }
};
