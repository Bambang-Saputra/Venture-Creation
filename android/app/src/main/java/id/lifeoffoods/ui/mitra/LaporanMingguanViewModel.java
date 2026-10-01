package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.LaporanMingguan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.LaporanMingguanDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.time.LocalDate;

/** M07 Laporan mingguan (PRD-13): minggu ini, bisa mundur ke minggu sebelumnya. */
public class LaporanMingguanViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL,
        /** 403: laporan hanya untuk pemilik toko (kriteria 2). */
        BUKAN_PEMILIK
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<LaporanMingguanDto> laporan = new MutableLiveData<>();
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private final LocalDate mingguIni;
    private LocalDate senin;
    private boolean sudahMuat;

    /** Nomor permintaan terakhir: jawaban minggu lama yang datang terlambat diabaikan. */
    private int permintaan;

    public LaporanMingguanViewModel(@NonNull Application app) {
        super(app);
        mingguIni = LaporanMingguan.senin(LocalDate.now(PesananMasukViewModel.WIB));
        senin = mingguIni;
    }

    public LocalDate senin() {
        return senin;
    }

    public boolean mingguIni() {
        return senin.equals(mingguIni);
    }

    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        int nomor = ++permintaan;
        String minggu = senin.toString();
        status.setValue(Status.MEMUAT);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        app.api()
                                .laporanMingguan(idToko, minggu)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(
                                                    Terbungkus<LaporanMingguanDto> data) {
                                                if (nomor != permintaan) {
                                                    return;
                                                }
                                                if (data == null || data.data == null) {
                                                    gagal(ApiError.jaringan());
                                                    return;
                                                }
                                                laporan.setValue(data.data);
                                                status.setValue(Status.SIAP);
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                if (nomor == permintaan) {
                                                    gagalMuat(e);
                                                }
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        if (nomor == permintaan) {
                            gagalMuat(e);
                        }
                    }
                });
    }

    public void muatUlang() {
        sudahMuat = false;
        muat();
    }

    /** -1 minggu sebelumnya, +1 minggu berikutnya; tidak bisa melewati minggu ini. */
    public void geser(int arah) {
        LocalDate baru = senin.plusWeeks(arah);
        if (baru.isAfter(mingguIni)) {
            return;
        }
        senin = baru;
        muatUlang();
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
