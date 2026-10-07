<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Iklan mitra yang dibayar per hari tayang (#3 rencana lanjutan).
     *
     * Dipasang per toko, bukan per jualan, karena satu listing hanya berlaku untuk satu
     * tanggal ambil. Dua paket:
     * - search_priority: maksimal dua jualan toko ini tampil paling atas di daftar utama K07
     *   dengan label "Iklan".
     * - home_banner: kartu di karusel paling atas K07.
     *
     * Selama uji coba belum ada tagihan: payment_status = simulated dan saldo mitra tidak
     * dipotong. Harga per hari tetap disimpan supaya laporan nanti memakai tarif saat dibeli.
     */
    public function up(): void
    {
        Schema::create('store_promotions', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->enum('package', ['search_priority', 'home_banner']);
            // Kalimat promo di banner, misalnya "Tas kejutan mulai Rp8.000".
            $table->string('headline', 80)->nullable();
            $table->date('starts_on');
            $table->date('ends_on');
            $table->unsignedSmallInteger('days');
            $table->unsignedInteger('price_per_day_rupiah');
            $table->unsignedInteger('total_rupiah');
            $table->enum('payment_status', ['simulated', 'paid'])->default('simulated');
            $table->foreignId('created_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->timestamps();

            $table->index(['package', 'starts_on', 'ends_on'], 'idx_promosi_paket_tanggal');
            $table->index(['store_id', 'ends_on'], 'idx_promosi_toko');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('store_promotions');
    }
};
