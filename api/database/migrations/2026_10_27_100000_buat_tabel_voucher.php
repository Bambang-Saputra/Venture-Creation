<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Voucher (ADR-0008). Potongan ditanggung Life of Foods, bukan mitra: pembeli membayar total
 * setelah potongan di toko, dan selisihnya dicatat sebagai penyesuaian saldo mitra saat kode
 * pickup ditukar.
 *
 * - vouchers: jenis voucher. source "popup" diklaim dari pop up Beranda, "game" hadiah Gosok &
 *   Menang (weight = peluang relatif), "admin" dibagikan tim.
 * - user_vouchers: voucher milik pembeli. Satu baris dipakai sekali (used_at, order_id).
 * - game_plays: satu kali gosok per pembeli per hari; voucher_id null berarti belum beruntung.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vouchers', function (Blueprint $table) {
            $table->id();
            $table->string('code', 40)->unique();
            $table->string('title', 120);
            $table->enum('type', ['percent', 'rupiah']);
            $table->unsignedInteger('value');
            $table->unsignedInteger('max_discount_rupiah')->nullable();
            $table->unsignedInteger('min_spend_rupiah')->default(0);
            $table->unsignedSmallInteger('valid_days')->default(3);
            $table->dateTime('claimable_until')->nullable();
            $table->unsignedInteger('quota')->nullable();
            $table->unsignedInteger('claimed_count')->default(0);
            $table->enum('source', ['popup', 'game', 'admin'])->default('admin');
            $table->unsignedSmallInteger('weight')->default(0);
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->index(['source', 'is_active']);
        });

        Schema::create('user_vouchers', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('voucher_id')->constrained()->cascadeOnDelete();
            $table->dateTime('claimed_at');
            $table->dateTime('expires_at');
            $table->dateTime('used_at')->nullable();
            $table->foreignId('order_id')->nullable()->constrained()->nullOnDelete();
            $table->timestamps();

            $table->index(['user_id', 'used_at', 'expires_at']);
        });

        Schema::create('game_plays', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->date('play_date');
            $table->foreignId('voucher_id')->nullable()->constrained()->nullOnDelete();
            $table->foreignId('user_voucher_id')->nullable()->constrained('user_vouchers')->nullOnDelete();
            $table->timestamps();

            $table->unique(['user_id', 'play_date']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('game_plays');
        Schema::dropIfExists('user_vouchers');
        Schema::dropIfExists('vouchers');
    }
};
