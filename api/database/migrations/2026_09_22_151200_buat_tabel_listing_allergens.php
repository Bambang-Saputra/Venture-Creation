<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('listing_allergens', function (Blueprint $table) {
            $table->foreignId('listing_id')->constrained()->cascadeOnDelete();
            $table->foreignId('allergen_id')->constrained()->cascadeOnDelete();
            // may_contain dipakai untuk risiko kontaminasi silang di dapur yang sama
            $table->enum('presence', ['contains', 'may_contain'])->default('contains');
            $table->timestamps();

            $table->primary(['listing_id', 'allergen_id']);
            $table->index('allergen_id', 'idx_listing_allergens_alergen');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('listing_allergens');
    }
};
