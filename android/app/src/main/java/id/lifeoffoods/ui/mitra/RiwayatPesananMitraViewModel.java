package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.HalamanPesananMitra;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * M21 Riwayat pesanan mitra: tab Riwayat di M11, dengan saringan 7 hari, 30 hari, atau semua, dan
 * "Muat lebih banyak" untuk halaman berikutnya (30 pesanan per halaman).
 */
public class RiwayatPesananMitraViewModel extends AndroidViewModel {

    public enum Saringan {
        TUJUH_HARI(7),
        TIGA_PULUH_HARI(30),
        SEMUA(null);

        @Nullable final Integer hari;

        Saringan(@Nullable Integer hari) {
            this.hari = hari;
        }
    }

    public enum Status {
        KOSONG_BELUM_DIMUAT,
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.KOSONG_BELUM_DIMUAT);
    public final MutableLiveData<Saringan> saringan = new MutableLiveData<>(Saringan.TUJUH_HARI);
    public final MutableLiveData<List<PesananMitraDto>> daftar =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<HalamanPesananMitra.Ringkasan> ringkasan = new MutableLiveData<>();
    public final MutableLiveData<Boolean> adaBerikutnya = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> memuatLagi = new MutableLiveData<>(false);
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galatLagi = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private int halaman;
    private int generasi;

    public RiwayatPesananMitraViewModel(@NonNull Application app) {
        super(app);
    }

    /** Pertama kali tab Riwayat dibuka. */
    public void pastikanTermuat() {
        if (status.getValue() == Status.KOSONG_BELUM_DIMUAT) {
            muatUlang();
        }
    }

    public void pilih(Saringan s) {
        if (s == saringan.getValue() && status.getValue() == Status.SIAP) {
            return;
        }
        saringan.setValue(s);
        muatUlang();
    }

    public void muatUlang() {
        halaman = 0;
        status.setValue(Status.MEMUAT);
        muatHalaman(1);
    }

    public void muatLagi() {
        if (!Boolean.TRUE.equals(adaBerikutnya.getValue())
                || Boolean.TRUE.equals(memuatLagi.getValue())) {
            return;
        }
        memuatLagi.setValue(true);
        muatHalaman(halaman + 1);
    }

    private void muatHalaman(int nomor) {
        int gen = ++generasi;
        Saringan s = saringan.getValue();
        Integer hari = s == null ? null : s.hari;
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        app.api()
                                .riwayatPesananMitra(idToko, hari, nomor)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(HalamanPesananMitra h) {
                                                if (gen == generasi) {
                                                    terima(nomor, h);
                                                }
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                if (gen == generasi) {
                                                    gagalMuat(nomor, e);
                                                }
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        if (gen == generasi) {
                            gagalMuat(nomor, e);
                        }
                    }
                });
    }

    private void terima(int nomor, @Nullable HalamanPesananMitra h) {
        memuatLagi.setValue(false);
        List<PesananMitraDto> baru = h == null || h.data == null ? Collections.emptyList() : h.data;
        List<PesananMitraDto> gabung = new ArrayList<>();
        if (nomor > 1 && daftar.getValue() != null) {
            gabung.addAll(daftar.getValue());
        }
        gabung.addAll(baru);
        halaman = nomor;
        daftar.setValue(gabung);
        if (h != null && h.summary != null) {
            ringkasan.setValue(h.summary);
        }
        adaBerikutnya.setValue(h != null && h.adaBerikutnya());
        status.setValue(Status.SIAP);
    }

    private void gagalMuat(int nomor, ApiError e) {
        memuatLagi.setValue(false);
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (nomor > 1) {
            // Halaman berikutnya gagal: daftar yang sudah ada tetap tampil.
            galatLagi.setValue(new Peristiwa<>(e.pesan()));
            return;
        }
        galat.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }
}
