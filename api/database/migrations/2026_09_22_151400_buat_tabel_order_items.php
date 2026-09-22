<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('order_items', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->constrained()->cascadeOnDelete();
            $table->foreignId('listing_id')->constrained()->restrictOnDelete();
            // Salinan judul dan harga saat memesan. Mitra boleh mengubah listing besok,
            // struk pembeli tidak boleh ikut berubah.
            $table->string('title_snapshot', 140);
            $table->unsignedInteger('unit_price_rupiah');
            $table->unsignedSmallInteger('qty');
            $table->unsignedInteger('line_total_rupiah');
            $table->timestamps();

            $table->index('order_id', 'idx_order_items_pesanan');
            $table->index('listing_id', 'idx_order_items_listing');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('order_items');
    }
};
