<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * Rating dan ulasan (rencana-lanjutan.md). Satu ulasan per pesanan, hanya
 * untuk pesanan yang sudah diambil, jadi yang menilai pasti pernah datang
 * ke toko itu. Rating toko dihitung dari tabel ini saat dibaca, tidak
 * disimpan di stores, supaya tidak ada angka yang bisa melenceng.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('reviews', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->unique()->constrained()->cascadeOnDelete();
            // Disalin dari orders supaya rata-rata per toko cukup membaca satu tabel.
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->foreignId('user_id')->constrained('users')->restrictOnDelete();
            $table->unsignedTinyInteger('rating');
            $table->string('comment', 500)->nullable();
            $table->timestamps();

            $table->index(['store_id', 'created_at'], 'idx_reviews_toko_waktu');
        });

        DB::statement('ALTER TABLE reviews ADD CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)');
    }

    public function down(): void
    {
        Schema::dropIfExists('reviews');
    }
};
