<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Cetakan tas kejutan supaya mitra memasang jualan harian dalam hitungan detik:
     * pilih cetakan, ubah jumlah, terbit. Nilai di sini hanya nilai awal,
     * yang mengikat tetap salinan di tabel listings.
     */
    public function up(): void
    {
        Schema::create('surprise_bag_templates', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->string('name', 140);
            $table->text('description')->nullable();
            // Perkiraan isi yang ditulis mitra, misal: 2 roti manis + 1 donat
            $table->text('content_hint')->nullable();
            $table->unsignedInteger('price_rupiah');
            $table->unsignedInteger('original_value_rupiah')->nullable();
            $table->unsignedSmallInteger('default_qty')->default(1);
            $table->time('pickup_start_time')->nullable();
            $table->time('pickup_end_time')->nullable();
            $table->text('ingredients_text')->nullable();
            $table->enum('halal_label', ['certified', 'self_claim', 'not_stated'])->default('not_stated');
            $table->string('photo_path', 255)->nullable();
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->index(['store_id', 'is_active'], 'idx_templates_toko_aktif');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('surprise_bag_templates');
    }
};
