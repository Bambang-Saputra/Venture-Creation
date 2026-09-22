<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Tabel sendiri, bukan format notifications bawaan Laravel yang memakai UUID
     * dan kolom notifiable polimorfik. Selama uji coba pengiriman dilakukan dengan
     * polling dari aplikasi, FCM ditunda.
     */
    public function up(): void
    {
        Schema::create('notifications', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            // Contoh: pesanan_baru, pengingat_ambil, listing_terbit, laporan_mingguan
            $table->string('type', 60);
            $table->string('title', 140);
            $table->text('body');
            // Tujuan ketukan, misal: {"screen":"K14","order_id":12}
            $table->json('data')->nullable();
            $table->timestamp('read_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'read_at'], 'idx_notif_pengguna_dibaca');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('notifications');
    }
};
