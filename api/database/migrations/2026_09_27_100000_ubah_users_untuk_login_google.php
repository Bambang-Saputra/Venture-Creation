<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * ADR-0006: login Google sebagai jalur masuk kedua di samping OTP.
 *
 * Akun Google tidak membawa nomor HP, jadi phone menjadi nullable.
 * Indeks unik phone tetap ada; MySQL membolehkan banyak NULL di indeks unik.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->string('phone', 20)->nullable()->change();
            // Klaim `sub` dari ID token Google. Dipakai sebagai identitas,
            // bukan email, karena email akun Google bisa berubah.
            $table->string('google_sub', 255)->nullable()->unique()->after('phone');
        });
    }

    /**
     * Sengaja gagal kalau sudah ada user tanpa nomor HP (akun dari Google).
     * Mengembalikan phone menjadi wajib tanpa menghapus akun itu tidak mungkin,
     * dan menghapus akun diam-diam lewat rollback lebih buruk daripada gagal.
     */
    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropUnique(['google_sub']);
            $table->dropColumn('google_sub');
            $table->string('phone', 20)->nullable(false)->change();
        });
    }
};
