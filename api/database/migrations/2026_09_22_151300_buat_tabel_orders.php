<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Satu pesanan selalu milik satu toko. Kalau pembeli mengambil dari dua toko,
     * terbit dua pesanan dengan dua kode pickup, karena pengambilannya juga
     * terjadi di dua tempat.
     *
     * Selama uji coba pembayaran dilakukan tunai atau QRIS milik mitra di tempat.
     * Aplikasi hanya mencatat, tidak memegang uang siapa pun.
     */
    public function up(): void
    {
        Schema::create('orders', function (Blueprint $table) {
            $table->id();
            // Nomor pesanan yang ditunjukkan ke mitra, contoh: LOF-8K3M2P
            $table->string('code', 20)->unique();
            $table->foreignId('user_id')->constrained('users')->restrictOnDelete();
            $table->foreignId('store_id')->constrained()->restrictOnDelete();

            $table->enum('status', ['pending_pickup', 'completed', 'cancelled', 'no_show'])->default('pending_pickup');

            $table->unsignedInteger('subtotal_rupiah');
            // Selalu 0 selama uji coba. Kolomnya tetap ada supaya perhitungan BEP
            // di Business Report memakai skema yang sama dengan aplikasinya.
            $table->unsignedInteger('service_fee_rupiah')->default(0);
            $table->unsignedInteger('discount_rupiah')->default(0);
            $table->unsignedInteger('total_rupiah');

            $table->enum('payment_method', ['cash', 'qris_static'])->default('cash');
            $table->enum('payment_status', ['unpaid', 'paid'])->default('unpaid');

            // Catatan bebas dari pembeli untuk mitra
            $table->text('note')->nullable();
            // Salinan alergi pembeli saat memesan. Disalin, bukan direlasikan,
            // supaya mitra melihat kondisi saat pesanan dibuat walau profil berubah.
            $table->json('allergen_snapshot')->nullable();

            $table->dateTime('pickup_start');
            $table->dateTime('pickup_end');

            $table->timestamp('placed_at');
            $table->timestamp('completed_at')->nullable();
            $table->timestamp('cancelled_at')->nullable();
            $table->string('cancel_reason', 255)->nullable();
            $table->timestamps();

            $table->index(['store_id', 'status', 'pickup_start'], 'idx_orders_toko_status_jam');
            $table->index(['user_id', 'status'], 'idx_orders_pembeli_status');
        });

        DB::statement('ALTER TABLE orders ADD CONSTRAINT chk_orders_total CHECK (total_rupiah = subtotal_rupiah + service_fee_rupiah - discount_rupiah)');
    }

    public function down(): void
    {
        Schema::dropIfExists('orders');
    }
};
