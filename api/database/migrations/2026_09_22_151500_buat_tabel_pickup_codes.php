<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Enam digit yang ditunjukkan pembeli di toko.
     *
     * Kode unik global, bukan unik per toko. Enam digit memberi sejuta kombinasi,
     * jauh lebih dari cukup untuk uji coba, dan unik global membuat kasir tidak
     * pernah bisa menukar kode milik toko lain karena kecocokan kebetulan.
     */
    public function up(): void
    {
        Schema::create('pickup_codes', function (Blueprint $table) {
            $table->id();
            $table->foreignId('order_id')->unique()->constrained()->cascadeOnDelete();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->char('code', 6)->unique();
            $table->enum('status', ['active', 'used', 'expired', 'cancelled'])->default('active');
            $table->timestamp('issued_at');
            $table->timestamp('expires_at');
            $table->timestamp('used_at')->nullable();
            // Identitas kasir yang menukar, dipakai saat rekonsiliasi harian
            $table->foreignId('used_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->timestamps();

            $table->index(['store_id', 'status'], 'idx_pickup_codes_toko_status');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('pickup_codes');
    }
};
