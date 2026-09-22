<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * value_rupiah disimpan, bukan dihitung ulang dari products.cost_rupiah.
     *
     * Kalau mitra menaikkan HPP bulan depan, laporan bulan lalu harus tetap
     * menampilkan angka yang mereka lihat saat mencatat. Laporan yang berubah
     * sendiri adalah laporan yang tidak dipercaya, dan mitra berhenti mengisi.
     */
    public function up(): void
    {
        Schema::create('waste_log_items', function (Blueprint $table) {
            $table->id();
            $table->foreignId('waste_log_id')->constrained()->cascadeOnDelete();
            $table->foreignId('product_id')->nullable()->constrained('products')->nullOnDelete();
            // Dipakai kalau yang tersisa tidak terdaftar sebagai produk
            $table->string('label', 140);
            $table->unsignedSmallInteger('qty')->nullable();
            $table->unsignedInteger('weight_gram')->nullable();
            $table->unsignedInteger('unit_value_rupiah')->default(0);
            $table->unsignedInteger('value_rupiah')->default(0);
            // Ke mana sisanya pergi. Yang dihitung sebagai pemborosan hanya discarded.
            $table->enum('disposition', ['discarded', 'sold_surplus', 'staff_meal', 'donated'])->default('discarded');
            $table->timestamps();

            $table->index('waste_log_id', 'idx_waste_items_catatan');
            $table->index(['product_id', 'created_at'], 'idx_waste_items_produk_waktu');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('waste_log_items');
    }
};
