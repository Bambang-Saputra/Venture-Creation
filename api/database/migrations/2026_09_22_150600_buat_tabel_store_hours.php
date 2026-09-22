<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('store_hours', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            // 0 Minggu sampai 6 Sabtu, mengikuti Carbon::dayOfWeek
            $table->unsignedTinyInteger('day_of_week');
            $table->time('open_time')->nullable();
            $table->time('close_time')->nullable();
            $table->boolean('is_closed')->default(false);
            $table->timestamps();

            $table->unique(['store_id', 'day_of_week'], 'uq_store_hours_hari');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('store_hours');
    }
};
