package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * Aturan M07 Laporan mingguan (PRD-13), Java murni supaya bisa diuji unit: Senin minggu, label
 * rentang, angka ribu grafik, dan tinggi batang.
 */
public final class LaporanMingguan {

    /** Di bawah ini grafik harian diganti ajakan mencatat (PRD-13 kriteria 6). */
    public static final int HARI_MIN_GRAFIK = 3;

    private static final Locale ID = new Locale("id", "ID");
    private static final DateTimeFormatter HARI_BULAN_TAHUN =
            DateTimeFormatter.ofPattern("d MMMM yyyy", ID);
    private static final DateTimeFormatter HARI_BULAN = DateTimeFormatter.ofPattern("d MMMM", ID);

    private LaporanMingguan() {}

    /** Senin dari minggu yang memuat tanggal itu. */
    public static LocalDate senin(LocalDate tanggal) {
        return tanggal.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * "8 sampai 14 September 2026"; "29 September sampai 5 Oktober 2026"; tahun ditulis dua kali
     * kalau berbeda.
     */
    public static String rentang(LocalDate mulai, LocalDate akhir) {
        String ujung = akhir.format(HARI_BULAN_TAHUN);
        String awal;
        if (mulai.getYear() != akhir.getYear()) {
            awal = mulai.format(HARI_BULAN_TAHUN);
        } else if (mulai.getMonth() != akhir.getMonth()) {
            awal = mulai.format(HARI_BULAN);
        } else {
            awal = String.valueOf(mulai.getDayOfMonth());
        }
        return awal + " sampai " + ujung;
    }

    /** Angka di atas batang dalam ribu rupiah: 42000 menjadi "42"; 400 menjadi "<1". */
    public static String ribu(long rupiah) {
        if (rupiah > 0 && rupiah < 500) {
            return "<1";
        }
        return String.format(ID, "%,d", Math.round(rupiah / 1000.0));
    }

    /** Nilai terbesar di antara hari yang dicatat; 0 kalau tidak ada. */
    public static long maks(List<Long> nilai) {
        long m = 0;
        for (Long n : nilai) {
            if (n != null && n > m) {
                m = n;
            }
        }
        return m;
    }

    /**
     * Tinggi batang dalam px dari tinggi penuh. Hari dengan sisa > 0 minimal {@code minimum} supaya
     * tetap terlihat; 0 menjadi 0 (garis datar digambar pemanggil).
     */
    public static int tinggi(@Nullable Long nilai, long maks, int penuh, int minimum) {
        if (nilai == null || nilai <= 0 || maks <= 0) {
            return 0;
        }
        int t = (int) Math.round((double) nilai / maks * penuh);
        return Math.max(minimum, Math.min(penuh, t));
    }

    /** Lebar batang produk teratas, 0..1, relatif terhadap produk dengan nilai terbesar. */
    public static float porsi(long nilai, long maks) {
        if (nilai <= 0 || maks <= 0) {
            return 0f;
        }
        return Math.min(1f, (float) nilai / maks);
    }
}
