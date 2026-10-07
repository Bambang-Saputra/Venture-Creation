package id.lifeoffoods.data.api;

import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Galat dari API dalam bentuk yang siap ditampilkan.
 *
 * <p>Server selalu mengirim {@code message} berbahasa Indonesia, dan untuk 422 juga {@code errors}
 * per field (kontrak API bagian 1, "Galat"). Kode 0 berarti permintaan tidak sampai ke server.
 */
public final class ApiError {

    public static final int TANPA_JARINGAN = 0;

    private static final Gson GSON = new Gson();
    private static final String PESAN_JARINGAN =
            "Tidak bisa terhubung ke server. Periksa koneksi internet lalu coba lagi.";
    private static final String PESAN_UMUM = "Terjadi kesalahan di server. Coba lagi sebentar.";

    private final int kode;
    private final String pesan;
    @Nullable private final String kodeGalat;
    private final Map<String, List<String>> galatField;

    private ApiError(
            int kode,
            String pesan,
            @Nullable String kodeGalat,
            Map<String, List<String>> galatField) {
        this.kode = kode;
        this.pesan = pesan;
        this.kodeGalat = kodeGalat;
        this.galatField = galatField;
    }

    /** Dari respons HTTP yang tidak sukses. Body boleh null atau bukan JSON. */
    public static ApiError dariRespons(int kode, @Nullable String body) {
        Isi isi = null;
        if (body != null && !body.isEmpty()) {
            try {
                isi = GSON.fromJson(body, Isi.class);
            } catch (JsonParseException e) {
                // Misalnya halaman HTML dari ngrok atau proxy; pakai pesan umum.
            }
        }
        String pesan = isi != null && isi.message != null ? isi.message : PESAN_UMUM;
        Map<String, List<String>> field =
                isi != null && isi.errors != null ? isi.errors : Collections.emptyMap();
        return new ApiError(kode, pesan, isi == null ? null : isi.code, field);
    }

    public static ApiError jaringan() {
        return new ApiError(TANPA_JARINGAN, PESAN_JARINGAN, null, Collections.emptyMap());
    }

    public int kode() {
        return kode;
    }

    public String pesan() {
        return pesan;
    }

    /**
     * Kode galat tetap dari server ({@code code}), misalnya {@link #AKUN_NONAKTIF}. null kalau
     * server tidak mengirimnya; galat biasa memang tidak punya kode.
     */
    @Nullable
    public String kodeGalat() {
        return kodeGalat;
    }

    /** Pesan pertama untuk field tertentu, untuk ditampilkan di bawah input. */
    @Nullable
    public String pesanField(String field) {
        List<String> daftar = galatField.get(field);
        return daftar == null || daftar.isEmpty() ? null : daftar.get(0);
    }

    /**
     * 401, atau 403 "Akun ini dinonaktifkan" (middleware PastikanAkunAktif): token tidak berlaku
     * lagi. Hapus sesi lalu kembali ke K01 (PRD-01 kriteria 10).
     */
    public boolean perluMasukUlang() {
        return kode == 401 || akunNonaktif();
    }

    /**
     * Akun dinonaktifkan ({@code account_inactive}). 403 lain (kasir membuka laporan, alergi untuk
     * mitra) tetap 403 biasa. Pencocokan teks hanya cadangan untuk server lama tanpa {@code code}.
     */
    public boolean akunNonaktif() {
        return kode == 403
                && (AKUN_NONAKTIF.equals(kodeGalat)
                        || (kodeGalat == null && pesan != null && pesan.contains("dinonaktifkan")));
    }

    /**
     * 403 dari verifikasi OTP atau Google: nomor terdaftar dengan peran lain ({@code wrong_role},
     * "Masuk lewat halaman mitra").
     */
    public boolean salahHalaman() {
        return kode == 403
                && (SALAH_PERAN.equals(kodeGalat)
                        || (kodeGalat == null
                                && pesan != null
                                && pesan.contains("Masuk lewat halaman")));
    }

    /** Akun mitra yang belum punya toko; lihat {@link #BELUM_ADA_TOKO}. */
    public boolean belumAdaToko() {
        return BELUM_ADA_TOKO.equals(kodeGalat);
    }

    /** Kode galat tetap dari server (kontrak API bagian 1, "Galat"). */
    public static final String AKUN_NONAKTIF = "account_inactive";

    public static final String SALAH_PERAN = "wrong_role";

    /**
     * Dibuat aplikasi, bukan dari server: GET /partner/stores kosong karena pendaftaran toko belum
     * disetujui. M05 lalu membuka M03/M04.
     */
    public static final String BELUM_ADA_TOKO = "no_store";

    public static final String BATAS_PESANAN_AKTIF = "active_order_limit";

    private static final class Isi {
        @SerializedName("message")
        String message;

        @SerializedName("code")
        String code;

        @SerializedName("errors")
        Map<String, List<String>> errors;
    }
}
