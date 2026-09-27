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
    private final Map<String, List<String>> galatField;

    private ApiError(int kode, String pesan, Map<String, List<String>> galatField) {
        this.kode = kode;
        this.pesan = pesan;
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
        return new ApiError(kode, pesan, field);
    }

    public static ApiError jaringan() {
        return new ApiError(TANPA_JARINGAN, PESAN_JARINGAN, Collections.emptyMap());
    }

    public int kode() {
        return kode;
    }

    public String pesan() {
        return pesan;
    }

    /** Pesan pertama untuk field tertentu, untuk ditampilkan di bawah input. */
    @Nullable
    public String pesanField(String field) {
        List<String> daftar = galatField.get(field);
        return daftar == null || daftar.isEmpty() ? null : daftar.get(0);
    }

    /** 401: token tidak berlaku lagi. Hapus sesi lalu kembali ke K01. */
    public boolean perluMasukUlang() {
        return kode == 401;
    }

    private static final class Isi {
        @SerializedName("message")
        String message;

        @SerializedName("errors")
        Map<String, List<String>> errors;
    }
}
