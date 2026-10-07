package id.lifeoffoods.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Aturan layar M22 Promosikan toko yang bisa diuji tanpa Android: batas lama tayang, total biaya,
 * dan teks rentang tanggal. Tarif dan tanggal mulai sebenarnya tetap ditentukan server.
 */
public final class PromosiToko {

    public static final int HARI_MIN = 1;
    public static final int HARI_MAKS = 30;
    public static final int HARI_AWAL = 7;

    private static final String[] BULAN = {
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };

    private PromosiToko() {}

    public static int batasiHari(int hari) {
        return Math.max(HARI_MIN, Math.min(HARI_MAKS, hari));
    }

    public static long total(long tarifPerHari, int hari) {
        return tarifPerHari * batasiHari(hari);
    }

    /** "6 Okt", tanpa tahun karena iklan paling lama 30 hari. */
    @NonNull
    public static String tanggalPendek(@NonNull LocalDate t) {
        return t.getDayOfMonth() + " " + BULAN[t.getMonthValue() - 1];
    }

    /** "6 Okt sampai 12 Okt"; satu hari cukup "6 Okt". */
    @NonNull
    public static String rentang(@NonNull LocalDate mulai, int hari) {
        LocalDate akhir = mulai.plusDays(batasiHari(hari) - 1L);
        return akhir.equals(mulai)
                ? tanggalPendek(mulai)
                : tanggalPendek(mulai) + " sampai " + tanggalPendek(akhir);
    }

    /** Rentang dari dua tanggal ISO yyyy-MM-dd jawaban API; null kalau tidak bisa dibaca. */
    @Nullable
    public static String rentang(@Nullable String mulai, @Nullable String akhir) {
        if (mulai == null || akhir == null) {
            return null;
        }
        try {
            LocalDate m = LocalDate.parse(mulai);
            LocalDate a = LocalDate.parse(akhir);
            return a.equals(m)
                    ? tanggalPendek(m)
                    : tanggalPendek(m) + " sampai " + tanggalPendek(a);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
