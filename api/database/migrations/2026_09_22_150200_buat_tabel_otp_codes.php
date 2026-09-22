<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('otp_codes', function (Blueprint $table) {
            $table->id();
            $table->string('phone', 20);
            // Hanya hash yang disimpan. Kode mentah tidak pernah masuk database maupun log.
            $table->string('code_hash', 255);
            $table->enum('purpose', ['login'])->default('login');
            $table->unsignedTinyInteger('attempts')->default(0);
            $table->timestamp('expires_at');
            $table->timestamp('consumed_at')->nullable();
            $table->string('request_ip', 45)->nullable();
            $table->timestamps();

            $table->index(['phone', 'expires_at'], 'idx_otp_nomor_kedaluwarsa');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('otp_codes');
    }
};
