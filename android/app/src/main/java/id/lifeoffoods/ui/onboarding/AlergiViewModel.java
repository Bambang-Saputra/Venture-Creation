package id.lifeoffoods.ui.onboarding;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AlergenBody;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * K05 Alergi dan pantangan. Pilihan chip disimpan di sini, bukan di view, supaya tidak hilang saat
 * layar diputar.
 */
public class AlergiViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<AlergenDto>> daftar = new MutableLiveData<>();
    public final MutableLiveData<Boolean> menyimpan = new MutableLiveData<>(false);
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> selesai = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private final Set<String> terpilih = new LinkedHashSet<>();
    private boolean sudahMuat;

    public AlergiViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dipanggil tiap onViewCreated; hanya permintaan pertama yang dikirim. */
    public void muat() {
        if (!sudahMuat) {
            muatUlang();
        }
    }

    /**
     * Daftar alergen dulu, lalu pilihan yang sudah tersimpan (misalnya kembali dari K05 ke K04 lalu
     * maju lagi, atau akun lama yang profilnya belum lengkap).
     */
    public void muatUlang() {
        sudahMuat = true;
        status.setValue(Status.MEMUAT);
        app().api()
                .daftarAlergen()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<AlergenDto>> data) {
                                List<AlergenDto> isi =
                                        data == null || data.data == null
                                                ? Collections.emptyList()
                                                : data.data;
                                muatPilihanTersimpan(isi);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                status.setValue(Status.GAGAL);
                            }
                        });
    }

    private void muatPilihanTersimpan(List<AlergenDto> isi) {
        app().api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse me) {
                                if (me != null && me.allergens != null) {
                                    for (MeResponse.Alergen a : me.allergens) {
                                        terpilih.add(a.code);
                                    }
                                }
                                tampilkan(isi);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                // Tanpa pilihan tersimpan, chip tetap bisa dipilih dari awal.
                                tampilkan(isi);
                            }
                        });
    }

    private void tampilkan(List<AlergenDto> isi) {
        daftar.setValue(isi);
        status.setValue(Status.SIAP);
    }

    public boolean terpilih(String kode) {
        return terpilih.contains(kode);
    }

    public void pilih(String kode, boolean dipilih) {
        if (dipilih) {
            terpilih.add(kode);
        } else {
            terpilih.remove(kode);
        }
    }

    public void simpan() {
        menyimpan.setValue(true);
        app().api()
                .simpanAlergen(AlergenBody.dariKode(terpilih))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<MeResponse.Alergen>> data) {
                                menyimpan.setValue(false);
                                selesai.setValue(new Peristiwa<>(true));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                menyimpan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                } else {
                                    galat.setValue(new Peristiwa<>(e.pesan()));
                                }
                            }
                        });
    }

    private LofApp app() {
        return getApplication();
    }
}
