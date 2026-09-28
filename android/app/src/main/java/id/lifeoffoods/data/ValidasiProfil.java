package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Pemeriksaan isian K04 di sisi aplikasi, sama dengan aturan PATCH /me (kontrak API): nama 2 sampai
 * 120 karakter, email opsional. Server tetap memeriksa ulang, termasuk email yang sudah dipakai
 * akun lain.
 *
 * <p>Tidak memakai android.util.Patterns supaya bisa diuji di JVM biasa.
 */
public final class ValidasiProfil {

    public static final int NAMA_MIN = 2;
    public static final int NAMA_MAKS = 120;

    // Cukup untuk menolak salah ketik yang umum; keputusan akhirnya tetap di server.
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private ValidasiProfil() {}

    /** Nama tanpa spasi berlebih, atau null kalau tidak memenuhi aturan. */
    @Nullable
    public static String nama(@Nullable String masukan) {
        if (masukan == null) {
            return null;
        }
        String rapi = masukan.trim().replaceAll("\\s+", " ");
        return rapi.length() >= NAMA_MIN && rapi.length() <= NAMA_MAKS ? rapi : null;
    }

    /** Kosong dianggap tidak diisi (opsional), jadi sah. */
    public static boolean emailSah(@Nullable String masukan) {
        String rapi = masukan == null ? "" : masukan.trim();
        return rapi.isEmpty() || EMAIL.matcher(rapi).matches();
    }

    /** Teks yang sudah dirapikan, atau null kalau kosong (tidak dikirim ke server). */
    @Nullable
    public static String kosongJadiNull(@Nullable String masukan) {
        if (masukan == null) {
            return null;
        }
        String rapi = masukan.trim();
        return rapi.isEmpty() ? null : rapi;
    }
}
