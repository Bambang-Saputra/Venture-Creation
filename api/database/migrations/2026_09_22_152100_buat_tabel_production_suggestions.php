<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Saran berbasis aturan sederhana, bukan model prediksi.
     * Contoh aturan: produk yang tersisa lebih dari 20 persen selama tiga hari
     * berturut disarankan dikurangi sebanyak rata-rata sisanya.
     *
     * rationale_text wajib diisi karena mitra berhak tahu dasar sarannya.
     * Saran tanpa alasan akan diabaikan, dan pantas diabaikan.
     */
    public function up(): void
    {
        Schema::create('production_suggestions', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->foreignId('product_id')->constrained()->cascadeOnDelete();
            $table->date('suggested_for_date');
            $table->unsignedSmallInteger('current_avg_production')->default(0);
            $table->unsignedSmallInteger('suggested_production')->default(0);
            $table->decimal('avg_waste_qty', 6, 2)->default(0);
            $table->unsignedInteger('estimated_saving_rupiah')->default(0);
            $table->text('rationale_text');
            // Berapa hari data yang dipakai. Di bawah tiga hari, saran tidak ditampilkan.
            $table->unsignedTinyInteger('sample_days')->default(0);
            $table->enum('status', ['new', 'accepted', 'dismissed'])->default('new');
            $table->timestamp('generated_at');
            $table->timestamp('responded_at')->nullable();
            $table->timestamps();

            $table->unique(['store_id', 'product_id', 'suggested_for_date'], 'uq_saran_produk_tanggal');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('production_suggestions');
    }
};
