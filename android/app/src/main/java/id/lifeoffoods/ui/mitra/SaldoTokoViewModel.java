package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.SaldoTokoDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** M13 Saldo dan pencairan: saldo, lalu riwayat transaksi 20 per halaman. Hanya pemilik. */
public class SaldoTokoViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL,
        BUKAN_PEMILIK
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<SaldoTokoDto> saldo = new MutableLiveData<>();
    public final MutableLiveData<List<SaldoTokoDto.Transaksi>> transaksi =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> adaBerikutnya = new MutableLiveData<>(false);
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private long idToko;
    private int halaman;
    private boolean sudahMuat;
    private boolean memuatLagi;

    public SaldoTokoViewModel(@NonNull Application app) {
        super(app);
    }

    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        status.setValue(Status.MEMUAT);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long id) {
                        idToko = id;
                        app.api()
                                .saldoToko(id)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<SaldoTokoDto> data) {
                                                if (data == null || data.data == null) {
                                                    gagalMuat(ApiError.jaringan());
                                                    return;
                                                }
                                                saldo.setValue(data.data);
                                                muatTransaksi(1);
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

    public void muatLagi() {
        if (!memuatLagi && Boolean.TRUE.equals(adaBerikutnya.getValue())) {
            memuatLagi = true;
            muatTransaksi(halaman + 1);
        }
    }

    private void muatTransaksi(int nomor) {
        LofApp app = getApplication();
        app.api()
                .transaksiSaldo(idToko, nomor)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(SaldoTokoDto.HalamanTransaksi h) {
                                memuatLagi = false;
                                List<SaldoTokoDto.Transaksi> gabung = new ArrayList<>();
                                if (nomor > 1 && transaksi.getValue() != null) {
                                    gabung.addAll(transaksi.getValue());
                                }
                                if (h != null && h.data != null) {
                                    gabung.addAll(h.data);
                                }
                                halaman = nomor;
                                transaksi.setValue(gabung);
                                adaBerikutnya.setValue(h != null && h.adaBerikutnya());
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memuatLagi = false;
                                if (nomor > 1 && !e.perluMasukUlang()) {
                                    pesan.setValue(new Peristiwa<>(e.pesan()));
                                    return;
                                }
                                gagalMuat(e);
                            }
                        });
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
