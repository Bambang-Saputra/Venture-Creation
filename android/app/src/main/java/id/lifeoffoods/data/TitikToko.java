package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.ListingDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Penanda peta K08: satu titik per toko yang punya koordinat, memakai jualan termurahnya. Urutan
 * mengikuti urutan pertama toko muncul di daftar (dari server: jarak terdekat dulu).
 */
public final class TitikToko {

    /** Jualan yang ditampilkan di penanda dan kartu. */
    public final ListingDto jualan;

    /** Jumlah jualan toko itu di daftar yang sedang dimuat. */
    public final int jumlah;

    private TitikToko(ListingDto jualan, int jumlah) {
        this.jualan = jualan;
        this.jumlah = jumlah;
    }

    public double lat() {
        return jualan.store.latitude;
    }

    public double lng() {
        return jualan.store.longitude;
    }

    public static List<TitikToko> dari(@Nullable List<ListingDto> daftar) {
        Map<Long, ListingDto> termurah = new LinkedHashMap<>();
        Map<Long, Integer> hitung = new LinkedHashMap<>();
        if (daftar != null) {
            for (ListingDto l : daftar) {
                if (l == null
                        || l.store == null
                        || l.store.latitude == null
                        || l.store.longitude == null) {
                    continue;
                }
                long id = l.store.id;
                ListingDto lama = termurah.get(id);
                if (lama == null || l.priceRupiah < lama.priceRupiah) {
                    termurah.put(id, l);
                }
                Integer n = hitung.get(id);
                hitung.put(id, n == null ? 1 : n + 1);
            }
        }
        List<TitikToko> hasil = new ArrayList<>();
        for (Map.Entry<Long, ListingDto> e : termurah.entrySet()) {
            Integer n = hitung.get(e.getKey());
            hasil.add(new TitikToko(e.getValue(), n == null ? 1 : n));
        }
        return hasil;
    }

    /**
     * Tingkat zoom osmdroid yang memuat lingkaran radius di layar HP (lebar ~400dp). Tanpa radius,
     * 15 cukup untuk satu kawasan seperti SCBD.
     */
    public static double zoomUntuk(@Nullable Integer radiusKm) {
        if (radiusKm == null) {
            return 15.0;
        }
        if (radiusKm <= 1) {
            return 15.5;
        }
        if (radiusKm <= 3) {
            return 14.0;
        }
        return 13.0;
    }
}
