package id.lifeoffoods.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

/**
 * Token Sanctum dan peran akun yang sedang masuk.
 *
 * <p>Disimpan di SharedPreferences privat aplikasi (ADR-0002). Berkasnya hanya bisa dibaca aplikasi
 * ini, dan tidak ikut dicadangkan karena allowBackup dimatikan di manifest.
 */
public class SesiPengguna {

    public static final String PERAN_KONSUMEN = "consumer";
    public static final String PERAN_MITRA = "partner";

    private static final String BERKAS = "sesi";
    private static final String KUNCI_TOKEN = "token";
    private static final String KUNCI_PERAN = "peran";

    private final SharedPreferences prefs;

    public SesiPengguna(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(BERKAS, Context.MODE_PRIVATE);
    }

    /** Dipanggil setelah POST /auth/otp/verify atau POST /auth/google berhasil. */
    public void simpan(String token, String peran) {
        prefs.edit().putString(KUNCI_TOKEN, token).putString(KUNCI_PERAN, peran).apply();
    }

    /** Dipanggil saat logout, akun dihapus, atau server menjawab 401. */
    public void hapus() {
        prefs.edit().clear().apply();
    }

    @Nullable
    public String token() {
        return prefs.getString(KUNCI_TOKEN, null);
    }

    /** consumer atau partner; null kalau belum masuk. */
    @Nullable
    public String peran() {
        return prefs.getString(KUNCI_PERAN, null);
    }

    public boolean sudahMasuk() {
        return token() != null && peran() != null;
    }
}
