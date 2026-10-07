<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Pendaftaran mitra dari app (M03, #7 rencana lanjutan). Sebelumnya akun mitra hanya dibuat
     * lewat SeederPilot.
     *
     * Alurnya: nomor baru masuk lewat M01/M02, mengisi data toko di M03, lalu menunggu di M04
     * sampai tim menyetujui lewat `php artisan mitra:setujui {id}`. Toko baru dibuat saat disetujui,
     * jadi akun yang belum disetujui tidak bisa membuka satu pun rute /partner/stores.
     *
     * KTP sengaja tidak diminta. NIB opsional karena banyak usaha kecil (termasuk yang dijalankan
     * orang tua) belum punya; yang mengisinya diperiksa lebih dulu.
     */
    public function up(): void
    {
        Schema::create('partner_applications', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('owner_name', 120);
            $table->string('store_name', 140);
            $table->enum('category', ['cafe', 'bakery', 'resto', 'catering', 'grocery']);
            $table->string('address', 255);
            $table->decimal('latitude', 10, 7)->nullable();
            $table->decimal('longitude', 10, 7)->nullable();
            $table->time('open_time');
            $table->time('close_time');
            // Nomor Induk Berusaha (OSS), 13 digit. Opsional.
            $table->string('nib', 13)->nullable();
            $table->string('halal_certificate_no', 80)->nullable();
            $table->enum('status', ['pending', 'approved', 'rejected'])->default('pending');
            $table->string('rejection_reason', 255)->nullable();
            $table->foreignId('store_id')->nullable()->constrained()->nullOnDelete();
            $table->timestamp('reviewed_at')->nullable();
            $table->timestamps();

            $table->index(['status', 'created_at'], 'idx_pendaftaran_status');
        });

        Schema::table('stores', function (Blueprint $table) {
            $table->string('nib', 13)->nullable()->after('halal_certificate_no');
        });
    }

    public function down(): void
    {
        Schema::table('stores', function (Blueprint $table) {
            $table->dropColumn('nib');
        });
        Schema::dropIfExists('partner_applications');
    }
};
