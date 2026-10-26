package id.lifeoffoods.notifikasi;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse.Notifikasi;
import java.util.ArrayList;
import java.util.List;

/**
 * Id notifikasi terakhir yang sudah ditampilkan (sistem atau pop up), supaya pekerja latar dan pop
 * di dalam aplikasi tidak menampilkan notifikasi yang sama dua kali. Pemeriksaan pertama hanya
 * mencatat titik awal, jadi notifikasi lama tidak dibanjirkan begitu fitur ini aktif.
 */
public final class CatatanNotifikasi {

    private static final String BERKAS = "notifikasi_tampil";
    private static final String KUNCI_ID = "id_terakhir";
    private static final String KUNCI_IZIN = "izin_ditanya";

    /** Paling banyak yang ditampilkan dalam satu pemeriksaan. */
    private static final int MAKS_SEKALIGUS = 3;

    private CatatanNotifikasi() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(BERKAS, Context.MODE_PRIVATE);
    }

    /**
     * Notifikasi belum dibaca yang lebih baru dari yang terakhir tampil, terbaru dulu; sekaligus
     * mencatat id terbesar sebagai yang sudah tampil.
     */
    public static synchronized List<Notifikasi> ambilBaru(
            Context c, @Nullable NotifikasiResponse r) {
        List<Notifikasi> baru = new ArrayList<>();
        if (r == null || r.data == null) {
            return baru;
        }
        long terakhir = prefs(c).getLong(KUNCI_ID, -1);
        long maks = terakhir;
        for (Notifikasi n : r.data) {
            maks = Math.max(maks, n.id);
            if (terakhir >= 0 && n.id > terakhir && !n.isRead && baru.size() < MAKS_SEKALIGUS) {
                baru.add(n);
            }
        }
        prefs(c).edit().putLong(KUNCI_ID, Math.max(maks, 0)).apply();
        return baru;
    }

    public static boolean izinSudahDitanya(Context c) {
        return prefs(c).getBoolean(KUNCI_IZIN, false);
    }

    public static void tandaiIzinDitanya(Context c) {
        prefs(c).edit().putBoolean(KUNCI_IZIN, true).apply();
    }

    /** Saat keluar akun: notifikasi akun berikutnya mulai dari titik awal baru. */
    public static void lupakan(Context c) {
        prefs(c).edit().remove(KUNCI_ID).apply();
    }
}
