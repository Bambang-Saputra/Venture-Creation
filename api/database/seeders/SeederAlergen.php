<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

/**
 * Data induk alergen dan pantangan. Aman dijalankan berulang kali.
 *
 * Daftar ini ikut ter-commit karena bukan data mitra, dan karena kode
 * di kolom code dipakai langsung oleh aplikasi Android saat memfilter.
 * Mengubah code berarti mengubah aplikasi, jadi tambah baris baru,
 * jangan mengganti code yang sudah dipakai.
 */
class SeederAlergen extends Seeder
{
    public function run(): void
    {
        $sekarang = now();

        $baris = [
            ['kacang_tanah', 'Kacang tanah', 'allergen', 10],
            ['kacang_pohon', 'Kacang pohon (almond, mete)', 'allergen', 20],
            ['susu', 'Susu dan produk susu', 'allergen', 30],
            ['telur', 'Telur', 'allergen', 40],
            ['gluten', 'Gandum dan gluten', 'allergen', 50],
            ['kedelai', 'Kedelai', 'allergen', 60],
            ['ikan', 'Ikan', 'allergen', 70],
            ['udang_kerang', 'Udang, kepiting, dan kerang', 'allergen', 80],
            ['wijen', 'Wijen', 'allergen', 90],
            ['vegetarian', 'Vegetarian', 'diet', 110],
            ['vegan', 'Vegan', 'diet', 120],
            ['tanpa_babi', 'Tanpa babi', 'diet', 130],
            ['tanpa_alkohol', 'Tanpa alkohol', 'diet', 140],
        ];

        $data = [];
        foreach ($baris as [$code, $name, $type, $urutan]) {
            $data[] = [
                'code' => $code,
                'name' => $name,
                'type' => $type,
                'sort_order' => $urutan,
                'is_active' => true,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ];
        }

        DB::table('allergens')->upsert($data, ['code'], ['name', 'type', 'sort_order', 'is_active', 'updated_at']);
    }
}
