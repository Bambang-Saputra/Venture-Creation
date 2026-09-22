<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Autentikasi memakai nomor HP + OTP, bukan email + kata sandi.
     * Karena itu tabel password_reset_tokens bawaan Laravel tidak dibuat:
     * tidak ada alur lupa kata sandi di aplikasi ini.
     */
    public function up(): void
    {
        Schema::create('users', function (Blueprint $table) {
            $table->id();
            // Disimpan ternormalisasi tanpa tanda plus, contoh: 6281234567890
            $table->string('phone', 20)->unique();
            $table->string('name', 120)->nullable();
            $table->string('email', 160)->nullable()->unique();
            $table->timestamp('phone_verified_at')->nullable();
            // Tidak dipakai pada alur OTP. Disiapkan untuk akun internal tim.
            $table->string('password')->nullable();
            $table->enum('role', ['consumer', 'partner', 'admin'])->default('consumer');
            $table->boolean('is_active')->default(true);
            $table->rememberToken();
            $table->timestamps();

            $table->index(['role', 'is_active'], 'idx_users_peran_aktif');
        });

        Schema::create('sessions', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->foreignId('user_id')->nullable()->index();
            $table->string('ip_address', 45)->nullable();
            $table->text('user_agent')->nullable();
            $table->longText('payload');
            $table->integer('last_activity')->index();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('sessions');
        Schema::dropIfExists('users');
    }
};
