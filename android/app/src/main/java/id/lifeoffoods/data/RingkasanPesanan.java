package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PratinjauPesananDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logika K12/K13 tanpa View: menyusun peringatan alergi dari pratinjau dan membaca galat 422 dari
 * POST /orders (kontrak API bagian 6). Java murni supaya bisa diuji unit.
 */
public final class RingkasanPesanan {

    /** Satu baris peringatan: jualan mana, alergen apa, pasti atau mungkin, dan seberapa berat. */
    public static final class Peringatan {
        public final String judul;
        public final String alergen;
        public final boolean mungkin;
        public final boolean berat;

        Peringatan(String judul, String alergen, boolean mungkin, boolean berat) {
            this.judul = judul;
            this.alergen = alergen;
            this.mungkin = mungkin;
            this.berat = berat;
        }
    }

    private RingkasanPesanan() {}

    /**
     * Peringatan untuk kotak sebelum tombol "Buat pesanan". Urutan mengikuti server, alergen yang
     * sama pada jualan yang sama hanya muncul sekali. Kosong kalau tidak ada yang bentrok.
     */
    public static List<Peringatan> peringatan(@Nullable PratinjauPesananDto p) {
        List<Peringatan> hasil = new ArrayList<>();
        if (p == null || p.allergenWarnings == null) {
            return hasil;
        }
        Map<Long, String> judul = new HashMap<>();
        if (p.items != null) {
            for (PratinjauPesananDto.Baris b : p.items) {
                judul.put(b.listingId, b.title);
            }
        }
        Set<String> sudah = new HashSet<>();
        for (PratinjauPesananDto.PeringatanAlergen w : p.allergenWarnings) {
            if (w == null || w.name == null || !sudah.add(w.listingId + "/" + w.code)) {
                continue;
            }
            String j = judul.get(w.listingId);
            hasil.add(
                    new Peringatan(
                            j == null ? "" : j,
                            w.name.toLowerCase(java.util.Locale.ROOT),
                            "may_contain".equals(w.presence),
                            "severe".equals(w.severity)));
        }
        return hasil;
    }

    /**
     * 422 karena pembeli masih punya 3 pesanan yang belum diambil ({@code active_order_limit});
     * K12/K13 lalu mengarahkan ke K15. Pencocokan teks hanya cadangan untuk server lama.
     */
    public static boolean batasPesananAktif(ApiError e) {
        if (e.kode() != 422) {
            return false;
        }
        if (e.kodeGalat() != null) {
            return ApiError.BATAS_PESANAN_AKTIF.equals(e.kodeGalat());
        }
        return pesanItems(e).contains("belum diambil");
    }

    /** Pesan 422 dari {@code errors.items}, atau pesan umum kalau tidak ada. */
    public static String pesanItems(ApiError e) {
        String p = e.pesanField("items");
        return p != null ? p : e.pesan();
    }
}
