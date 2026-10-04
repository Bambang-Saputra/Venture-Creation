package id.lifeoffoods.ui.mitra;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AnggotaTokoDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;

import java.util.List;

/** M15 Pengaturan toko dan kasir: daftar, undang, dan cabut kasir. Hanya pemilik. */
public class PengaturanTokoViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL,
        /** 403: kasir membuka layar ini. */
        BUKAN_PEMILIK
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<AnggotaTokoDto>> anggota = new MutableLiveData<>();
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();

    /** Undangan berhasil: teks yang dibagikan lewat WhatsApp. */
    public final MutableLiveData<Peristiwa<String>> undangan = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();
    public final MutableLiveData<Boolean> mengirim = new MutableLiveData<>(false);

    private long idToko;
    private boolean sudahMuat;

    public PengaturanTokoViewModel(@NonNull Application app) {
        super(app);
    }

    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        if (anggota.getValue() == null) {
            status.setValue(Status.MEMUAT);
        }
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long id) {
                        idToko = id;
                        app.api()
                                .anggotaToko(id)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(
                                                    Terbungkus<List<AnggotaTokoDto>> data) {
                                                if (data == null || data.data == null) {
                                                    gagalMuat(ApiError.jaringan());
                                                    return;
                                                }
                                                anggota.setValue(data.data);
                                                status.setValue(Status.SIAP);
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

    /**
     * @param hp nomor yang sudah dinormalisasi (628...)
     */
    public void undang(String nama, String hp) {
        if (idToko <= 0 || Boolean.TRUE.equals(mengirim.getValue())) {
            return;
        }
        mengirim.setValue(true);
        LofApp app = getApplication();
        app.api()
                .undangKasir(idToko, new AnggotaTokoDto.Undang(hp, nama))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<AnggotaTokoDto> data) {
                                mengirim.setValue(false);
                                if (data != null && data.data != null) {
                                    String teks = data.data.inviteMessage;
                                    if (teks != null) {
                                        undangan.setValue(new Peristiwa<>(teks));
                                    }
                                }
                                muatUlang();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                mengirim.setValue(false);
                                gagalAksi(e);
                            }
                        });
    }

    public void cabut(AnggotaTokoDto a) {
        if (idToko <= 0 || a.id == null || !a.bisaDicabut()) {
            return;
        }
        LofApp app = getApplication();
        app.api()
                .cabutKasir(idToko, a.id)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Void data) {
                                pesan.setValue(
                                        new Peristiwa<>(
                                                app.getString(R.string.m15_dicabut, a.name)));
                                muatUlang();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                gagalAksi(e);
                            }
                        });
    }

    private void gagalAksi(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        // 422 (nomor tidak valid) dan 409 (nomor konsumen, sudah anggota) membawa pesan server.
        String field = e.pesanField("phone");
        pesan.setValue(new Peristiwa<>(field != null ? field : e.pesan()));
    }

    private void gagalMuat(ApiError e) {
        sudahMuat = false;
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (e.kode() == 403) {
            status.setValue(Status.BUKAN_PEMILIK);
            return;
        }
        galat.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }
}
