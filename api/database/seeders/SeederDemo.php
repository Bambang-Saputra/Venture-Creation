<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/**
 * Data contoh untuk pengembangan, demo ke dosen, dan screenshot.
 *
 * Semua nama toko di sini karangan. Data mitra sungguhan tidak pernah
 * masuk berkas ini, tempatnya di SeederPilot yang membaca CSV lokal.
 *
 * Isi: 8 toko (3 di sekitar BINUS Kemanggisan untuk uji responden), jualan tiga hari
 * berturut dengan foto contoh, 20 pesanan dengan berbagai status, dan catatan sisa harian
 * supaya laporan mingguan punya angka. Jualan hari ini dan besok berlaku sampai 23.00, jadi
 * jalankan ulang seeder kalau uji coba lewat dari besok.
 *
 * Nomor HP memakai awalan 62811000 yang tidak dipakai operator mana pun,
 * supaya OTP demo tidak pernah terkirim ke nomor orang sungguhan.
 */
class SeederDemo extends Seeder
{
    private Carbon $hariIni;

    public function run(): void
    {
        $this->hariIni = Carbon::today();
        $this->salinFoto();

        $konsumen = $this->buatKonsumen();
        $toko = $this->buatToko();
        $listing = $this->buatListing($toko);
        $this->buatPesanan($konsumen, $listing);
        $this->buatCatatanSisa($toko);

        $this->command?->info('SeederDemo selesai: '.count($toko).' toko, '.count($listing).' listing.');
    }

    /**
     * Foto contoh ikut repo di database/seeders/foto-demo (sumber dan lisensi di SUMBER.md).
     * Disalin ke disk public supaya dilayani lewat /storage; butuh `php artisan storage:link`.
     */
    private function salinFoto(): void
    {
        $asal = __DIR__.'/foto-demo';
        foreach (glob($asal.'/*.jpg') ?: [] as $berkas) {
            Storage::disk('public')->put(self::FOLDER_FOTO.'/'.basename($berkas), (string) file_get_contents($berkas));
        }
    }

    private const FOLDER_FOTO = 'demo';

    /**
     * Jam ambil demo 18.00-23.00 dan toko tutup 23.30, supaya responden yang mencoba malam
     * hari masih menemukan jualan. Jualan hanya tampil selama jam ambilnya belum lewat.
     */
    private const JAM_TUTUP = '23:30:00';

    private const AMBIL_MULAI = 18;

    private const AMBIL_SELESAI = 23;

    private function foto(?string $nama): ?string
    {
        return $nama === null ? null : self::FOLDER_FOTO.'/'.$nama;
    }

    /**
     * @return list<int> id pengguna konsumen
     */
    private function buatKonsumen(): array
    {
        $orang = [
            ['62811000101', 'Dinda Pratiwi', 'Tebet, Jakarta Selatan', -6.2297, 106.8583, ['kacang_tanah' => 'severe']],
            ['62811000102', 'Rizky Maulana', 'Kemang, Jakarta Selatan', -6.2607, 106.8133, ['susu' => 'avoid']],
            ['62811000103', 'Ayu Lestari', 'Manggarai, Jakarta Selatan', -6.2105, 106.8501, ['udang_kerang' => 'severe', 'ikan' => 'avoid']],
            ['62811000104', 'Bagas Nugroho', 'Pasar Minggu, Jakarta Selatan', -6.2843, 106.8446, []],
            ['62811000105', 'Sinta Rahmawati', 'Setiabudi, Jakarta Selatan', -6.2183, 106.8283, ['gluten' => 'avoid', 'vegetarian' => 'avoid']],
            ['62811000106', 'Fajar Ramadhan', 'Tebet, Jakarta Selatan', -6.2260, 106.8560, []],
        ];

        $alergen = DB::table('allergens')->pluck('id', 'code');
        $sekarang = now();
        $id = [];

        foreach ($orang as [$hp, $nama, $area, $lat, $lng, $pantangan]) {
            $uid = DB::table('users')->insertGetId([
                'phone' => $hp,
                'name' => $nama,
                'phone_verified_at' => $sekarang,
                'role' => 'consumer',
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            DB::table('consumer_profiles')->insert([
                'user_id' => $uid,
                'area_label' => $area,
                'latitude' => $lat,
                'longitude' => $lng,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            foreach ($pantangan as $kode => $tingkat) {
                DB::table('user_allergens')->insert([
                    'user_id' => $uid,
                    'allergen_id' => $alergen[$kode],
                    'severity' => $tingkat,
                    'created_at' => $sekarang,
                    'updated_at' => $sekarang,
                ]);
            }

            $id[] = $uid;
        }

        return $id;
    }

    /**
     * Delapan toko karangan (lima di Jakarta Selatan, tiga di sekitar BINUS Kemanggisan), masing-masing dengan pemilik,
     * jam buka, produk, satu cetakan tas kejutan, dan baris saldo kosong.
     *
     * @return list<array<string,mixed>>
     */
    private function buatToko(): array
    {
        // Kolom terakhir tiap produk dan tiap toko adalah nama berkas di foto-demo (null = tanpa foto).
        // Tiga toko terakhir sengaja di sekitar BINUS Kemanggisan, tempat responden uji coba
        // berada; satu di antaranya berbahan kacang tanah supaya tugas filter alergi teruji.
        $daftar = [
            [
                'Roti Sari Bakery', 'bakery', 'Jl. Tebet Raya No. 12, Jakarta Selatan', -6.2297, 106.8583,
                'self_claim', 'Tepung terigu, gula, telur, susu sapi, mentega, ragi',
                [['Roti manis cokelat', 'pcs', 8000, 4500, 80, 'roti-manis-cokelat.jpg'], ['Donat gula', 'pcs', 6000, 3000, 60, 'donat-gula.jpg'], ['Roti tawar gandum', 'pack', 18000, 9000, 400, null]],
                'roti-sari-tas.jpg',
            ],
            [
                'Kopi Lembur', 'cafe', 'Jl. Kemang Selatan No. 4, Jakarta Selatan', -6.2607, 106.8133,
                'not_stated', 'Biji kopi arabika, susu sapi, gula aren, air',
                [['Croissant mentega', 'pcs', 15000, 7000, 70, 'croissant.jpg'], ['Kopi susu gula aren', 'botol', 22000, 9000, 250, null], ['Cookies oatmeal', 'pcs', 9000, 4000, 45, null]],
                'kopi-lembur-tas.jpg',
            ],
            [
                'Warung Bu Tuti', 'resto', 'Jl. Manggarai Utara No. 8, Jakarta Selatan', -6.2105, 106.8501,
                'certified', 'Nasi putih, ayam, cabai, bawang merah, bawang putih, santan',
                [['Nasi ayam bakar', 'porsi', 25000, 13000, 350, 'nasi-ayam-bakar.jpg'], ['Nasi telur balado', 'porsi', 18000, 8000, 300, 'nasi-telur-balado.jpg'], ['Sayur lodeh', 'porsi', 10000, 4500, 200, null]],
                'nasi-ayam-bakar.jpg',
            ],
            [
                'Dapur Katering Amanah', 'catering', 'Jl. Pasar Minggu Raya No. 21, Jakarta Selatan', -6.2843, 106.8446,
                'certified', 'Nasi putih, ayam, tempe, tahu, sayur, bumbu rempah',
                [['Nasi kotak komplit', 'pack', 30000, 16000, 450, 'nasi-ayam-telur.jpg'], ['Snack box isi 4', 'pack', 20000, 10000, 250, null]],
                'nasi-ayam-telur.jpg',
            ],
            [
                'Segar Mart', 'grocery', 'Jl. Setiabudi Tengah No. 3, Jakarta Selatan', -6.2183, 106.8283,
                'not_stated', 'Aneka buah dan sayur segar, tanpa bahan tambahan',
                [['Paket buah potong', 'pack', 25000, 12000, 500, 'buah-segar.jpg'], ['Sayur bayam ikat', 'pcs', 5000, 2500, 200, 'sayur-segar.jpg'], ['Pisang cavendish', 'kg', 20000, 11000, 1000, null]],
                'buah-segar.jpg',
            ],
            [
                'Nasi Goreng Pak Syahdan', 'resto', 'Jl. Syahdan No. 15, Palmerah, Jakarta Barat', -6.2002, 106.7858,
                'self_claim', 'Nasi putih, telur, ayam, kecap manis, bawang merah, cabai',
                [['Nasi goreng telur', 'porsi', 20000, 9000, 350, 'nasi-goreng.jpg'], ['Nasi goreng pattaya', 'porsi', 24000, 11000, 380, 'nasi-goreng-pattaya.jpg']],
                'nasi-goreng.jpg',
            ],
            [
                'Gado-Gado Bu Anggrek', 'resto', 'Jl. Anggrek Cakra No. 7, Kemanggisan, Jakarta Barat', -6.2036, 106.7835,
                'certified', 'Sayur rebus, tahu, tempe, telur, lontong, saus kacang tanah, kerupuk',
                [['Gado-gado lontong', 'porsi', 22000, 10000, 400, 'gado-gado.jpg'], ['Ketoprak', 'porsi', 20000, 9000, 380, null]],
                'gado-gado.jpg',
            ],
            [
                'Rotiku Palmerah', 'bakery', 'Jl. Kemanggisan Raya No. 30, Jakarta Barat', -6.1985, 106.7802,
                'not_stated', 'Tepung terigu, gula, mentega, kayu manis, ragi',
                [['Roti sobek kayu manis', 'pcs', 16000, 7000, 150, 'roti-sobek.jpg'], ['Roti tawar susu', 'pack', 17000, 8000, 400, null]],
                'roti-sobek.jpg',
            ],
        ];

        $sekarang = now();
        $hasil = [];
        $nomor = 201;

        foreach ($daftar as [$nama, $kategori, $alamat, $lat, $lng, $halal, $kandungan, $produk, $fotoTas]) {
            $uid = DB::table('users')->insertGetId([
                'phone' => '62811000'.$nomor,
                'name' => 'Pemilik '.$nama,
                'phone_verified_at' => $sekarang,
                'role' => 'partner',
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);
            $nomor++;

            $sid = DB::table('stores')->insertGetId([
                'owner_user_id' => $uid,
                'name' => $nama,
                'slug' => Str::slug($nama),
                'category' => $kategori,
                'address' => $alamat,
                'latitude' => $lat,
                'longitude' => $lng,
                'whatsapp' => '62811000'.($nomor - 1),
                'halal_label' => $halal,
                'halal_certificate_no' => $halal === 'certified' ? 'ID'.random_int(10000000, 99999999) : null,
                'default_ingredients_text' => $kandungan,
                'photo_path' => $this->foto($fotoTas),
                'pilot_consent_at' => null,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            DB::table('store_members')->insert([
                'store_id' => $sid,
                'user_id' => $uid,
                'role' => 'owner',
                'invited_at' => $sekarang,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            DB::table('store_balances')->insert([
                'store_id' => $sid,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            for ($hari = 0; $hari <= 6; $hari++) {
                DB::table('store_hours')->insert([
                    'store_id' => $sid,
                    'day_of_week' => $hari,
                    'open_time' => '08:00:00',
                    // Tutup larut supaya uji coba malam hari masih menemukan jualan aktif.
                    'close_time' => self::JAM_TUTUP,
                    'is_closed' => false,
                    'created_at' => $sekarang,
                    'updated_at' => $sekarang,
                ]);
            }

            $idProduk = [];
            foreach ($produk as [$namaProduk, $satuan, $harga, $hpp, $berat, $foto]) {
                $idProduk[] = [
                    'id' => DB::table('products')->insertGetId([
                        'store_id' => $sid,
                        'name' => $namaProduk,
                        'unit' => $satuan,
                        'price_rupiah' => $harga,
                        'cost_rupiah' => $hpp,
                        'weight_gram' => $berat,
                        'ingredients_text' => $kandungan,
                        'created_at' => $sekarang,
                        'updated_at' => $sekarang,
                    ]),
                    'nama' => $namaProduk,
                    'harga' => $harga,
                    'hpp' => $hpp,
                    'berat' => $berat,
                    'foto' => $this->foto($foto),
                ];
            }

            $hasil[] = [
                'id' => $sid,
                'owner' => $uid,
                'nama' => $nama,
                'halal' => $halal,
                'kandungan' => $kandungan,
                'foto' => $this->foto($fotoTas),
                'produk' => $idProduk,
            ];
        }

        return $hasil;
    }

    /**
     * Tiga hari jualan per toko: kemarin sudah habis, hari ini aktif,
     * besok aktif. Tiap hari berisi satu tas kejutan dan dua menu satuan,
     * supaya kedua jenis layar punya isi saat dibuka.
     *
     * @param  list<array<string,mixed>>  $toko
     * @return list<array<string,mixed>>
     */
    private function buatListing(array $toko): array
    {
        $sekarang = now();
        $hasil = [];

        foreach ($toko as $urut => $t) {
            $nilaiAsli = (int) array_sum(array_column($t['produk'], 'harga'));
            $hargaTas = (int) (round($nilaiAsli * 0.35 / 1000) * 1000);

            $templateId = DB::table('surprise_bag_templates')->insertGetId([
                'store_id' => $t['id'],
                'name' => 'Tas Kejutan '.$t['nama'],
                'description' => 'Isi menyesuaikan sisa hari ini, minimal tiga item.',
                'content_hint' => implode(', ', array_slice(array_column($t['produk'], 'nama'), 0, 3)),
                'price_rupiah' => $hargaTas,
                'original_value_rupiah' => $nilaiAsli,
                'default_qty' => 5,
                'pickup_start_time' => sprintf('%02d:00:00', self::AMBIL_MULAI),
                'pickup_end_time' => sprintf('%02d:00:00', self::AMBIL_SELESAI),
                'ingredients_text' => $t['kandungan'],
                'halal_label' => $t['halal'],
                'photo_path' => $t['foto'],
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            foreach ([-1, 0, 1] as $geser) {
                $tanggal = $this->hariIni->copy()->addDays($geser);
                $mulai = $tanggal->copy()->setTime(self::AMBIL_MULAI, 0);
                $selesai = $tanggal->copy()->setTime(self::AMBIL_SELESAI, 0);
                $status = $geser < 0 ? 'sold_out' : 'active';
                // Jumlah terjual hari ini dibuat beragam per toko supaya urutan
                // "Populer hari ini" di K07 kelihatan, tidak seri semua.
                $terjual = $geser < 0 ? 5 : ($geser === 0 ? 1 + ($urut % 4) : 0);

                $hasil[] = $this->simpanListing([
                    'store_id' => $t['id'],
                    'type' => 'surprise_bag',
                    'template_id' => $templateId,
                    'title' => 'Tas Kejutan '.$t['nama'],
                    'content_hint' => implode(', ', array_slice(array_column($t['produk'], 'nama'), 0, 3)),
                    'price_rupiah' => $hargaTas,
                    'original_value_rupiah' => $nilaiAsli,
                    'qty_total' => 5,
                    'qty_sold' => $terjual,
                    'pickup_date' => $tanggal->toDateString(),
                    'pickup_start' => $mulai,
                    'pickup_end' => $selesai,
                    'status' => $status,
                    'ingredients_text' => $t['kandungan'],
                    'halal_label' => $t['halal'],
                    'photo_path' => $t['foto'],
                ], $geser);

                foreach (array_slice($t['produk'], 0, 2) as $ke => $p) {
                    $hasil[] = $this->simpanListing([
                        'store_id' => $t['id'],
                        'type' => 'menu_item',
                        'product_id' => $p['id'],
                        'title' => $p['nama'],
                        'description' => 'Sisa produksi hari ini, kualitas tetap baik.',
                        'price_rupiah' => (int) (round($p['harga'] * 0.5 / 500) * 500),
                        'original_value_rupiah' => $p['harga'],
                        'qty_total' => 4,
                        'qty_sold' => $geser < 0 ? 4 : ($geser === 0 ? ($urut + $ke) % 3 : 0),
                        'pickup_date' => $tanggal->toDateString(),
                        'pickup_start' => $mulai,
                        'pickup_end' => $selesai,
                        'status' => $geser < 0 ? 'sold_out' : 'active',
                        'ingredients_text' => $t['kandungan'],
                        'halal_label' => $t['halal'],
                        'photo_path' => $p['foto'],
                    ], $geser);
                }
            }
        }

        return $hasil;
    }

    /**
     * @param  array<string,mixed>  $data
     * @return array<string,mixed>
     */
    private function simpanListing(array $data, int $geser): array
    {
        $sekarang = now();
        $id = DB::table('listings')->insertGetId($data + [
            'published_at' => $sekarang,
            'created_at' => $sekarang,
            'updated_at' => $sekarang,
        ]);

        $this->tandaiAlergen($id, $data['ingredients_text']);

        return [
            'id' => $id,
            'store_id' => $data['store_id'],
            'title' => $data['title'],
            'harga' => $data['price_rupiah'],
            'sisa' => $data['qty_total'] - $data['qty_sold'],
            'geser' => $geser,
            'pickup_start' => $data['pickup_start'],
            'pickup_end' => $data['pickup_end'],
        ];
    }

    /**
     * Menandai alergen dari teks kandungan. Cara ini hanya layak untuk data contoh.
     * Di aplikasi sungguhan mitra yang mencentang sendiri, karena menebak alergen
     * dari teks bebas adalah cara yang bagus untuk mengirim orang ke rumah sakit.
     */
    private function tandaiAlergen(int $listingId, ?string $kandungan): void
    {
        if ($kandungan === null) {
            return;
        }

        $petunjuk = [
            'susu' => ['susu', 'mentega', 'keju'],
            'telur' => ['telur'],
            'gluten' => ['terigu', 'gandum', 'roti'],
            'kacang_tanah' => ['kacang tanah'],
            'udang_kerang' => ['udang', 'kepiting', 'kerang'],
            'ikan' => ['ikan'],
            'kedelai' => ['kedelai', 'tempe', 'tahu'],
        ];

        $teks = Str::lower($kandungan);
        $alergen = DB::table('allergens')->pluck('id', 'code');
        $sekarang = now();

        foreach ($petunjuk as $kode => $kata) {
            foreach ($kata as $k) {
                if (str_contains($teks, $k) && isset($alergen[$kode])) {
                    DB::table('listing_allergens')->insertOrIgnore([
                        'listing_id' => $listingId,
                        'allergen_id' => $alergen[$kode],
                        'presence' => 'contains',
                        'created_at' => $sekarang,
                        'updated_at' => $sekarang,
                    ]);
                    break;
                }
            }
        }
    }

    /**
     * 20 pesanan: 12 sudah selesai kemarin, 8 menunggu diambil hari ini.
     * Jumlahnya sengaja tidak pernah melebihi qty_sold pada listing
     * supaya angka di layar mitra cocok dengan angka di layar pembeli.
     *
     * Pesanan dibagi bergiliran antar toko, bukan diurut. Kalau diurut,
     * toko terakhir bisa tidak kebagian pesanan sama sekali dan layar
     * mitranya kosong saat gilirannya didemokan.
     *
     * @param  list<int>  $konsumen
     * @param  list<array<string,mixed>>  $listing
     */
    private function buatPesanan(array $konsumen, array $listing): void
    {
        $kemarin = array_filter($listing, fn ($l) => $l['geser'] === -1 && $l['sisa'] < 4);
        $hariIni = array_filter($listing, fn ($l) => $l['geser'] === 0 && $l['sisa'] < 5);

        $nomor = 1;

        foreach ([[$kemarin, 12, 'completed'], [$hariIni, 8, 'pending_pickup']] as [$kolam, $jumlah, $status]) {
            $perToko = [];
            foreach ($kolam as $l) {
                $perToko[$l['store_id']][] = $l;
            }

            $idToko = array_keys($perToko);

            for ($i = 0; $i < $jumlah; $i++) {
                $pilihan = $perToko[$idToko[$i % count($idToko)]];
                $l = $pilihan[intdiv($i, count($idToko)) % count($pilihan)];

                $this->simpanSatuPesanan(
                    kode: 'LOF-'.str_pad((string) $nomor++, 4, '0', STR_PAD_LEFT),
                    userId: $konsumen[$i % count($konsumen)],
                    listing: $l,
                    status: $status,
                );
            }
        }
    }

    /**
     * @param  array<string,mixed>  $listing
     */
    private function simpanSatuPesanan(string $kode, int $userId, array $listing, string $status): void
    {
        $sekarang = now();
        $harga = (int) $listing['harga'];
        $selesai = $status === 'completed';

        $alergi = DB::table('user_allergens')
            ->join('allergens', 'allergens.id', '=', 'user_allergens.allergen_id')
            ->where('user_allergens.user_id', $userId)
            ->get(['allergens.code', 'allergens.name', 'user_allergens.severity'])
            ->map(fn ($a) => ['code' => $a->code, 'name' => $a->name, 'severity' => $a->severity])
            ->all();

        $orderId = DB::table('orders')->insertGetId([
            'code' => $kode,
            'user_id' => $userId,
            'store_id' => $listing['store_id'],
            'status' => $status,
            'subtotal_rupiah' => $harga,
            'service_fee_rupiah' => 0,
            'discount_rupiah' => 0,
            'total_rupiah' => $harga,
            'payment_method' => 'cash',
            'payment_status' => $selesai ? 'paid' : 'unpaid',
            'note' => $alergi === [] ? null : 'Mohon dipisahkan, saya ada pantangan.',
            'allergen_snapshot' => $alergi === [] ? null : json_encode($alergi, JSON_UNESCAPED_UNICODE),
            'pickup_start' => $listing['pickup_start'],
            'pickup_end' => $listing['pickup_end'],
            'placed_at' => $listing['pickup_start']->copy()->subHours(6),
            'completed_at' => $selesai ? $listing['pickup_start']->copy()->addMinutes(35) : null,
            'created_at' => $sekarang,
            'updated_at' => $sekarang,
        ]);

        DB::table('order_items')->insert([
            'order_id' => $orderId,
            'listing_id' => $listing['id'],
            'title_snapshot' => $listing['title'],
            'unit_price_rupiah' => $harga,
            'qty' => 1,
            'line_total_rupiah' => $harga,
            'created_at' => $sekarang,
            'updated_at' => $sekarang,
        ]);

        DB::table('pickup_codes')->insert([
            'order_id' => $orderId,
            'store_id' => $listing['store_id'],
            'code' => str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT),
            'status' => $selesai ? 'used' : 'active',
            'issued_at' => $listing['pickup_start']->copy()->subHours(6),
            'expires_at' => $listing['pickup_end'],
            'used_at' => $selesai ? $listing['pickup_start']->copy()->addMinutes(35) : null,
            'created_at' => $sekarang,
            'updated_at' => $sekarang,
        ]);

        // lifetime hanya menghitung yang benar-benar sudah diambil. Kalau pesanan
        // yang masih menunggu ikut dihitung, angka total pendapatan di dasbor
        // mitra jadi lebih besar dari uang yang pernah mereka terima.
        $kolom = $selesai ? 'available_rupiah' : 'pending_rupiah';
        $saldo = DB::table('store_balances')->where('store_id', $listing['store_id']);
        $saldo->increment($kolom, $harga);

        if ($selesai) {
            $saldo->increment('lifetime_rupiah', $harga);

            $sesudah = (int) DB::table('store_balances')
                ->where('store_id', $listing['store_id'])
                ->value('available_rupiah');

            DB::table('balance_transactions')->insert([
                'store_id' => $listing['store_id'],
                'order_id' => $orderId,
                'type' => 'sale',
                'amount_rupiah' => $harga,
                'balance_after_rupiah' => $sesudah,
                'description' => 'Pesanan '.$kode.' diambil',
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);
        }
    }

    /**
     * Catatan sisa tujuh hari ke belakang, supaya layar laporan mingguan
     * dan saran produksi punya angka sejak hari pertama dibuka.
     *
     * @param  list<array<string,mixed>>  $toko
     */
    private function buatCatatanSisa(array $toko): void
    {
        $sekarang = now();

        foreach ($toko as $t) {
            foreach (range(1, 7) as $mundur) {
                $tanggal = $this->hariIni->copy()->subDays($mundur);

                $logId = DB::table('waste_logs')->insertGetId([
                    'store_id' => $t['id'],
                    'log_date' => $tanggal->toDateString(),
                    'method' => $mundur % 3 === 0 ? 'weight' : 'per_item',
                    'recorded_by_user_id' => $t['owner'],
                    'note' => $mundur === 1 ? 'Hujan sore, pembeli sepi.' : null,
                    'created_at' => $sekarang,
                    'updated_at' => $sekarang,
                ]);

                $totalNilai = 0;
                $totalBerat = 0;
                $totalItem = 0;

                foreach ($t['produk'] as $index => $p) {
                    $qty = max(0, (int) round(sin($mundur + $index) * 3 + 4));
                    if ($qty === 0) {
                        continue;
                    }

                    $nilai = $qty * (int) $p['hpp'];
                    $berat = $qty * (int) $p['berat'];

                    DB::table('waste_log_items')->insert([
                        'waste_log_id' => $logId,
                        'product_id' => $p['id'],
                        'label' => $p['nama'],
                        'qty' => $qty,
                        'weight_gram' => $berat,
                        'unit_value_rupiah' => $p['hpp'],
                        'value_rupiah' => $nilai,
                        'disposition' => $index === 0 && $mundur % 4 === 0 ? 'staff_meal' : 'discarded',
                        'created_at' => $sekarang,
                        'updated_at' => $sekarang,
                    ]);

                    $totalNilai += $nilai;
                    $totalBerat += $berat;
                    $totalItem += $qty;
                }

                DB::table('waste_logs')->where('id', $logId)->update([
                    'total_value_rupiah' => $totalNilai,
                    'total_weight_gram' => $totalBerat,
                    'total_items' => $totalItem,
                ]);
            }
        }
    }
}
