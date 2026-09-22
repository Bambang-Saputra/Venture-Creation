<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Buku besar saldo mitra, hanya boleh ditambah, tidak pernah diubah atau dihapus.
     * Kalau ada kekeliruan, tulis baris adjustment dengan nilai berlawanan.
     *
     * amount_rupiah di sini bertanda: pemasukan positif, koreksi dan pencairan negatif.
     * Ini satu-satunya kolom uang di proyek yang tidak UNSIGNED, dan itu disengaja.
     */
    public function up(): void
    {
        Schema::create('balance_transactions', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->foreignId('order_id')->nullable()->constrained()->nullOnDelete();
            $table->enum('type', ['sale', 'adjustment', 'withdrawal', 'service_fee'])->default('sale');
            $table->integer('amount_rupiah');
            $table->unsignedInteger('balance_after_rupiah')->default(0);
            $table->string('description', 255)->nullable();
            $table->foreignId('created_by_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->timestamps();

            $table->index(['store_id', 'created_at'], 'idx_balance_trx_toko_waktu');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('balance_transactions');
    }
};
