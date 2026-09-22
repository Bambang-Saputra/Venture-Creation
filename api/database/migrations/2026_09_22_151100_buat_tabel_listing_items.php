<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Rincian isi sebuah listing, sifatnya opsional.
     *
     * Menu satuan tidak memakai tabel ini: satu produk menjadi satu baris listings
     * sendiri supaya stoknya bisa dikurangi per produk.
     *
     * Yang memakai tabel ini adalah tas kejutan, ketika mitra bersedia mencatat
     * apa yang benar-benar dimasukkan. Catatan itu yang membuat laporan
     * kilogram terselamatkan punya dasar, bukan tebakan.
     */
    public function up(): void
    {
        Schema::create('listing_items', function (Blueprint $table) {
            $table->id();
            $table->foreignId('listing_id')->constrained()->cascadeOnDelete();
            $table->foreignId('product_id')->nullable()->constrained('products')->nullOnDelete();
            // Dipakai kalau isinya tidak terdaftar sebagai produk
            $table->string('label', 140);
            $table->unsignedSmallInteger('qty')->default(1);
            $table->unsignedInteger('weight_gram')->nullable();
            $table->unsignedInteger('unit_value_rupiah')->nullable();
            $table->timestamps();

            $table->index('listing_id', 'idx_listing_items_listing');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('listing_items');
    }
};
