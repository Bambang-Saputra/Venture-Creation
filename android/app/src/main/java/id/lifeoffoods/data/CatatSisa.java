package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Aturan M06 Catat sisa dan M19 Catat sisa timbang (PRD-12), Java murni supaya bisa diuji unit:
 * nilai terbuang saat mengetik, berat kg ke gram, dan teks ringkasan.
 */
public final class CatatSisa {

    public static final String DIBUANG = "discarded";
    public static final String DISUMBANGKAN = "donated";
    public static final String MAKAN_KARYAWAN = "staff_meal";
    public static final String TERJUAL_SURPLUS = "sold_surplus";

    public static final int JUMLAH_MAKS = 10000;
    public static final int GRAM_MAKS = 1_000_000;

    private CatatSisa() {}

    /** Satu baris untuk hitungan: nilai satuan, jumlah, dan tujuan sisanya. */
    public static final class Baris {
        public final long nilaiSatuan;
        public final int jumlah;
        @Nullable public final String tujuan;

        public Baris(long nilaiSatuan, int jumlah, @Nullable String tujuan) {
            this.nilaiSatuan = nilaiSatuan;
            this.jumlah = jumlah;
            this.tujuan = tujuan;
        }
    }

    /** Hanya yang dibuang dihitung sebagai pemborosan (PRD-12 kriteria 2). Null = dibuang. */
    public static boolean terbuang(@Nullable String tujuan) {
        return tujuan == null || DIBUANG.equals(tujuan);
    }

    /** Nilai rupiah terbuang, dihitung saat angka berubah (kriteria 9, mode per item). */
    public static long nilaiTerbuang(Iterable<Baris> baris) {
        long total = 0;
        for (Baris b : baris) {
            if (terbuang(b.tujuan) && b.jumlah > 0) {
                total += b.nilaiSatuan * b.jumlah;
            }
        }
        return total;
    }

    /** Jumlah item terbuang (mode per item). */
    public static int itemTerbuang(Iterable<Baris> baris) {
        int total = 0;
        for (Baris b : baris) {
            if (terbuang(b.tujuan) && b.jumlah > 0) {
                total += b.jumlah;
            }
        }
        return total;
    }

    /**
     * Ketikan berat dalam kg ("1,2", "0.8", "2") menjadi gram. -1 kalau tidak terbaca atau di luar
     * batas; 0 untuk kosong.
     */
    public static int gram(@Nullable CharSequence kg) {
        if (kg == null) {
            return 0;
        }
        String s = kg.toString().trim().replace(',', '.');
        if (s.isEmpty()) {
            return 0;
        }
        try {
            double nilai = Double.parseDouble(s);
            if (nilai < 0 || nilai * 1000 > GRAM_MAKS) {
                return -1;
            }
            return (int) Math.round(nilai * 1000);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 1200 menjadi "1,2 kg", 850 menjadi "0,85 kg", 3000 menjadi "3 kg". */
    public static String kg(long gram) {
        if (gram % 1000 == 0) {
            return (gram / 1000) + " kg";
        }
        String s = String.format(Locale.ROOT, "%.2f", gram / 1000.0);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "").replace('.', ',');
        return s + " kg";
    }

    /** Teks isian berat dari gram tersimpan: 1200 menjadi "1,2"; 0 atau null menjadi "". */
    public static String isianKg(@Nullable Integer gram) {
        if (gram == null || gram <= 0) {
            return "";
        }
        String k = kg(gram);
        return k.substring(0, k.length() - 3);
    }
}
