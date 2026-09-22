<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('store_members', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            // cashier hanya boleh mencatat sisa dan mencocokkan kode pickup
            $table->enum('role', ['owner', 'cashier'])->default('cashier');
            $table->timestamp('invited_at')->nullable();
            $table->timestamp('revoked_at')->nullable();
            $table->timestamps();

            $table->unique(['store_id', 'user_id'], 'uq_store_members_anggota');
            $table->index(['user_id', 'revoked_at'], 'idx_store_members_aktif');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('store_members');
    }
};
