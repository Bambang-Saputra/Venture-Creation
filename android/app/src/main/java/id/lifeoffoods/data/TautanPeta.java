package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * URI geo: untuk K08 (ADR-0005). Peta di dalam aplikasi belum ada karena butuh penagihan Google
 * Maps; tombol "Rute" dan "Buka di Google Maps" menyerahkan peta ke aplikasi peta di HP.
 */
public final class TautanPeta {

    private TautanPeta() {}

    /**
     * Pin di lokasi toko dengan label nama toko, contoh {@code
     * geo:-6.2297,106.8583?q=-6.2297,106.8583(Roti%20Sari)}. Tanpa koordinat, cari alamatnya.
     */
    @Nullable
    public static String toko(
            @Nullable Double lat,
            @Nullable Double lng,
            @Nullable String nama,
            @Nullable String alamat) {
        if (lat != null && lng != null) {
            String titik = koordinat(lat) + "," + koordinat(lng);
            String label = nama == null || nama.isEmpty() ? "" : "(" + kode(nama) + ")";
            return "geo:" + titik + "?q=" + titik + label;
        }
        if (alamat != null && !alamat.isEmpty()) {
            return "geo:0,0?q=" + kode(alamat);
        }
        return null;
    }

    /** Peta di sekitar lokasi pembeli, atau area teks kalau koordinatnya belum ada. */
    @Nullable
    public static String sekitar(
            @Nullable Double lat, @Nullable Double lng, @Nullable String area) {
        if (lat != null && lng != null) {
            return "geo:" + koordinat(lat) + "," + koordinat(lng) + "?z=15";
        }
        if (area != null && !area.isEmpty()) {
            return "geo:0,0?q=" + kode(area);
        }
        return null;
    }

    private static String koordinat(double v) {
        return String.format(Locale.ROOT, "%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String kode(String s) {
        try {
            // encode(String, Charset) baru ada di API 33; minSdk 27.
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
