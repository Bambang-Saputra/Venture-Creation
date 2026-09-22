<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('stores', function (Blueprint $table) {
            $table->id();
            $table->foreignId('owner_user_id')->constrained('users')->restrictOnDelete();
            $table->string('name', 140);
            $table->string('slug', 160)->unique();
            $table->enum('category', ['cafe', 'bakery', 'resto', 'catering', 'grocery']);
            $table->string('address', 255);
            $table->decimal('latitude', 10, 7)->nullable();
            $table->decimal('longitude', 10, 7)->nullable();
            $table->string('whatsapp', 20)->nullable();
            $table->string('photo_path', 255)->nullable();
            // Hanya tiga nilai. Aplikasi tidak pernah menulis "halal" tanpa dasar.
            $table->enum('halal_label', ['certified', 'self_claim', 'not_stated'])->default('not_stated');
            $table->string('halal_certificate_no', 80)->nullable();
            // Dipakai sebagai nilai awal saat mitra membuat listing baru
            $table->text('default_ingredients_text')->nullable();
            $table->boolean('is_active')->default(true);
            $table->boolean('is_temporarily_closed')->default(false);
            // Bukti mitra setuju ikut uji coba, termasuk izin memakai nama dan foto
            $table->timestamp('pilot_consent_at')->nullable();
            $table->timestamps();

            $table->index(['is_active', 'is_temporarily_closed'], 'idx_stores_ketersediaan');
            $table->index(['latitude', 'longitude'], 'idx_stores_koordinat');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('stores');
    }
};
