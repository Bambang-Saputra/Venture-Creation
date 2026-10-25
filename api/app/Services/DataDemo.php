<?php

namespace App\Services;

use Illuminate\Support\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;

/**
 * Menyegarkan data demo supaya aplikasi terasa aktif saat diuji: jualan hari ini dan besok,
 * iklan, pesanan hari ini (mengisi "Populer hari ini"), dan ulasan (rating toko).
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
    ];

    /** @return array<string,int> jumlah yang dibuat per bagian */
    public function segarkan(): array
    {
        return [
            'jualan' => $this->jualan(),
            'iklan' => $this->iklan(),
            ...$this->pesanan(),
            'ulasan' => $this->ulasan(),
        ];
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

            $baru = (array) $l;
            unset($baru['id']);
            $id = DB::table('listings')->insertGetId([
                ...$baru,
                'pickup_date' => $ke->toDateString(), 'pickup_start' => $mulai, 'pickup_end' => $akhir,
                'status' => 'active', 'qty_sold' => 0, 'qty_reserved' => 0,
                'published_at' => now(), 'created_at' => now(), 'updated_at' => now(),
            ]);

            foreach (DB::table('listing_allergens')->where('listing_id', $l->id)->get() as $a) {
                DB::table('listing_allergens')->insert([
                    'listing_id' => $id, 'allergen_id' => $a->allergen_id, 'presence' => $a->presence,
                    'created_at' => now(), 'updated_at' => now(),
                ]);
            }
            foreach (DB::table('listing_items')->where('listing_id', $l->id)->get() as $item) {
                $salinan = (array) $item;
                unset($salinan['id']);
                DB::table('listing_items')->insert([...$salinan, 'listing_id' => $id, 'created_at' => now(), 'updated_at' => now()]);
            }
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
