package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Logika tampilan akun pembeli K17-K20, tanpa Android supaya bisa dites. */
public final class AkunPembeli {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

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

    private AkunPembeli() {}

    /**
     * Waktu notifikasi K17: "Baru saja", "5 menit lalu", "1 jam lalu" untuk hari ini, lalu
     * "Kemarin, 20.12" dan seterusnya seperti riwayat.
     */
    public static String waktuNotifikasi(@Nullable String iso, OffsetDateTime sekarang) {
        if (iso == null) {
            return "";
        }
        OffsetDateTime t;
        try {
            t = OffsetDateTime.parse(iso).atZoneSameInstant(WIB).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            return "";
        }
        OffsetDateTime kini = sekarang.atZoneSameInstant(WIB).toOffsetDateTime();
        if (t.toLocalDate().equals(kini.toLocalDate())) {
            long menit = Math.max(0, Duration.between(t, kini).toMinutes());
            if (menit < 1) {
                return "Baru saja";
            }
            if (menit < 60) {
                return menit + " menit lalu";
            }
            return (menit / 60) + " jam lalu";
        }
        return RiwayatMitra.waktu(iso, kini.toLocalDate());
    }

    /** Ubin "Hemat" K18: "Rp412rb", "Rp1,2jt", atau "Rp500". */
    public static String rupiahRingkas(long nilai) {
        if (nilai < 1_000) {
            return "Rp" + Math.max(0, nilai);
        }
        if (nilai < 1_000_000) {
            return "Rp" + (nilai / 1_000) + "rb";
        }
        double juta = Math.floor(nilai / 100_000d) / 10d;
        String teks =
                juta == Math.rint(juta)
                        ? String.valueOf((long) juta)
                        : String.format(Locale.ROOT, "%.1f", juta).replace('.', ',');
        return "Rp" + teks + "jt";
    }

    /** "Maret 2026" dari created_at, atau null kalau tidak terbaca. */
    @Nullable
    public static String bulanTahun(@Nullable String iso) {
        if (iso == null) {
            return null;
        }
        try {
            OffsetDateTime t = OffsetDateTime.parse(iso).atZoneSameInstant(WIB).toOffsetDateTime();
            return BULAN[t.getMonthValue() - 1] + " " + t.getYear();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
