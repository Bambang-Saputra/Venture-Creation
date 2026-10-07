package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.DasborMitra;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.RingkasanTokoDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.Collections;
import java.util.List;

/**
 * M05 Dashboard mitra (PRD-24): ringkasan toko dari /summary dan tas hari ini dari /listings.
 * Dimuat ulang diam-diam setiap layar tampil lagi, misalnya sesudah mencatat sisa di M06, supaya
 * pengingat tidak tertinggal.
 */
public class DasborMitraViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<RingkasanTokoDto> ringkasan = new MutableLiveData<>();

    /** null = belum termuat atau gagal; kartu tas disembunyikan, sisa dashboard tetap jalan. */
    public final MutableLiveData<List<JualanMitraDto>> tas = new MutableLiveData<>();

    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    /** Akun mitra belum punya toko: fragment membuka M03 Daftar sebagai mitra. */
    public final MutableLiveData<Peristiwa<Boolean>> perluDaftar = new MutableLiveData<>();

    private boolean memuat;

    /** Nomor permintaan terakhir: jawaban lama yang datang terlambat diabaikan. */
    private int permintaan;

    public DasborMitraViewModel(@NonNull Application app) {
        super(app);
    }

    /** Pertama kali dengan layar memuat; sesudahnya diam-diam di belakang data lama. */
    public void segarkan() {
        if (memuat) {
            return;
        }
        memuat = true;
        int nomor = ++permintaan;
        if (ringkasan.getValue() == null) {
            status.setValue(Status.MEMUAT);
        }
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        muatTas(app, idToko, nomor);
                        app.api()
                                .ringkasanToko(idToko)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<RingkasanTokoDto> data) {
                                                if (nomor != permintaan) {
                                                    return;
                                                }
                                                memuat = false;
                                                if (data == null || data.data == null) {
                                                    gagalMuat(ApiError.jaringan());
                                                    return;
                                                }
                                                ringkasan.setValue(data.data);
                                                status.setValue(Status.SIAP);
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                if (nomor == permintaan) {
                                                    memuat = false;
                                                    gagalMuat(e);
                                                }
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        if (nomor == permintaan) {
                            memuat = false;
                            gagalMuat(e);
                        }
                    }
                });
    }

    private void muatTas(LofApp app, long idToko, int nomor) {
        app.api()
                .jualanMitra(idToko, JualanMitraDto.TIPE_TAS)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<JualanMitraDto>> data) {
                                if (nomor == permintaan) {
                                    tas.setValue(
                                            DasborMitra.tasHariIni(
                                                    data == null
                                                            ? Collections.emptyList()
                                                            : data.data));
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Tetap tampilkan daftar lama kalau ada; kalau belum, kartunya
                                // disembunyikan.
                            }
                        });
    }

    private void gagalMuat(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (e.belumAdaToko()) {
            perluDaftar.setValue(new Peristiwa<>(true));
            return;
        }
        // Penyegaran diam-diam yang gagal tidak menimpa dashboard yang sudah tampil.
        if (ringkasan.getValue() != null) {
            return;
        }
        galat.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }
}
