<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('user_allergens', function (Blueprint $table) {
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('allergen_id')->constrained()->cascadeOnDelete();
            // severe membuat peringatan tampil lebih tegas dan filter aktif otomatis
            $table->enum('severity', ['avoid', 'severe'])->default('avoid');
            $table->timestamps();

            $table->primary(['user_id', 'allergen_id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('user_allergens');
    }
};
