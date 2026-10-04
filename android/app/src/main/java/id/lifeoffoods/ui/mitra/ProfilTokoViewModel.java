package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.TokoDetailDto;
import id.lifeoffoods.ui.umum.Peristiwa;

/**
 * M14 Profil toko: data dari GET /partner/stores/{store}, dan sakelar "Tutup sementara" lewat
 * PATCH. Dimuat ulang diam-diam setiap layar tampil lagi, misalnya sesudah dari M15.
 */
public class ProfilTokoViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<TokoDetailDto> toko = new MutableLiveData<>();
    public final MutableLiveData<String> galat = new MutableLiveData<>();

    /** Teks Snackbar: hasil sakelar, atau alasan gagal. */
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    /** Selama PATCH berjalan sakelar dikunci, supaya ketukan beruntun tidak saling menimpa. */
    public final MutableLiveData<Boolean> menyimpan = new MutableLiveData<>(false);

    private boolean memuat;
    private int permintaan;

    public ProfilTokoViewModel(@NonNull Application app) {
        super(app);
    }

    public void segarkan() {
        if (memuat) {
            return;
        }
        memuat = true;
        int nomor = ++permintaan;
        if (toko.getValue() == null) {
            status.setValue(Status.MEMUAT);
        }
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        app.api()
                                .detailToko(idToko)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<TokoDetailDto> data) {
                                                if (nomor != permintaan) {
                                                    return;
                                                }
                                                memuat = false;
                                                if (data == null || data.data == null) {
                                                    gagalMuat(ApiError.jaringan());
                                                    return;
                                                }
                                                toko.setValue(data.data);
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

    public void muatUlang() {
        memuat = false;
        segarkan();
    }

    /** Sakelar "Tutup sementara". Kalau server menolak, sakelar kembali ke nilai semula. */
    public void ubahTutupSementara(boolean tutup) {
        TokoDetailDto t = toko.getValue();
        if (t == null
                || !t.pemilik()
                || t.isTemporarilyClosed == tutup
                || Boolean.TRUE.equals(menyimpan.getValue())) {
            return;
        }
        menyimpan.setValue(true);
        TokoDetailDto.Ubah body = new TokoDetailDto.Ubah();
        body.isTemporarilyClosed = tutup;
        LofApp app = getApplication();
        app.api()
                .ubahToko(t.id, body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<TokoDetailDto> data) {
                                menyimpan.setValue(false);
                                if (data != null && data.data != null) {
                                    toko.setValue(data.data);
                                }
                                pesan.setValue(
                                        new Peristiwa<>(
                                                app.getString(
                                                        tutup
                                                                ? R.string.m14_tutup_aktif
                                                                : R.string.m14_tutup_nonaktif)));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                menyimpan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                // Pasang ulang nilai lama supaya sakelar kembali ke posisinya.
                                toko.setValue(toko.getValue());
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    private void gagalMuat(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (toko.getValue() != null) {
            // Muat ulang diam-diam gagal: data lama tetap ditampilkan.
            return;
        }
        galat.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }
}
