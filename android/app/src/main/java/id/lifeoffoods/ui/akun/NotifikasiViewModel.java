package id.lifeoffoods.ui.akun;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * K17 Notifikasi (PRD-18): 20 per halaman. Setelah halaman pertama tampil, semua ditandai dibaca di
 * server supaya titik di lonceng K07 hilang; tanda dibaca di daftar tetap seperti saat dibuka.
 */
public class NotifikasiViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<NotifikasiResponse.Notifikasi>> daftar =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> adaBerikutnya = new MutableLiveData<>(false);
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private int halaman;
    private boolean sudahMuat;
    private boolean memuatLagi;
    private boolean sudahDitandai;

    public NotifikasiViewModel(@NonNull Application app) {
        super(app);
    }

    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        status.setValue(Status.MEMUAT);
        muatHalaman(1);
    }

    public void muatUlang() {
        sudahMuat = false;
        muat();
    }

    public void muatLagi() {
        if (!memuatLagi && Boolean.TRUE.equals(adaBerikutnya.getValue())) {
            memuatLagi = true;
            muatHalaman(halaman + 1);
        }
    }

    private void muatHalaman(int nomor) {
        LofApp app = getApplication();
        app.api()
                .daftarNotifikasi(nomor)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(NotifikasiResponse r) {
                                memuatLagi = false;
                                List<NotifikasiResponse.Notifikasi> gabung = new ArrayList<>();
                                if (nomor > 1 && daftar.getValue() != null) {
                                    gabung.addAll(daftar.getValue());
                                }
                                if (r != null && r.data != null) {
                                    gabung.addAll(r.data);
                                }
                                halaman = nomor;
                                daftar.setValue(gabung);
                                adaBerikutnya.setValue(r != null && r.adaBerikutnya());
                                status.setValue(Status.SIAP);
                                if (r != null && r.unreadCount > 0) {
                                    tandaiDibaca(app);
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memuatLagi = false;
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                if (nomor > 1) {
                                    pesan.setValue(new Peristiwa<>(e.pesan()));
                                    return;
                                }
                                sudahMuat = false;
                                galat.setValue(e.pesan());
                                status.setValue(Status.GAGAL);
                            }
                        });
    }

    private void tandaiDibaca(LofApp app) {
        if (sudahDitandai) {
            return;
        }
        sudahDitandai = true;
        app.api()
                .bacaSemuaNotifikasi()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Void data) {}

                            @Override
                            public void gagal(ApiError e) {
                                // Coba lagi saat layar dibuka berikutnya.
                                sudahDitandai = false;
                            }
                        });
    }
}
