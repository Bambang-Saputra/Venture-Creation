package id.lifeoffoods.ui.pesanan;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.DaftarPesanan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.HalamanPesanan;
import id.lifeoffoods.data.api.model.PesananRingkasDto;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;

/**
 * K15 Pesanan saya (PRD-08). Dua tab, GET /orders?status=active|history, 20 per halaman dengan
 * gulir berkelanjutan. Dimuat ulang tiap layar terlihat lagi, supaya status yang berubah di K14
 * (dibatalkan, ditukar kasir) langsung tampil.
 */
public class PesananSayaViewModel extends AndroidViewModel {

    public enum Tab {
        AKTIF("active"),
        RIWAYAT("history");

        final String kueri;

        Tab(String kueri) {
            this.kueri = kueri;
        }
    }

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Tab> tab = new MutableLiveData<>(Tab.AKTIF);
    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<PesananRingkasDto>> daftar =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private final List<PesananRingkasDto> terkumpul = new ArrayList<>();
    private int halaman;
    private boolean adaBerikutnya;
    private boolean sedangMuat;

    /** Menandai permintaan yang masih berlaku; jawaban untuk tab lama diabaikan. */
    private int generasi;

    private Call<HalamanPesanan> berjalan;

    public PesananSayaViewModel(@NonNull Application app) {
        super(app);
    }

    public void pilihTab(Tab t) {
        if (t == tab.getValue()) {
            return;
        }
        tab.setValue(t);
        terkumpul.clear();
        daftar.setValue(Collections.emptyList());
        status.setValue(Status.MEMUAT);
        muatUlang();
    }

    /** Dari onStart dan tombol Coba lagi: mulai lagi dari halaman 1. */
    public void muatUlang() {
        generasi++;
        if (berjalan != null) {
            berjalan.cancel();
        }
        sedangMuat = false;
        halaman = 0;
        adaBerikutnya = true;
        if (terkumpul.isEmpty() || status.getValue() != Status.SIAP) {
            status.setValue(Status.MEMUAT);
        }
        muatHalaman(true);
    }

    /** Dari gulir: ambil halaman berikutnya kalau masih ada. */
    public void muatBerikutnya() {
        if (!sedangMuat && adaBerikutnya && status.getValue() == Status.SIAP) {
            muatHalaman(false);
        }
    }

    private void muatHalaman(boolean awal) {
        sedangMuat = true;
        int gen = generasi;
        Tab t = tab.getValue() == null ? Tab.AKTIF : tab.getValue();
        berjalan = ((LofApp) getApplication()).api().daftarPesanan(t.kueri, halaman + 1);
        berjalan.enqueue(
                new ApiCallback<>() {
                    @Override
                    public void sukses(HalamanPesanan data) {
                        if (gen != generasi) {
                            return;
                        }
                        sedangMuat = false;
                        if (awal) {
                            terkumpul.clear();
                        }
                        if (data != null && data.data != null) {
                            terkumpul.addAll(data.data);
                        }
                        halaman = data == null ? halaman : Math.max(halaman + 1, data.currentPage);
                        adaBerikutnya = data != null && data.adaBerikutnya();
                        daftar.setValue(
                                t == Tab.AKTIF
                                        ? DaftarPesanan.urutAktif(terkumpul)
                                        : new ArrayList<>(terkumpul));
                        status.setValue(Status.SIAP);
                    }

                    @Override
                    public void gagal(ApiError e) {
                        if (gen != generasi) {
                            return;
                        }
                        sedangMuat = false;
                        if (e.perluMasukUlang()) {
                            sesiBerakhir.setValue(new Peristiwa<>(true));
                            return;
                        }
                        if (awal) {
                            // Salinan lama tetap tampil; baru anggap gagal kalau belum ada isi.
                            if (terkumpul.isEmpty()) {
                                status.setValue(Status.GAGAL);
                            } else {
                                galat.setValue(new Peristiwa<>(e.pesan()));
                            }
                        } else {
                            adaBerikutnya = false;
                            galat.setValue(new Peristiwa<>(e.pesan()));
                        }
                    }
                });
    }

    @Override
    protected void onCleared() {
        if (berjalan != null) {
            berjalan.cancel();
        }
    }
}
