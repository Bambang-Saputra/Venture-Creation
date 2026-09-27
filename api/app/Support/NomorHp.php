<?php

namespace App\Support;

/**
 * Nomor HP disimpan ternormalisasi 628xxxxxxxxx: tanpa tanda plus, tanpa
 * spasi (kamus data). Android boleh mengirim 08xx, +628xx, 628xx, atau 8xx.
 */
final class NomorHp
{
    /**
     * @return string|null null kalau bukan nomor seluler Indonesia
     */
    public static function normalisasi(string $masukan): ?string
    {
        $angka = preg_replace('/[\s\-().]/', '', $masukan) ?? '';

        if (str_starts_with($angka, '+')) {
            $angka = substr($angka, 1);
        }

        if (str_starts_with($angka, '0')) {
            $angka = '62'.substr($angka, 1);
        } elseif (str_starts_with($angka, '8')) {
            $angka = '62'.$angka;
        }

        // 08xx dengan total 10 sampai 13 digit.
        return preg_match('/^628[1-9][0-9]{7,10}$/', $angka) === 1 ? $angka : null;
    }
}
