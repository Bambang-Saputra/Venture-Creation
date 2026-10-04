package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.JualanBody;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.TemplateTasDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M09 Pasang tas (PRD-09). Pilih cetakan (nilai awal jumlah, jam, dan halal dari cetakan) atau "tas
 * campur" dengan judul, harga, perkiraan isi, dan kandungan. Mitra wajib menyatakan alergen:
 * memilih alergen atau "Tidak mengandung alergen umum" (kriteria 10).
 */
public class PasangTasViewModel extends PasangJualanViewModel {

    /** Id pilihan "tas campur". */
    public static final long CAMPUR = -1;

    public final MutableLiveData<List<TemplateTasDto>> template =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Long> terpilih = new MutableLiveData<>(CAMPUR);
    public final MutableLiveData<Integer> jumlah = new MutableLiveData<>(1);
    public final MutableLiveData<Set<String>> alergenTerpilih =
            new MutableLiveData<>(Collections.emptySet());
    public final MutableLiveData<Boolean> tanpaAlergen = new MutableLiveData<>(false);

    /** Harga tas campur yang sedang diketik, untuk potensi pemasukan. */
    public final MutableLiveData<Long> hargaCampur = new MutableLiveData<>(0L);

    public PasangTasViewModel(@NonNull Application app) {
        super(app);
    }

    @Override
    protected void muatPilihan(LofApp app, long idToko) {
        app.api()
                .templateTas(idToko)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<TemplateTasDto>> data) {
                                List<TemplateTasDto> d =
                                        data == null || data.data == null
                                                ? Collections.emptyList()
                                                : data.data;
                                template.setValue(d);
                                // Belum ada cetakan: langsung formulir tas campur (PRD-09).
                                pilih(d.isEmpty() ? CAMPUR : d.get(0).id);
                                siap();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                gagalMuat(e);
                            }
                        });
    }

    public void pilih(long id) {
        terpilih.setValue(id);
        TemplateTasDto t = cetakan();
        if (t != null) {
            jumlah.setValue(Math.max(1, Math.min(JualanMitra.JUMLAH_MAKS, t.defaultQty)));
            int m = JualanMitra.menit(t.pickupStartTime);
            int a = JualanMitra.menit(t.pickupEndTime);
            mulai.setValue(m >= 0 ? m : MULAI_BAWAAN);
            akhir.setValue(a > m ? a : AKHIR_BAWAAN);
            halalBawaan = t.halalLabel;
            halal.setValue(JualanMitra.halalBawaan(t.halalLabel));
        } else {
            halalBawaan = null;
        }
        galatField.setValue(Collections.emptyMap());
    }

    @Nullable
    public TemplateTasDto cetakan() {
        Long id = terpilih.getValue();
        List<TemplateTasDto> d = template.getValue();
        if (id == null || id == CAMPUR || d == null) {
            return null;
        }
        for (TemplateTasDto t : d) {
            if (t.id == id) {
                return t;
            }
        }
        return null;
    }

    @Override
    protected long idTemplateUntukFoto() {
        TemplateTasDto t = cetakan();
        return t == null ? 0 : t.id;
    }

    public void ubahJumlah(boolean tambah) {
        int j = jumlah.getValue() == null ? 1 : jumlah.getValue();
        jumlah.setValue(Math.max(1, Math.min(JualanMitra.JUMLAH_MAKS, j + (tambah ? 1 : -1))));
    }

    public void pilihAlergen(String kode, boolean pilih) {
        Set<String> s = new LinkedHashSet<>(alergenTerpilih.getValue());
        if (pilih) {
            s.add(kode);
            tanpaAlergen.setValue(false);
        } else {
            s.remove(kode);
        }
        alergenTerpilih.setValue(s);
        hapusGalat("allergens");
    }

    /** "Tidak mengandung alergen umum": menghapus semua pilihan alergen. */
    public void pilihTanpaAlergen(boolean pilih) {
        tanpaAlergen.setValue(pilih);
        if (pilih) {
            alergenTerpilih.setValue(Collections.emptySet());
        }
        hapusGalat("allergens");
    }

    public long harga() {
        TemplateTasDto t = cetakan();
        if (t != null) {
            return t.priceRupiah;
        }
        Long h = hargaCampur.getValue();
        return h == null ? 0 : h;
    }

    public long potensi() {
        return harga() * (jumlah.getValue() == null ? 0 : jumlah.getValue());
    }

    public void ubahHargaCampur(long harga) {
        hargaCampur.setValue(harga);
        hapusGalat("price_rupiah");
        hapusGalat("original_value_rupiah");
    }

    /**
     * Tombol "Terbitkan tas". Teks tas campur dibaca dari formulir. Galat lokal dipetakan ke kode
     * JualanMitra.G_*; fragment mengubahnya jadi teks.
     */
    public Map<String, Integer> terbitkan(
            @Nullable String judul,
            @Nullable Long hargaNormal,
            @Nullable String perkiraanIsi,
            @Nullable String kandungan) {
        boolean campur = cetakan() == null;
        Set<String> pilihan = alergenTerpilih.getValue();
        // "Dapur juga mengolah kacang" bukan pernyataan alergen isi tas; tetap wajib pilih alergen
        // atau "Tidak mengandung alergen umum".
        boolean dinyatakan =
                Boolean.TRUE.equals(tanpaAlergen.getValue())
                        || (pilihan != null && !pilihan.isEmpty());
        JualanMitra.Periksa p =
                JualanMitra.periksaTas(
                        campur,
                        judul,
                        harga(),
                        hargaNormal,
                        kandungan,
                        jumlah.getValue() == null ? 0 : jumlah.getValue(),
                        jamMulai(),
                        jamAkhir(),
                        dinyatakan);
        if (!p.sah()) {
            return p.galat;
        }
        JualanBody b = bodyDasar(JualanMitraDto.TIPE_TAS);
        b.qtyTotal = jumlah.getValue();
        b.allergens = alergenKirim(pilihan == null ? Collections.emptySet() : pilihan);
        TemplateTasDto t = cetakan();
        if (t != null) {
            b.templateId = t.id;
        } else {
            b.title = judul == null ? null : judul.trim();
            b.priceRupiah = harga();
            b.originalValueRupiah = hargaNormal;
            b.contentHint = kosongJadiNull(perkiraanIsi);
            b.ingredientsText = kosongJadiNull(kandungan);
        }
        galatField.setValue(Collections.emptyMap());
        kirim(b);
        return new LinkedHashMap<>();
    }

    @Nullable
    private static String kosongJadiNull(@Nullable String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    @Override
    protected String[] fieldGalatServer() {
        return new String[] {
            "title",
            "price_rupiah",
            "original_value_rupiah",
            "ingredients_text",
            "qty_total",
            "pickup_start",
            "pickup_end",
            "template_id",
            "allergens"
        };
    }
}
