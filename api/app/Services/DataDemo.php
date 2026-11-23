<?php

namespace App\Services;

use Illuminate\Support\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;

/**
 * Menyegarkan data demo supaya aplikasi terasa aktif saat diuji: jualan hari ini dan besok,
 * iklan, pesanan hari ini (mengisi "Populer hari ini"), riwayat empat minggu, dan ulasan (rating toko).
 *
 * Hanya menyentuh akun dan toko demo dari SeederDemo, yaitu nomor berawalan 62811000.
 * Tidak menghapus apa pun, jadi akun penguji dan mitra sungguhan aman. Setiap langkah
 * hanya berjalan kalau datanya belum ada, sehingga aman dijalankan berulang kali.
 */
class DataDemo
{
    public const AWALAN_HP = '62811000';

    private const HURUF_KODE = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

    /** Banner Beranda dan prioritas pencarian, sama dengan contoh di SeederDemo. */
    private const IKLAN = [
        ['Nasi Goreng Pak Syahdan', Promosi::BANNER, 'Nasi goreng hangat, hemat sampai 60%'],
        ['Roti Sari Bakery', Promosi::BANNER, 'Tas roti sore mulai jam 18.00'],
        ['Kopi Lembur', Promosi::BANNER, 'Croissant dan kopi sore, mulai Rp7.500'],
        ['Rotiku Palmerah', Promosi::PRIORITAS, null],
        ['Gado-Gado Bu Anggrek', Promosi::PRIORITAS, null],
    ];

    /** Jumlah pesanan per jualan, bertingkat supaya "Populer hari ini" punya urutan. */
    private const POLA_PESANAN = [4, 3, 3, 2, 2, 2, 1, 1, 1, 1];

    private const ULASAN = [
        [5, 'Isinya banyak, rotinya masih empuk.'],
        [5, 'Worth it banget buat harga segini.'],
        [5, 'Sesuai deskripsi, label alerginya jelas.'],
        [4, 'Enak, cuma antre sebentar waktu ambil.'],
        [5, 'Kasirnya ramah, ambilnya cepat.'],
        [4, 'Porsinya pas buat makan malam.'],
        [5, 'Bakal pesan lagi besok.'],
        [3, 'Rasanya oke, tapi isinya kurang variasi.'],
        [5, null],
        [4, null],
        [5, 'Langganan tiap pulang kantor, hemat banget.'],
        [4, 'Kopinya masih enak walau sudah sore.'],
        [5, null],
        [5, 'Isi tasnya kejutan terus, seru.'],
        [4, 'Sesuai foto. Lain kali datang lebih awal biar pilihannya banyak.'],
        [5, 'Kodenya langsung dicek kasir, cepat.'],
        [3, 'Agak dingin pas diambil, tapi masih layak.'],
        [5, null],
        [4, 'Harga normalnya jelas, jadi tahu hematnya berapa.'],
        [5, 'Pas buat sarapan besok paginya.'],
        [4, null],
        [5, 'Senang bisa ikut mengurangi makanan terbuang.'],
    ];

    /** Panjang riwayat yang diisi: empat minggu, cukup untuk laporan mingguan dan saran produksi. */
    public const HARI_RIWAYAT = 28;

    /** Pembeli demo tambahan supaya riwayat pesanan toko memuat banyak nama, bukan enam orang saja. */
    private const PEMBELI_TAMBAHAN = [
        'Nadia Putri', 'Arif Setiawan', 'Maya Anggraini', 'Yoga Pratama', 'Citra Dewi', 'Dimas Saputra',
        'Laras Wulandari', 'Hendra Gunawan', 'Putri Ayuningtyas', 'Reza Firmansyah', 'Tania Halim', 'Galih Prakoso',
        'Indah Permata', 'Kevin Wijaya', 'Salsa Nabila', 'Bima Aditya', 'Rara Kusuma', 'Ilham Hidayat',
        'Vina Oktaviani', 'Agus Salim', 'Melati Sari', 'Andre Kurniawan', 'Nisa Rahma', 'Toni Hartono',
    ];

    /** Produksi harian bawaan per satuan, untuk produk demo yang belum punya angka ini. */
    private const PRODUKSI_PER_SATUAN = ['pcs' => 30, 'porsi' => 25, 'pack' => 20, 'botol' => 24, 'kg' => 10];

    private const CATATAN_SISA = [
        'Hujan sore, pembeli sepi.', 'Ada acara kantor dekat toko, ramai sampai malam.',
        'Produksi dilebihkan karena pesanan katering batal.', 'Sisa roti dibagikan ke karyawan.',
    ];

    /** @return array<string,int> jumlah yang dibuat per bagian */
    public function segarkan(): array
    {
        return [
            'jualan' => $this->jualan(),
            'iklan' => $this->iklan(),
            ...$this->pesanan(),
            ...$this->riwayat(),
            'ulasan' => $this->ulasan(),
            'voucher' => $this->voucher(),
        ];
    }

    /**
     * Voucher demo (ADR-0008): satu voucher pop up Beranda dan hadiah Gosok & Menang. Hanya dibuat
     * kalau kodenya belum ada; voucher pop up diperpanjang supaya tetap bisa diklaim saat demo.
     */
    public function voucher(): int
    {
        $daftar = [
            ['WARHEMAT', 'Diskon 20% war hari ini', 'percent', 20, 10000, 15000, 'popup', 0],
            ['GOSOK5RB', 'Potongan Rp5.000', 'rupiah', 5000, null, 15000, 'game', 30],
            ['GOSOK3RB', 'Potongan Rp3.000', 'rupiah', 3000, null, 10000, 'game', 40],
            ['GOSOK15', 'Diskon 15%', 'percent', 15, 8000, 12000, 'game', 20],
            ['GOSOK50', 'Jackpot diskon 50%', 'percent', 50, 20000, 20000, 'game', 3],
        ];
        $dibuat = 0;
        foreach ($daftar as [$kode, $judul, $jenis, $nilai, $maks, $min, $sumber, $bobot]) {
            if (DB::table('vouchers')->where('code', $kode)->exists()) {
                continue;
            }
            DB::table('vouchers')->insert([
                'code' => $kode, 'title' => $judul, 'type' => $jenis, 'value' => $nilai,
                'max_discount_rupiah' => $maks, 'min_spend_rupiah' => $min, 'valid_days' => 3,
                'source' => $sumber, 'weight' => $bobot, 'is_active' => true,
                'created_at' => now(), 'updated_at' => now(),
            ]);
            $dibuat++;
        }

        return $dibuat;
    }

    /**
     * Jualan hari ini dan besok untuk setiap toko demo yang belum punya jualan di tanggal itu,
     * disalin dari tanggal terakhir toko itu berjualan: foto, harga, kandungan, dan alergen
     * ikut, stok kembali penuh. Jam ambil hari ini yang sudah lewat diperpanjang sampai 23.30.
     */
    public function jualan(): int
    {
        $dibuat = 0;
        foreach ($this->tokoDemo() as $toko) {
            // Sumber dipilih sekali, dari sebelum hari ini, supaya jualan besok tidak ikut
            // menyalin jam ambil hari ini yang mungkin baru diperpanjang. Toko yang baru
            // diisi hari ini (tanpa riwayat) memakai jualan hari ini sebagai sumber.
            $jualanToko = DB::table('listings')->where('store_id', $toko->id)->where('status', '!=', 'draft');
            $terakhir = (clone $jualanToko)->whereDate('pickup_date', '<', today())->max('pickup_date')
                ?? (clone $jualanToko)->whereDate('pickup_date', today())->max('pickup_date');
            if ($terakhir === null) {
                continue;
            }
            foreach ([0, 1] as $geser) {
                $tanggal = Carbon::today()->addDays($geser);
                if (DB::table('listings')->where('store_id', $toko->id)->whereDate('pickup_date', $tanggal)->exists()) {
                    continue;
                }
                $dibuat += DB::transaction(fn () => $this->salinJualan($toko->id, Carbon::parse($terakhir), $tanggal));
            }
        }

        return $dibuat;
    }

    /** Iklan 7 hari mulai hari ini, kalau belum ada iklan toko demo yang sedang tayang. */
    public function iklan(): int
    {
        $toko = $this->tokoDemo()->keyBy('name');
        $sedangTayang = DB::table('store_promotions')->whereIn('store_id', $toko->pluck('id'))
            ->whereDate('starts_on', '<=', today())->whereDate('ends_on', '>=', today())->exists();
        if ($sedangTayang) {
            return 0;
        }

        $dibuat = 0;
        foreach (self::IKLAN as [$nama, $paket, $kalimat]) {
            $t = $toko->get($nama);
            if ($t === null) {
                continue;
            }
            DB::table('store_promotions')->insert([
                'store_id' => $t->id, 'package' => $paket, 'headline' => $kalimat,
                'starts_on' => today()->toDateString(), 'ends_on' => today()->addDays(6)->toDateString(),
                'days' => 7, 'price_per_day_rupiah' => Promosi::TARIF[$paket], 'total_rupiah' => Promosi::TARIF[$paket] * 7,
                'payment_status' => 'simulated', 'created_by_user_id' => $t->owner_user_id,
                'created_at' => now(), 'updated_at' => now(),
            ]);
            $dibuat++;
        }

        return $dibuat;
    }

    /**
     * Pesanan hari ini dari pembeli demo, kalau mereka belum memesan apa pun hari ini.
     * Setiap jualan selalu menyisakan stok, karena jualan yang habis tidak tampil lagi,
     * termasuk di "Populer hari ini". Pesanan baru dianggap sudah diambil kalau jam
     * ambilnya sudah mulai.
     *
     * @return array{pesanan_selesai:int, pesanan_menunggu:int}
     */
    public function pesanan(): array
    {
        $hasil = ['pesanan_selesai' => 0, 'pesanan_menunggu' => 0];
        $pembeli = $this->pembeliDemo();
        if ($pembeli === [] || DB::table('orders')->whereIn('user_id', $pembeli)->whereDate('placed_at', today())->exists()) {
            return $hasil;
        }

        $jualan = DB::table('listings')->whereIn('store_id', $this->tokoDemo()->pluck('id'))
            ->whereDate('pickup_date', today())->where('status', 'active')
            ->where('pickup_end', '>', now())->inRandomOrder()->get();

        $n = 0;
        foreach ($jualan->values() as $i => $l) {
            $berapa = min(self::POLA_PESANAN[$i] ?? 0, $l->qty_total - $l->qty_sold - $l->qty_reserved - 1);
            for ($j = 0; $j < $berapa; $j++) {
                $sudahMulai = Carbon::parse($l->pickup_start)->isPast();
                // Pesanan terakhir tiap jualan dibiarkan menunggu, supaya M11 mitra punya isi.
                $selesai = $sudahMulai && $j < $berapa - 1;
                DB::transaction(fn () => $this->buatPesanan($l, $pembeli[$n++ % count($pembeli)], $selesai));
                $hasil[$selesai ? 'pesanan_selesai' : 'pesanan_menunggu']++;
            }
        }

        return $hasil;
    }

    /** Ulasan untuk pesanan pembeli demo yang sudah diambil tapi belum diulas. */
    public function ulasan(): int
    {
        $pesanan = DB::table('orders')->where('status', 'completed')->whereIn('user_id', $this->pembeliDemo())
            ->whereNotIn('id', DB::table('reviews')->select('order_id'))
            ->orderBy('id')->get(['id', 'store_id', 'user_id', 'completed_at']);

        foreach ($pesanan->values() as $i => $o) {
            [$bintang, $komentar] = self::ULASAN[$i % count(self::ULASAN)];
            $waktu = Carbon::parse($o->completed_at ?? now())->addMinutes(random_int(20, 120));
            DB::table('reviews')->insert([
                'order_id' => $o->id, 'store_id' => $o->store_id, 'user_id' => $o->user_id,
                'rating' => $bintang, 'comment' => $komentar,
                'created_at' => $waktu->isFuture() ? now() : $waktu,
                'updated_at' => $waktu->isFuture() ? now() : $waktu,
            ]);
        }

        return $pesanan->count();
    }

    /**
     * Riwayat empat minggu terakhir untuk setiap toko demo, supaya laporan mingguan, saran produksi,
     * saldo, dan riwayat pesanan terlihat seperti toko yang sudah lama memakai aplikasi:
     * - jualan yang sudah lewat beserta pesanan selesai, sesekali tidak diambil atau dibatalkan;
     * - catatan sisa harian yang makin kecil dari minggu ke minggu (cerita "sisa makin sedikit").
     *
     * Hanya tanggal yang belum punya jualan atau catatan sisa yang diisi, jadi data lama tidak
     * berubah dan perintah ini aman diulang. Hari ini tidak ikut: sisanya belum diketahui.
     *
     * @return array{riwayat_jualan:int, riwayat_pesanan:int, catatan_sisa:int, produk_diberi_produksi:int, pembeli_tambahan:int}
     */
    public function riwayat(): array
    {
        $hasil = ['riwayat_jualan' => 0, 'riwayat_pesanan' => 0, 'catatan_sisa' => 0,
            'produk_diberi_produksi' => $this->produksiHarian(), 'pembeli_tambahan' => $this->pembeliTambahan()];
        $pembeli = $this->pembeliDemo();
        if ($pembeli === []) {
            return $hasil;
        }
        // Enam akun utama (yang dipakai saat demo) mendapat bagian lebih besar supaya riwayatnya ramai.
        $utama = array_slice($pembeli, 0, 6);

        foreach ($this->tokoDemo() as $toko) {
            $contoh = $this->jualanContoh($toko->id);
            $produk = DB::table('products')->where('store_id', $toko->id)->where('is_active', true)->orderBy('id')->get();

            for ($mundur = self::HARI_RIWAYAT; $mundur >= 1; $mundur--) {
                $tanggal = Carbon::today()->subDays($mundur);

                if ($contoh->isNotEmpty() && ! DB::table('listings')->where('store_id', $toko->id)->whereDate('pickup_date', $tanggal)->exists()) {
                    [$jualan, $pesanan] = DB::transaction(fn () => $this->hariLalu($toko, $contoh, $tanggal, $mundur, $pembeli, $utama));
                    $hasil['riwayat_jualan'] += $jualan;
                    $hasil['riwayat_pesanan'] += $pesanan;
                }

                if ($produk->isNotEmpty() && ! DB::table('waste_logs')->where('store_id', $toko->id)->whereDate('log_date', $tanggal)->exists()) {
                    DB::transaction(fn () => $this->catatSisaLalu($toko, $produk, $tanggal, $mundur));
                    $hasil['catatan_sisa']++;
                }
            }
        }

        return $hasil;
    }

    /**
     * Saran produksi (M08) hanya menghitung produk yang punya angka produksi harian. Produk demo
     * dari SeederDemo belum punya, jadi diberi angka bawaan per satuan. Angka yang sudah diisi tidak diubah.
     */
    public function produksiHarian(): int
    {
        $diubah = 0;
        $produk = DB::table('products')->whereIn('store_id', $this->tokoDemo()->pluck('id'))->whereNull('daily_production_qty')->get(['id', 'unit']);
        foreach ($produk as $p) {
            $diubah += DB::table('products')->where('id', $p->id)->whereNull('daily_production_qty')
                ->update(['daily_production_qty' => self::PRODUKSI_PER_SATUAN[$p->unit] ?? 20, 'updated_at' => now()]);
        }

        return $diubah;
    }

    /** Pembeli demo bernomor 62811000111 dan seterusnya, kalau belum ada. */
    public function pembeliTambahan(): int
    {
        $dibuat = 0;
        foreach (self::PEMBELI_TAMBAHAN as $i => $nama) {
            $hp = self::AWALAN_HP.(111 + $i);
            if (DB::table('users')->where('phone', $hp)->exists()) {
                continue;
            }
            $daftar = Carbon::today()->subDays(self::HARI_RIWAYAT + 5 + $i)->setTime(19, 0);
            DB::table('users')->insert([
                'phone' => $hp, 'name' => $nama, 'role' => 'consumer', 'phone_verified_at' => $daftar,
                'created_at' => $daftar, 'updated_at' => $daftar,
            ]);
            $dibuat++;
        }

        return $dibuat;
    }

    /** Jualan terakhir toko sebelum hari ini, dipakai sebagai contoh untuk hari-hari yang lalu. */
    private function jualanContoh(int $toko): Collection
    {
        $jualan = DB::table('listings')->where('store_id', $toko)->where('status', '!=', 'draft');
        $tanggal = (clone $jualan)->whereDate('pickup_date', '<', today())->max('pickup_date')
            ?? (clone $jualan)->max('pickup_date');

        return $tanggal === null ? collect() : (clone $jualan)->whereDate('pickup_date', $tanggal)->orderBy('id')->get();
    }

    /**
     * Satu hari yang lalu: jualan disalin dari contoh lalu ditutup, ditambah pesanan yang kebanyakan
     * selesai. Pesanan makin banyak mendekati hari ini dan di akhir pekan.
     *
     * @param  list<int>  $pembeli
     * @param  list<int>  $utama
     * @return array{0:int, 1:int}
     */
    private function hariLalu(object $toko, Collection $contoh, Carbon $tanggal, int $mundur, array $pembeli, array $utama): array
    {
        $jualan = [];
        foreach ($contoh as $l) {
            $mulai = Carbon::parse($l->pickup_start)->setDateFrom($tanggal);
            $akhir = Carbon::parse($l->pickup_end)->setDateFrom($tanggal);
            // Terbit beberapa jam sebelum jam ambil, supaya semua pesanan dibuat setelah jualan tayang.
            $terbit = $mulai->copy()->subMinutes(random_int(360, 480));
            $id = $this->salinBaris($l, $tanggal, [
                'pickup_start' => $mulai, 'pickup_end' => $akhir, 'status' => 'expired', 'qty_sold' => 0, 'qty_reserved' => 0,
                'published_at' => $terbit, 'created_at' => $terbit, 'updated_at' => $akhir,
            ]);
            $jualan[] = DB::table('listings')->where('id', $id)->first();
        }

        $akhirPekan = $tanggal->isWeekend() ? 1 : 0;
        $jumlah = random_int(1, 3) + intdiv(self::HARI_RIWAYAT - $mundur, 10) + $akhirPekan;
        $dibuat = 0;
        for ($i = 0; $i < $jumlah + 1; $i++) {
            $l = $jualan[array_rand($jualan)];
            $terjual = (int) DB::table('listings')->where('id', $l->id)->value('qty_sold');
            if ($terjual >= $l->qty_total) {
                continue;
            }
            // Satu pesanan tambahan per hari: kadang tidak diambil, kadang dibatalkan, biasanya tidak ada.
            $status = $i < $jumlah ? 'completed' : (random_int(1, 100) <= 15 ? 'no_show' : (random_int(1, 100) <= 6 ? 'cancelled' : null));
            if ($status === null) {
                continue;
            }
            $siapa = random_int(1, 100) <= 30 ? $utama[array_rand($utama)] : $pembeli[array_rand($pembeli)];
            $this->buatPesananLalu($l, $siapa, $status);
            $dibuat++;
        }

        return [count($jualan), $dibuat];
    }

    private function buatPesananLalu(object $l, int $pembeli, string $status): void
    {
        $mulai = Carbon::parse($l->pickup_start);
        $akhir = Carbon::parse($l->pickup_end);
        $terbit = Carbon::parse($l->published_at);
        $dipesan = $terbit->copy()->addMinutes(random_int(10, max(11, (int) $terbit->diffInMinutes($mulai) - 20)));
        $diambil = $status === 'completed' ? $mulai->copy()->addMinutes(random_int(5, max(6, min(90, (int) $mulai->diffInMinutes($akhir) - 5)))) : null;
        $batal = $status === 'cancelled' ? $dipesan->copy()->addMinutes(random_int(10, 60)) : null;
        $selesai = $diambil ?? $batal ?? $akhir->copy()->addMinutes(30);

        $alergi = DB::table('user_allergens')->join('allergens', 'allergens.id', '=', 'user_allergens.allergen_id')
            ->where('user_allergens.user_id', $pembeli)
            ->get(['allergens.code', 'allergens.name', 'user_allergens.severity'])
            ->map(fn ($a) => ['code' => $a->code, 'name' => $a->name, 'severity' => $a->severity])->all();
        $kode = $this->kodeUnik('orders', 'code', 'LOF-');

        $orderId = DB::table('orders')->insertGetId([
            'code' => $kode, 'user_id' => $pembeli, 'store_id' => $l->store_id, 'status' => $status,
            'subtotal_rupiah' => $l->price_rupiah, 'service_fee_rupiah' => 0, 'discount_rupiah' => 0, 'total_rupiah' => $l->price_rupiah,
            'payment_method' => random_int(0, 2) === 0 ? 'qris_static' : 'cash',
            'payment_status' => $status === 'completed' ? 'paid' : 'unpaid',
            'note' => $alergi === [] ? null : 'Mohon dipisahkan ya, saya ada pantangan.',
            'allergen_snapshot' => $alergi === [] ? null : json_encode($alergi, JSON_UNESCAPED_UNICODE),
            'pickup_start' => $l->pickup_start, 'pickup_end' => $l->pickup_end,
            'placed_at' => $dipesan, 'completed_at' => $diambil, 'cancelled_at' => $batal,
            'cancel_reason' => $batal === null ? null : 'Tidak jadi, ada urusan mendadak.',
            'created_at' => $dipesan, 'updated_at' => $selesai,
        ]);
        DB::table('order_items')->insert([
            'order_id' => $orderId, 'listing_id' => $l->id, 'title_snapshot' => $l->title,
            'unit_price_rupiah' => $l->price_rupiah, 'qty' => 1, 'line_total_rupiah' => $l->price_rupiah,
            'created_at' => $dipesan, 'updated_at' => $dipesan,
        ]);
        DB::table('pickup_codes')->insert([
            'order_id' => $orderId, 'store_id' => $l->store_id, 'code' => $this->kodeUnik('pickup_codes', 'code', ''),
            'status' => match ($status) {
                'completed' => 'used', 'cancelled' => 'cancelled', default => 'expired'
            },
            'issued_at' => $dipesan, 'expires_at' => $akhir->copy()->addMinutes(30), 'used_at' => $diambil,
            'used_by_user_id' => $diambil === null ? null : DB::table('stores')->where('id', $l->store_id)->value('owner_user_id'),
            'created_at' => $dipesan, 'updated_at' => $selesai,
        ]);

        if ($status !== 'completed') {
            return;
        }
        DB::table('listings')->where('id', $l->id)->increment('qty_sold');
        DB::table('store_balances')->where('store_id', $l->store_id)->increment('available_rupiah', $l->price_rupiah);
        DB::table('store_balances')->where('store_id', $l->store_id)->increment('lifetime_rupiah', $l->price_rupiah);
        DB::table('balance_transactions')->insert([
            'store_id' => $l->store_id, 'order_id' => $orderId, 'type' => 'sale', 'amount_rupiah' => $l->price_rupiah,
            'balance_after_rupiah' => (int) DB::table('store_balances')->where('store_id', $l->store_id)->value('available_rupiah'),
            'description' => 'Pesanan '.$kode.' diambil', 'created_at' => $diambil, 'updated_at' => $diambil,
        ]);
    }

    /**
     * Catatan sisa satu hari yang lalu. Sisa per produk sekitar 25% dari produksi empat minggu lalu
     * dan turun ke sekitar 10% kemarin, jadi laporan mingguan menunjukkan sisa yang makin sedikit.
     */
    private function catatSisaLalu(object $toko, Collection $produk, Carbon $tanggal, int $mundur): void
    {
        $dicatat = $tanggal->copy()->setTime(21, random_int(0, 50));
        $porsi = 0.10 + 0.15 * $mundur / self::HARI_RIWAYAT + ($tanggal->isWeekend() ? -0.03 : 0.0);
        $logId = DB::table('waste_logs')->insertGetId([
            'store_id' => $toko->id, 'log_date' => $tanggal->toDateString(), 'method' => 'per_item',
            'recorded_by_user_id' => $toko->owner_user_id,
            'note' => random_int(1, 100) <= 15 ? self::CATATAN_SISA[array_rand(self::CATATAN_SISA)] : null,
            'locked_at' => $tanggal->copy()->addDays(2)->startOfDay(),
            'created_at' => $dicatat, 'updated_at' => $dicatat,
        ]);

        $nilai = $berat = $jumlah = 0;
        foreach ($produk as $i => $p) {
            $produksi = $p->daily_production_qty ?? self::PRODUKSI_PER_SATUAN[$p->unit] ?? 20;
            $qty = max(0, (int) round($produksi * $porsi) + random_int(-1, 1));
            if ($qty === 0) {
                continue;
            }
            $satuan = (int) ($p->cost_rupiah ?? $p->price_rupiah);
            $gram = $p->weight_gram === null ? null : $qty * (int) $p->weight_gram;
            // Sesekali satu produk dimakan karyawan atau disumbangkan, bukan dibuang.
            $nasib = $i === 0 && random_int(1, 100) <= 20 ? (random_int(0, 1) === 0 ? 'staff_meal' : 'donated') : 'discarded';
            DB::table('waste_log_items')->insert([
                'waste_log_id' => $logId, 'product_id' => $p->id, 'label' => $p->name, 'qty' => $qty,
                'weight_gram' => $gram, 'unit_value_rupiah' => $satuan, 'value_rupiah' => $qty * $satuan,
                'disposition' => $nasib, 'created_at' => $dicatat, 'updated_at' => $dicatat,
            ]);
            $nilai += $qty * $satuan;
            $berat += $gram ?? 0;
            $jumlah += $qty;
        }

        DB::table('waste_logs')->where('id', $logId)->update([
            'total_value_rupiah' => $nilai, 'total_weight_gram' => $berat, 'total_items' => $jumlah,
        ]);
    }

    /** Salin satu jualan ke tanggal lain, beserta alergen dan isi tasnya. */
    private function salinBaris(object $l, Carbon $ke, array $ubah): int
    {
        $baru = (array) $l;
        unset($baru['id']);
        $id = DB::table('listings')->insertGetId([...$baru, 'pickup_date' => $ke->toDateString(), ...$ubah]);

        foreach (DB::table('listing_allergens')->where('listing_id', $l->id)->get() as $a) {
            DB::table('listing_allergens')->insert([
                'listing_id' => $id, 'allergen_id' => $a->allergen_id, 'presence' => $a->presence,
                'created_at' => $ubah['created_at'], 'updated_at' => $ubah['created_at'],
            ]);
        }
        foreach (DB::table('listing_items')->where('listing_id', $l->id)->get() as $item) {
            $salinan = (array) $item;
            unset($salinan['id']);
            DB::table('listing_items')->insert([...$salinan, 'listing_id' => $id, 'created_at' => $ubah['created_at'], 'updated_at' => $ubah['created_at']]);
        }

        return $id;
    }

    private function salinJualan(int $toko, Carbon $dari, Carbon $ke): int
    {
        $sumber = DB::table('listings')->where('store_id', $toko)->whereDate('pickup_date', $dari)
            ->where('status', '!=', 'draft')->orderBy('id')->get();
        $dibuat = 0;

        foreach ($sumber as $l) {
            $mulai = Carbon::parse($l->pickup_start)->setDateFrom($ke);
            $akhir = Carbon::parse($l->pickup_end)->setDateFrom($ke);
            if ($akhir->isPast()) {
                $akhir = $ke->copy()->setTime(23, 30);
            }
            if ($akhir->isPast() || $akhir <= $mulai) {
                continue;
            }

            $this->salinBaris($l, $ke, [
                'pickup_start' => $mulai, 'pickup_end' => $akhir, 'status' => 'active', 'qty_sold' => 0, 'qty_reserved' => 0,
                'published_at' => now(), 'created_at' => now(), 'updated_at' => now(),
            ]);
            $dibuat++;
        }

        return $dibuat;
    }

    private function buatPesanan(object $l, int $pembeli, bool $selesai): void
    {
        $mulai = Carbon::parse($l->pickup_start);
        $batas = $mulai->isPast() ? $mulai : now();
        $dipesan = today()->setTime(8, 0);
        $dipesan = $batas->greaterThan($dipesan)
            ? $dipesan->addMinutes(random_int(0, (int) $dipesan->diffInMinutes($batas)))
            : now()->subMinutes(5);
        $diambil = $selesai ? $mulai->copy()->addMinutes(random_int(5, 50)) : null;
        if ($diambil?->isFuture()) {
            $diambil = now()->subMinutes(random_int(1, 15));
        }

        $alergi = DB::table('user_allergens')->join('allergens', 'allergens.id', '=', 'user_allergens.allergen_id')
            ->where('user_allergens.user_id', $pembeli)
            ->get(['allergens.code', 'allergens.name', 'user_allergens.severity'])
            ->map(fn ($a) => ['code' => $a->code, 'name' => $a->name, 'severity' => $a->severity])->all();
        $kode = $this->kodeUnik('orders', 'code', 'LOF-');
        $pemilik = DB::table('stores')->where('id', $l->store_id)->value('owner_user_id');

        $orderId = DB::table('orders')->insertGetId([
            'code' => $kode, 'user_id' => $pembeli, 'store_id' => $l->store_id,
            'status' => $selesai ? 'completed' : 'pending_pickup',
            'subtotal_rupiah' => $l->price_rupiah, 'service_fee_rupiah' => 0, 'discount_rupiah' => 0, 'total_rupiah' => $l->price_rupiah,
            'payment_method' => random_int(0, 2) === 0 ? 'qris_static' : 'cash',
            'payment_status' => $selesai ? 'paid' : 'unpaid',
            'note' => $alergi === [] ? null : 'Mohon dipisahkan ya, saya ada pantangan.',
            'allergen_snapshot' => $alergi === [] ? null : json_encode($alergi, JSON_UNESCAPED_UNICODE),
            'pickup_start' => $l->pickup_start, 'pickup_end' => $l->pickup_end,
            'placed_at' => $dipesan, 'completed_at' => $diambil,
            'created_at' => $dipesan, 'updated_at' => $diambil ?? $dipesan,
        ]);
        DB::table('order_items')->insert([
            'order_id' => $orderId, 'listing_id' => $l->id, 'title_snapshot' => $l->title,
            'unit_price_rupiah' => $l->price_rupiah, 'qty' => 1, 'line_total_rupiah' => $l->price_rupiah,
            'created_at' => $dipesan, 'updated_at' => $dipesan,
        ]);
        DB::table('pickup_codes')->insert([
            'order_id' => $orderId, 'store_id' => $l->store_id, 'code' => $this->kodeUnik('pickup_codes', 'code', ''),
            'status' => $selesai ? 'used' : 'active', 'issued_at' => $dipesan,
            'expires_at' => Carbon::parse($l->pickup_end)->addMinutes(30), 'used_at' => $diambil,
            'used_by_user_id' => $selesai ? $pemilik : null,
            'created_at' => $dipesan, 'updated_at' => $diambil ?? $dipesan,
        ]);

        DB::table('listings')->where('id', $l->id)->increment($selesai ? 'qty_sold' : 'qty_reserved');
        if (! $selesai) {
            DB::table('store_balances')->where('store_id', $l->store_id)->increment('pending_rupiah', $l->price_rupiah);

            return;
        }
        DB::table('store_balances')->where('store_id', $l->store_id)->increment('available_rupiah', $l->price_rupiah);
        DB::table('store_balances')->where('store_id', $l->store_id)->increment('lifetime_rupiah', $l->price_rupiah);
        DB::table('balance_transactions')->insert([
            'store_id' => $l->store_id, 'order_id' => $orderId, 'type' => 'sale', 'amount_rupiah' => $l->price_rupiah,
            'balance_after_rupiah' => (int) DB::table('store_balances')->where('store_id', $l->store_id)->value('available_rupiah'),
            'description' => 'Pesanan '.$kode.' diambil', 'created_at' => $diambil, 'updated_at' => $diambil,
        ]);
    }

    /** Toko yang pemiliknya akun demo, bukan mitra sungguhan. */
    private function tokoDemo(): Collection
    {
        return DB::table('stores')->join('users', 'users.id', '=', 'stores.owner_user_id')
            ->where('users.phone', 'like', self::AWALAN_HP.'%')->whereNull('stores.pilot_consent_at')
            ->orderBy('stores.id')->get(['stores.id', 'stores.name', 'stores.owner_user_id']);
    }

    /** @return list<int> */
    private function pembeliDemo(): array
    {
        return DB::table('users')->where('role', 'consumer')->where('phone', 'like', self::AWALAN_HP.'%')
            ->orderBy('id')->pluck('id')->all();
    }

    private function kodeUnik(string $tabel, string $kolom, string $awalan): string
    {
        do {
            $kode = $awalan;
            for ($i = 0; $i < 6; $i++) {
                $kode .= self::HURUF_KODE[random_int(0, strlen(self::HURUF_KODE) - 1)];
            }
        } while (DB::table($tabel)->where($kolom, $kode)->exists());

        return $kode;
    }
}
