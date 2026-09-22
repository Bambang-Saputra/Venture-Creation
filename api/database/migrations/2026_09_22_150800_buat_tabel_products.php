<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('products', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->string('name', 140);
            $table->enum('unit', ['pcs', 'porsi', 'pack', 'kg', 'botol'])->default('pcs');
            // Harga jual normal, dipakai sebagai acuan coret pada menu satuan
            $table->unsignedInteger('price_rupiah')->default(0);
            // Harga pokok produksi. Dipakai menghitung nilai sisa saat mencatat.
            $table->unsignedInteger('cost_rupiah')->nullable();
            $table->unsignedInteger('weight_gram')->nullable();
            $table->text('ingredients_text')->nullable();
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->index(['store_id', 'is_active'], 'idx_products_toko_aktif');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('products');
    }
};
