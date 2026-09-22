<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Jantung Arah B: mitra mencatat sisa hari ini, aplikasi mengubahnya jadi rupiah.
     * Satu baris untuk satu toko pada satu tanggal.
     *
     * Kolom total diisi ulang setiap kali rinciannya berubah, dan dikunci lewat
     * locked_at setelah masuk rekap mingguan supaya laporan yang sudah
     * ditunjukkan ke mitra tidak berubah diam-diam.
     */
    public function up(): void
    {
        Schema::create('waste_logs', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->date('log_date');
            // per_item: hitung satuan. weight: timbang per kilogram.
            $table->enum('method', ['per_item', 'weight'])->default('per_item');
            $table->unsignedInteger('total_value_rupiah')->default(0);
            $table->unsignedInteger('total_weight_gram')->default(0);
            $table->unsignedSmallInteger('total_items')->default(0);
            $table->text('note')->nullable();
            $table->foreignId('recorded_by_user_id')->constrained('users')->restrictOnDelete();
            $table->timestamp('locked_at')->nullable();
            $table->timestamps();

            $table->unique(['store_id', 'log_date'], 'uq_waste_logs_toko_tanggal');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('waste_logs');
    }
};
