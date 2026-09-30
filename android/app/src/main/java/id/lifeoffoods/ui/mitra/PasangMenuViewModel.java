package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.JualanBody;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.ProdukDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M16 Pasang menu satuan (PRD-09). Tiap produk: stok (0 = tidak dijual hari ini), harga jual, dan
 * pernyataan alergen. Satu produk menjadi satu jualan; harga normal produk jadi harga coret.
 */
public class PasangMenuViewModel extends PasangJualanViewModel {

    /** Harga jual minimum per item di server. */
    public static final long HARGA_MIN_ITEM = 500;

    public final MutableLiveData<List<ProdukDto>> produk =
            new MutableLiveData<>(Collections.emptyList());

    /** Memicu gambar ulang baris saat stok, harga, atau alergen berubah. */
    public final MutableLiveData<Long> versi = new MutableLiveData<>(0L);

    private final Map<Long, Integer> stok = new HashMap<>();
    private final Map<Long, Long> harga = new HashMap<>();
    private final Map<Long, Set<String>> alergenItem = new HashMap<>();

    /** Produk yang alergennya sudah dinyatakan (termasuk "tidak mengandung alergen umum"). */
    private final Set<Long> dinyatakan = new LinkedHashSet<>();

    public PasangMenuViewModel(@NonNull Application app) {
        super(app);
    }

    @Override
    protected void muatPilihan(LofApp app, long idToko) {
        app.api()
                .produkToko(idToko)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<ProdukDto>> data) {
                                produk.setValue(
                                        data == null || data.data == null
                                                ? Collections.emptyList()
                                                : data.data);
                                siap();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                gagalMuat(e);
                            }
                        });
    }

    public int stok(long id) {
        Integer s = stok.get(id);
        return s == null ? 0 : s;
    }

    public long harga(long id) {
        Long h = harga.get(id);
        return h == null ? 0 : h;
    }

    public Set<String> alergen(long id) {
        Set<String> s = alergenItem.get(id);
        return s == null ? Collections.emptySet() : s;
    }

    public boolean sudahDinyatakan(long id) {
        return dinyatakan.contains(id);
    }

    public void ubahStok(long id, boolean tambah) {
        int s = Math.max(0, Math.min(JualanMitra.JUMLAH_MAKS, stok(id) + (tambah ? 1 : -1)));
        stok.put(id, s);
        hapusGalat("items");
        berubah();
    }

    /** Dari kolom harga; tidak memicu gambar ulang supaya kursor tidak melompat. */
    public void ubahHarga(long id, long nilai) {
        harga.put(id, nilai);
        hapusGalat("items");
        versi.setValue(versi.getValue());
    }

    public void simpanAlergen(long id, Set<String> kode) {
        alergenItem.put(id, new LinkedHashSet<>(kode));
        dinyatakan.add(id);
        hapusGalat("allergens");
        berubah();
    }

    private void berubah() {
        Long v = versi.getValue();
        versi.setValue(v == null ? 1 : v + 1);
    }

    public long potensi() {
        List<long[]> hj = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : stok.entrySet()) {
            if (e.getValue() > 0) {
                hj.add(new long[] {harga(e.getKey()), e.getValue()});
            }
        }
        return JualanMitra.potensi(hj);
    }

    /**
     * Tombol "Terbitkan menu". Kunci galat: "items" (tidak ada stok atau harga di bawah minimum),
     * "allergens", "pickup_end". Nilai = kode JualanMitra.G_* atau {@link #G_HARGA_ITEM}.
     */
    public Map<String, Integer> terbitkan() {
        List<ProdukDto> d = produk.getValue() == null ? Collections.emptyList() : produk.getValue();
        int dipilih = 0;
        boolean semuaDinyatakan = true;
        boolean hargaKurang = false;
        List<JualanBody.Item> items = new ArrayList<>();
        for (ProdukDto p : d) {
            int s = stok(p.id);
            if (s <= 0) {
                continue;
            }
            dipilih++;
            // Dapur kacang tidak menggantikan pernyataan alergen tiap item.
            if (!sudahDinyatakan(p.id)) {
                semuaDinyatakan = false;
            }
            if (harga(p.id) < HARGA_MIN_ITEM) {
                hargaKurang = true;
            }
            items.add(new JualanBody.Item(p.id, s, harga(p.id), alergenKirim(alergen(p.id))));
        }
        JualanMitra.Periksa cek =
                JualanMitra.periksaMenu(dipilih, jamMulai(), jamAkhir(), semuaDinyatakan);
        Map<String, Integer> galat = new LinkedHashMap<>(cek.galat);
        if (hargaKurang && !galat.containsKey("items")) {
            galat.put("items", G_HARGA_ITEM);
        }
        if (!galat.isEmpty()) {
            return galat;
        }
        JualanBody b = bodyDasar(JualanMitraDto.TIPE_MENU);
        b.items = items;
        galatField.setValue(Collections.emptyMap());
        kirim(b);
        return galat;
    }

    /** Harga item di bawah Rp500. */
    public static final int G_HARGA_ITEM = 100;

    @Override
    protected String[] fieldGalatServer() {
        return new String[] {"items", "pickup_start", "pickup_end", "allergens"};
    }
}
