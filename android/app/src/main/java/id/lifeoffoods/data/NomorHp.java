package id.lifeoffoods.data;

import androidx.annotation.Nullable;

/**
 * Nomor HP seluler Indonesia, dengan aturan yang sama seperti App\Support\NomorHp di API: boleh
 * 08xx, +628xx, 628xx, atau 8xx, lalu dinormalisasi jadi 628xxxxxxxxx (10 sampai 13 digit lokal).
 *
 * <p>Pemeriksaan di aplikasi hanya untuk memberi tahu lebih cepat. Server tetap memeriksa ulang.
 */
public final class NomorHp {

    private NomorHp() {}

    /**
     * @return 628xxxxxxxxx, atau null kalau bukan nomor seluler Indonesia
     */
    @Nullable
    public static String normalisasi(@Nullable String masukan) {
        if (masukan == null) {
            return null;
        }
        String angka = masukan.replaceAll("[\\s\\-().]", "");
        if (angka.startsWith("+")) {
            angka = angka.substring(1);
        }
        if (angka.startsWith("0")) {
            angka = "62" + angka.substring(1);
        } else if (angka.startsWith("8")) {
            angka = "62" + angka;
        }
        return angka.matches("^628[1-9][0-9]{7,10}$") ? angka : null;
    }

    /** 6281234567890 menjadi "+62 812-3456-7890" untuk teks K03. */
    public static String tampilan(String ternormalisasi) {
        String lokal =
                ternormalisasi.startsWith("62") ? ternormalisasi.substring(2) : ternormalisasi;
        if (lokal.length() < 7) {
            return "+62 " + lokal;
        }
        return "+62 "
                + lokal.substring(0, 3)
                + "-"
                + lokal.substring(3, 7)
                + "-"
                + lokal.substring(7);
    }
}
