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
    private static final String KUNCI_PERLU_PROFIL = "perlu_profil";
    private static final String KUNCI_PESANAN = "pesanan_";
    private static final String KUNCI_PESANAN_WAKTU = "pesanan_waktu_";
    private static final String KUNCI_TOKO = "toko";

    private final SharedPreferences prefs;

    public SesiPengguna(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(BERKAS, Context.MODE_PRIVATE);
    }

    /**
     * Dipanggil setelah POST /auth/otp/verify atau POST /auth/google berhasil.
     *
     * @param perluProfil true kalau K04 belum diisi; aplikasi yang dibuka ulang kembali ke K04,
     *     bukan ke beranda.
     */
    public void simpan(String token, String peran, boolean perluProfil) {
        prefs.edit()
                .putString(KUNCI_TOKEN, token)
                .putString(KUNCI_PERAN, peran)
                .putBoolean(KUNCI_PERLU_PROFIL, perluProfil)
                .apply();
    }

    /** Dipanggil setelah PATCH /me di K04 berhasil. */
    public void profilLengkap() {
        prefs.edit().putBoolean(KUNCI_PERLU_PROFIL, false).apply();
    }

    public boolean perluProfil() {
        return prefs.getBoolean(KUNCI_PERLU_PROFIL, false);
    }

    /** Dipanggil saat logout, akun dihapus, atau server menjawab 401. */
    public void hapus() {
        prefs.edit().clear().apply();
    }

    /**
     * Toko mitra dari GET /partner/stores, disimpan supaya M11/M12 tidak memanggilnya tiap layar.
     * Selama pilot satu akun mitra memegang satu toko. Ikut terhapus di {@link #hapus()}.
     */
    public void simpanToko(long id) {
        prefs.edit().putLong(KUNCI_TOKO, id).apply();
    }

    /** 0 kalau belum diketahui. */
    public long toko() {
        return prefs.getLong(KUNCI_TOKO, 0);
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

    /**
     * K14: salinan terakhir GET /orders/{id} (JSON) supaya kode pickup tetap bisa ditunjukkan tanpa
     * sinyal (PRD-07 kriteria 7). Ikut terhapus di {@link #hapus()} saat keluar atau sesi berakhir.
     */
    public void simpanPesanan(long id, String json, long waktuMs) {
        prefs.edit()
                .putString(KUNCI_PESANAN + id, json)
                .putLong(KUNCI_PESANAN_WAKTU + id, waktuMs)
                .apply();
    }

    @Nullable
    public String pesananTersimpan(long id) {
        return prefs.getString(KUNCI_PESANAN + id, null);
    }

    /** Waktu salinan K14 disimpan (epoch ms), 0 kalau tidak ada. */
    public long waktuPesananTersimpan(long id) {
        return prefs.getLong(KUNCI_PESANAN_WAKTU + id, 0);
    }

    /** Pesanan sudah selesai atau batal: kodenya tidak perlu disimpan lagi. */
    public void lupakanPesanan(long id) {
        prefs.edit().remove(KUNCI_PESANAN + id).remove(KUNCI_PESANAN_WAKTU + id).apply();
    }
}
