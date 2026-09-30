package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.JualanBody;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bagian bersama M09 Pasang tas dan M16 Pasang menu satuan: jam ambil, saklar halal dan dapur
 * kacang, daftar alergen, dan pengiriman POST /listings. Status galat per field mengikuti nama
 * field API supaya galat 422 dari server bisa ditaruh di tempat yang sama.
 */
public abstract class PasangJualanViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    /** Jam ambil bawaan kalau cetakan tidak punya: 19.00 sampai 21.00. */
    static final int MULAI_BAWAAN = 19 * 60;

    static final int AKHIR_BAWAAN = 21 * 60;

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<AlergenDto>> alergen =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Integer> mulai = new MutableLiveData<>(MULAI_BAWAAN);
    public final MutableLiveData<Integer> akhir = new MutableLiveData<>(AKHIR_BAWAAN);
    public final MutableLiveData<Boolean> halal = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> dapurKacang = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> mengirim = new MutableLiveData<>(false);

    /** Nama field API ke pesan galat; kosong kalau tidak ada galat. */
    public final MutableLiveData<Map<String, String>> galatField =
            new MutableLiveData<>(Collections.emptyMap());

    public final MutableLiveData<String> galatAwal = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> terbit = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    @Nullable protected String halalBawaan;
    private boolean sudahMuat;

    protected PasangJualanViewModel(@NonNull Application app) {
        super(app);
    }

    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        status.setValue(Status.MEMUAT);
        LofApp app = getApplication();
        app.api()
                .daftarAlergen()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<AlergenDto>> data) {
                                List<AlergenDto> hanyaAlergen = new ArrayList<>();
                                if (data != null && data.data != null) {
                                    for (AlergenDto a : data.data) {
                                        if (AlergenDto.TIPE_ALERGEN.equals(a.type)) {
                                            hanyaAlergen.add(a);
                                        }
                                    }
                                }
                                alergen.setValue(hanyaAlergen);
                                TokoAktif.ambil(
                                        app,
                                        new TokoAktif.Hasil() {
                                            @Override
                                            public void siap(long idToko) {
                                                muatPilihan(app, idToko);
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                gagalMuat(e);
                                            }
                                        });
                            }

                            @Override
                            public void gagal(ApiError e) {
                                gagalMuat(e);
                            }
                        });
    }

    public void muatUlang() {
        sudahMuat = false;
        muat();
    }

    /** Cetakan (M09) atau produk (M16). Panggil {@link #siap()} atau {@link #gagalMuat}. */
    protected abstract void muatPilihan(LofApp app, long idToko);

    protected void siap() {
        status.setValue(Status.SIAP);
    }

    protected void gagalMuat(ApiError e) {
        sudahMuat = false;
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        galatAwal.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }

    public void ubahJam(boolean awal, int menit) {
        (awal ? mulai : akhir).setValue(menit);
        hapusGalat("pickup_end");
        hapusGalat("pickup_start");
    }

    public void ubahHalal(boolean nyala) {
        halal.setValue(nyala);
    }

    public void ubahDapurKacang(boolean nyala) {
        dapurKacang.setValue(nyala);
    }

    protected void hapusGalat(String field) {
        Map<String, String> g = galatField.getValue();
        if (g != null && g.containsKey(field)) {
            Map<String, String> baru = new java.util.LinkedHashMap<>(g);
            baru.remove(field);
            galatField.setValue(baru);
        }
    }

    protected int jamMulai() {
        Integer m = mulai.getValue();
        return m == null ? -1 : m;
    }

    protected int jamAkhir() {
        Integer a = akhir.getValue();
        return a == null ? -1 : a;
    }

    /** Body dengan field umum sudah terisi. */
    protected JualanBody bodyDasar(String tipe) {
        JualanBody b = new JualanBody();
        b.type = tipe;
        b.pickupStart = JualanMitra.jamApi(jamMulai());
        b.pickupEnd = JualanMitra.jamApi(jamAkhir());
        b.halalLabel = JualanMitra.labelHalal(Boolean.TRUE.equals(halal.getValue()), halalBawaan);
        return b;
    }

    /** Alergen terpilih ditambah kacang "mungkin" kalau dapur juga mengolah kacang. */
    protected List<JualanMitraDto.Alergen> alergenKirim(Set<String> dipilih) {
        return JualanMitra.alergen(
                new LinkedHashSet<>(dipilih), Boolean.TRUE.equals(dapurKacang.getValue()));
    }

    protected void kirim(JualanBody body) {
        if (Boolean.TRUE.equals(mengirim.getValue())) {
            return;
        }
        mengirim.setValue(true);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        app.api()
                                .pasangJualan(idToko, body)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(
                                                    Terbungkus<List<JualanMitraDto>> data) {
                                                mengirim.setValue(false);
                                                terbit.setValue(new Peristiwa<>(true));
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                gagalKirim(e);
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        gagalKirim(e);
                    }
                });
    }

    /** Field yang mungkin disebut galat 422 server; pesannya ditaruh di bawah field itu. */
    protected abstract String[] fieldGalatServer();

    private void gagalKirim(ApiError e) {
        mengirim.setValue(false);
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (e.kode() == 422) {
            Map<String, String> g = new java.util.LinkedHashMap<>();
            for (String f : fieldGalatServer()) {
                String p = e.pesanField(f);
                if (p != null) {
                    g.put(f, p);
                }
            }
            galatField.setValue(g);
            if (g.isEmpty()) {
                galat.setValue(new Peristiwa<>(e.pesan()));
            }
            return;
        }
        galat.setValue(new Peristiwa<>(e.pesan()));
    }
}
