package id.lifeoffoods.ui.pesanan;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;

/**
 * K14 Kode pickup (PRD-07). Menampilkan salinan tersimpan lebih dulu supaya kode tetap ada tanpa
 * sinyal, lalu GET /orders/{id} tiap 10 detik selama layar terlihat dan pesanan belum selesai.
 */
public class KodePickupViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    static final long JEDA_PANTAU_MS = 10_000;

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<PesananDto> pesanan = new MutableLiveData<>();

    /** Epoch ms salinan yang sedang tampil kalau server tidak terjangkau; null kalau segar. */
    public final MutableLiveData<Long> luringSejak = new MutableLiveData<>();

    public final MutableLiveData<Boolean> membatalkan = new MutableLiveData<>(false);
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private final Gson gson = new Gson();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pantau = this::muat;
    private long id;
    private boolean sudahMulai;
    private boolean terlihat;

    public KodePickupViewModel(@NonNull Application app) {
        super(app);
    }

    public void mulai(long id) {
        if (sudahMulai) {
            return;
        }
        sudahMulai = true;
        this.id = id;
        PesananDto tersimpan = tersimpan();
        if (tersimpan != null) {
            pesanan.setValue(tersimpan);
            status.setValue(Status.SIAP);
        }
    }

    /** Dari onStart: muat sekarang lalu terus pantau. */
    public void terlihat() {
        terlihat = true;
        handler.removeCallbacks(pantau);
        muat();
    }

    /** Dari onStop: berhenti memanggil server selama layar tidak terlihat. */
    public void tersembunyi() {
        terlihat = false;
        handler.removeCallbacks(pantau);
    }

    public void muatUlang() {
        if (pesanan.getValue() == null) {
            status.setValue(Status.MEMUAT);
        }
        handler.removeCallbacks(pantau);
        muat();
    }

    private void muat() {
        app().api()
                .detailPesanan(id)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananDto> data) {
                                if (data == null || data.data == null) {
                                    gagalMuat(ApiError.jaringan());
                                    return;
                                }
                                terima(data.data);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                gagalMuat(e);
                            }
                        });
    }

    private void terima(PesananDto p) {
        pesanan.setValue(p);
        luringSejak.setValue(null);
        status.setValue(Status.SIAP);
        if (p.kodeAktif()) {
            sesi().simpanPesanan(p.id, gson.toJson(p), System.currentTimeMillis());
            jadwalkan();
        } else {
            // Selesai, batal, atau tidak diambil: kode tidak berlaku lagi, jangan disimpan.
            sesi().lupakanPesanan(p.id);
        }
    }

    private void gagalMuat(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (e.kode() == 404) {
            sesi().lupakanPesanan(id);
            pesanan.setValue(null);
            status.setValue(Status.GAGAL);
            return;
        }
        if (pesanan.getValue() != null) {
            // Tetap tampilkan kode yang tersimpan (PRD-07 kriteria 7) dan coba lagi nanti.
            long waktu = sesi().waktuPesananTersimpan(id);
            luringSejak.setValue(waktu > 0 ? waktu : null);
            jadwalkan();
        } else {
            status.setValue(Status.GAGAL);
        }
    }

    private void jadwalkan() {
        handler.removeCallbacks(pantau);
        if (terlihat) {
            handler.postDelayed(pantau, JEDA_PANTAU_MS);
        }
    }

    /** Setelah dialog konfirmasi. 409 berarti kasir sudah menukar kodenya atau pesanan lewat. */
    public void batalkan() {
        if (Boolean.TRUE.equals(membatalkan.getValue())) {
            return;
        }
        membatalkan.setValue(true);
        app().api()
                .batalkanPesanan(id)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananDto> data) {
                                membatalkan.setValue(false);
                                if (data != null && data.data != null) {
                                    terima(data.data);
                                } else {
                                    muatUlang();
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                membatalkan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                galat.setValue(new Peristiwa<>(e.pesan()));
                                if (e.kode() == 409) {
                                    muatUlang();
                                }
                            }
                        });
    }

    @Nullable
    private PesananDto tersimpan() {
        String json = sesi().pesananTersimpan(id);
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, PesananDto.class);
        } catch (JsonParseException e) {
            sesi().lupakanPesanan(id);
            return null;
        }
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(pantau);
    }

    private SesiPengguna sesi() {
        return ((LofApp) getApplication()).sesi();
    }

    private LofApp app() {
        return getApplication();
    }
}
