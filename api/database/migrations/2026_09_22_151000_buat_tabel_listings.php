<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Tabel inti. Satu baris adalah satu barang yang dijual hari ini,
     * baik tas kejutan maupun menu satuan.
     *
     * Judul, harga, kandungan, dan label halal sengaja disalin ke sini,
     * bukan dibaca lewat relasi ke products atau surprise_bag_templates.
     * Alasannya: mitra boleh mengubah harga besok, dan pesanan kemarin
     * harus tetap menunjukkan angka yang dilihat pembeli saat memesan.
     */
    public function up(): void
    {
        Schema::create('listings', function (Blueprint $table) {
            $table->id();
            $table->foreignId('store_id')->constrained()->cascadeOnDelete();
            $table->enum('type', ['surprise_bag', 'menu_item']);
            $table->foreignId('template_id')->nullable()->constrained('surprise_bag_templates')->nullOnDelete();
            $table->foreignId('product_id')->nullable()->constrained('products')->nullOnDelete();

            $table->string('title', 140);
            $table->text('description')->nullable();
            $table->text('content_hint')->nullable();
            $table->string('photo_path', 255)->nullable();

            $table->unsignedInteger('price_rupiah');
            $table->unsignedInteger('original_value_rupiah')->nullable();

            // Sisa stok = qty_total - qty_reserved - qty_sold, selalu dihitung, tidak disimpan.
            // Pengurangan hanya boleh di dalam transaksi dengan SELECT ... FOR UPDATE.
            $table->unsignedSmallInteger('qty_total');
            $table->unsignedSmallInteger('qty_reserved')->default(0);
            $table->unsignedSmallInteger('qty_sold')->default(0);

            $table->date('pickup_date');
            $table->dateTime('pickup_start');
            $table->dateTime('pickup_end');

            $table->enum('status', ['draft', 'active', 'paused', 'sold_out', 'expired'])->default('draft');

            // Wajib terisi sebelum terbit. Ditegakkan oleh chk_listings_kandungan di bawah.
            $table->text('ingredients_text')->nullable();
            $table->enum('halal_label', ['certified', 'self_claim', 'not_stated'])->default('not_stated');
            $table->string('halal_certificate_no', 80)->nullable();

            $table->timestamp('published_at')->nullable();
            $table->timestamps();

            $table->index(['status', 'pickup_date'], 'idx_listings_status_tanggal');
            $table->index(['store_id', 'status'], 'idx_listings_toko_status');
            $table->index('pickup_end', 'idx_listings_batas_ambil');
        });

        // Stok tidak boleh terjual melebihi yang dipasang, walau ada bug di kode aplikasi.
        DB::statement('ALTER TABLE listings ADD CONSTRAINT chk_listings_kuota CHECK (qty_reserved + qty_sold <= qty_total)');

        // Janji keamanan alergi tidak boleh bergantung pada validasi aplikasi saja.
        DB::statement("ALTER TABLE listings ADD CONSTRAINT chk_listings_kandungan CHECK (status = 'draft' OR (ingredients_text IS NOT NULL AND CHAR_LENGTH(TRIM(ingredients_text)) >= 3))");

        DB::statement('ALTER TABLE listings ADD CONSTRAINT chk_listings_jam_ambil CHECK (pickup_end > pickup_start)');
    }

    public function down(): void
    {
        Schema::dropIfExists('listings');
    }
};
