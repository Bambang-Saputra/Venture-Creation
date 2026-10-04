package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Logika tampilan M21 Riwayat pesanan mitra, tanpa Android supaya bisa dites. */
public final class RiwayatMitra {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    private static final String[] HARI = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };

    private static final String[] BULAN = {
        "Januari",
        "Februari",
        "Maret",
        "April",
        "Mei",
        "Juni",
        "Juli",
        "Agustus",
        "September",
        "Oktober",
        "November",
        "Desember"
    };

    private RiwayatMitra() {}

    /** Tanggal ambil (WIB) dari pickup_start ISO 8601, atau null kalau tidak terbaca. */
    @Nullable
    public static LocalDate tanggal(@Nullable String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(WIB).toLocalDate();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Judul kelompok hari seperti Figma M21: "Hari ini", "Kemarin", "2 hari lalu" sampai "6 hari
     * lalu", lalu nama hari dan tanggal ("Senin, 21 September").
     */
    public static String judulHari(LocalDate tanggal, LocalDate hariIni) {
        long selisih = ChronoUnit.DAYS.between(tanggal, hariIni);
        if (selisih <= 0) {
            return "Hari ini";
        }
        if (selisih == 1) {
            return "Kemarin";
        }
        if (selisih < 7) {
            return selisih + " hari lalu";
        }
        String teks = hariTanggal(tanggal);
        return tanggal.getYear() == hariIni.getYear() ? teks : teks + " " + tanggal.getYear();
    }

    /** "Sabtu, 19 September" (subjudul M08, judul kelompok lama di M21). */
    public static String hariTanggal(LocalDate tanggal) {
        return HARI[tanggal.getDayOfWeek().getValue() - 1]
                + ", "
                + tanggal.getDayOfMonth()
                + " "
                + BULAN[tanggal.getMonthValue() - 1];
    }

    /** Baris waktu riwayat saldo M13: "Hari ini, 20.34", "Kemarin, 09.00". */
    public static String waktu(@Nullable String iso, LocalDate hariIni) {
        if (iso == null) {
            return "";
        }
        try {
            OffsetDateTime t = OffsetDateTime.parse(iso).atZoneSameInstant(WIB).toOffsetDateTime();
            return judulHari(t.toLocalDate(), hariIni)
                    + ", "
                    + String.format(java.util.Locale.ROOT, "%02d.%02d", t.getHour(), t.getMinute());
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    /** Judul hari ditampilkan di baris pertama tiap tanggal. */
    public static boolean awalKelompok(@Nullable String sebelumnya, @Nullable String ini) {
        LocalDate a = tanggal(sebelumnya);
        LocalDate b = tanggal(ini);
        return a == null || b == null || !a.equals(b);
    }
}
