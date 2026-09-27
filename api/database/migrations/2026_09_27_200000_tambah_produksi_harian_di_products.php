<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * M08 Saran produksi butuh "biasa dibuat berapa" per produk. Skema awal
 * tidak menyimpan jumlah produksi di mana pun, jadi mitra mengisinya
 * sekali per produk. Kosong berarti produk itu tidak diberi saran.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('products', function (Blueprint $table) {
            $table->unsignedSmallInteger('daily_production_qty')->nullable()->after('weight_gram');
        });
    }

    public function down(): void
    {
        Schema::table('products', function (Blueprint $table) {
            $table->dropColumn('daily_production_qty');
        });
    }
};
