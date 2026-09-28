package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Format angka dan waktu untuk kartu jualan, persis seperti contoh di Figma ("Rp18.000", "380 m",
 * "48 menit", "20.30 sampai 21.00"). Java murni supaya bisa diuji unit tanpa Android.
 */
public final class FormatTampilan {

    private static final DateTimeFormatter JAM = DateTimeFormatter.ofPattern("HH.mm", Locale.US);

    private FormatTampilan() {}

    /** 18000 menjadi "Rp18.000". */
    public static String rupiah(long nilai) {
        String angka = Long.toString(Math.abs(nilai));
        StringBuilder sb = new StringBuilder();
        int awal = angka.length() % 3;
        if (awal > 0) {
            sb.append(angka, 0, awal);
        }
        for (int i = awal; i < angka.length(); i += 3) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(angka, i, i + 3);
        }
        return (nilai < 0 ? "-Rp" : "Rp") + sb;
    }

    /** Di bawah 1 km dalam meter (dibulatkan ke 10 m), selebihnya "1,2 km". Null kalau tak ada. */
    @Nullable
    public static String jarak(@Nullable Double km) {
        if (km == null || km < 0) {
            return null;
        }
        if (km < 1) {
            long meter = Math.max(10, Math.round(km * 100) * 10);
            return meter >= 1000 ? "1 km" : meter + " m";
        }
        return String.format(Locale.US, "%.1f km", km).replace('.', ',');
    }

    /** Sisa waktu sampai jam ambil berakhir: "48 menit", "1 jam 12 m", "2 jam". */
    public static String sisaWaktu(int menit) {
        if (menit < 60) {
            return Math.max(menit, 0) + " menit";
        }
        int jam = menit / 60;
        int sisa = menit % 60;
        return sisa == 0 ? jam + " jam" : jam + " jam " + sisa + " m";
    }

    /** "2026-09-18T20:30:00+07:00" dan akhir 21:00 menjadi "20.30 sampai 21.00". */
    public static String rentangJam(@Nullable String mulai, @Nullable String akhir) {
        String a = jam(mulai);
        String b = jam(akhir);
        if (a.isEmpty() || b.isEmpty()) {
            return a + b;
        }
        return a + " sampai " + b;
    }

    /** Jam dalam zona waktu yang dikirim server (WIB), bukan zona waktu HP. */
    public static String jam(@Nullable String iso) {
        if (iso == null || iso.isEmpty()) {
            return "";
        }
        try {
            return OffsetDateTime.parse(iso).format(JAM);
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    /** Inisial avatar: "Dara Renata" menjadi "DR", "Dara" menjadi "D". */
    public static String inisial(@Nullable String nama) {
        if (nama == null) {
            return "";
        }
        String[] kata = nama.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String k : kata) {
            if (!k.isEmpty() && sb.length() < 2) {
                sb.appendCodePoint(k.codePointAt(0));
            }
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }
}
