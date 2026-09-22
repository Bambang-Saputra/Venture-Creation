<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Hasil agregasi terjadwal, bukan perhitungan saat layar dibuka.
     * Alasannya laporan ini yang dikirim tiap Senin dan harus sama persis
     * dengan yang dibaca mitra minggu lalu.
     */
    public function up(): void
    {
        Schema::create('weekly_reports', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->date('week_start');
            $table->date('week_end');
            // Yang terbuang
            $table->unsignedInteger('wasted_value_rupiah')->default(0);
            $table->unsignedInteger('wasted_weight_gram')->default(0);
            // Yang terselamatkan lewat penjualan surplus
            $table->unsignedInteger('rescued_value_rupiah')->default(0);
            $table->unsignedInteger('rescued_weight_gram')->default(0);
            $table->unsignedSmallInteger('orders_count')->default(0);
            $table->unsignedTinyInteger('logged_days')->default(0);
            // Lima produk dengan sisa terbanyak, sudah terurut
            $table->json('top_wasted_products')->nullable();
            $table->timestamp('generated_at');
            $table->timestamps();

            $table->unique(['store_id', 'week_start'], 'uq_weekly_reports_toko_minggu');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('weekly_reports');
    }
};
