<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Wajib ditulis untuk: penukaran kode pickup, publikasi listing, edit catatan
     * sisa, perubahan saldo, dan perubahan keanggotaan toko.
     *
     * Baris di sini tidak pernah diubah atau dihapus. Kalau mitra bertanya
     * siapa yang menukar kode jam berapa, jawabannya harus ada di sini.
     * Nomor HP disamarkan sebelum masuk kolom meta.
     */
    public function up(): void
    {
        Schema::create('audit_logs', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->nullable()->constrained()->nullOnDelete();
            $table->foreignId('store_id')->nullable()->constrained()->nullOnDelete();
            // Contoh: pickup_code.redeem, listing.publish, waste_log.update
            $table->string('action', 64);
            $table->string('subject_type', 64)->nullable();
            $table->unsignedBigInteger('subject_id')->nullable();
            $table->json('meta')->nullable();
            $table->string('ip', 45)->nullable();
            $table->timestamps();

            $table->index(['store_id', 'created_at'], 'idx_audit_toko_waktu');
            $table->index(['action', 'created_at'], 'idx_audit_aksi_waktu');
            $table->index(['subject_type', 'subject_id'], 'idx_audit_subjek');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('audit_logs');
    }
};
