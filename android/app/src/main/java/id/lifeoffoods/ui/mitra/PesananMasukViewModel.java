package id.lifeoffoods.ui.mitra;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.HalamanPesananMitra;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * M11 Pesanan masuk (PRD-10). Dua permintaan per pembaruan: pesanan menunggu hari ini (urut jam
 * ambil) dan riwayat hari ini (untuk ubin Diambil / Tidak diambil dan tab Riwayat). Diperbarui tiap
 * 30 detik selama layar terlihat. Kalau jaringan putus, daftar terakhir tetap tampil.
 */
public class PesananMasukViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public enum Tab {
        HARI_INI,
        RIWAYAT
    }

    static final long JEDA_PANTAU_MS = 30_000;
    static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<Tab> tab = new MutableLiveData<>(Tab.HARI_INI);
    public final MutableLiveData<List<PesananMitraDto>> menunggu =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<PesananMitraDto>> riwayat =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<PesananMitra.Hitungan> hitungan =
            new MutableLiveData<>(new PesananMitra.Hitungan(0, 0, 0));

    /**
     * Epoch ms pembaruan terakhir yang berhasil kalau pembaruan terbaru gagal; null kalau segar.
     */
    public final MutableLiveData<Long> luringSejak = new MutableLiveData<>();

    public final MutableLiveData<String> galatAwal = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    /**
     * Jumlah jualan aktif hari ini untuk keadaan kosong PRD-10 ("Jualan yang aktif: {jumlah}");
     * null kalau belum termuat atau gagal, dan keadaan kosong tetap tampil tanpa angka.
     */
    public final MutableLiveData<Integer> jualanAktif = new MutableLiveData<>(null);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pantau = this::muat;
    private boolean terlihat;
    private int generasi;
    private long terakhirBerhasil;

    public PesananMasukViewModel(@NonNull Application app) {
        super(app);
    }

    public void pilihTab(Tab t) {
        tab.setValue(t);
    }

    /** Dari onStart: muat sekarang lalu pantau. */
    public void terlihat() {
        terlihat = true;
        muat();
    }

    /** Dari onStop. */
    public void tersembunyi() {
        terlihat = false;
        handler.removeCallbacks(pantau);
    }

    public void muatUlang() {
        if (status.getValue() == Status.GAGAL) {
            status.setValue(Status.MEMUAT);
        }
        muat();
    }

    private void muat() {
        handler.removeCallbacks(pantau);
        int gen = ++generasi;
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        if (gen == generasi) {
                            muatDaftar(app, idToko, gen);
                        }
                    }

                    @Override
                    public void gagal(ApiError e) {
                        if (gen == generasi) {
                            gagalMuat(e);
                        }
                    }
                });
    }

    private void muatJualanAktif(LofApp app, long idToko, int gen) {
        app.api()
                .jualanMitra(idToko, null)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<JualanMitraDto>> data) {
                                if (gen != generasi) {
                                    return;
                                }
                                int n = 0;
                                if (data != null && data.data != null) {
                                    for (JualanMitraDto j : data.data) {
                                        if (JualanMitraDto.AKTIF.equals(j.status)) {
                                            n++;
                                        }
                                    }
                                }
                                jualanAktif.setValue(n);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Hanya pelengkap keadaan kosong; galat utamanya dari daftar.
                                if (gen == generasi) {
                                    jualanAktif.setValue(null);
                                }
                            }
                        });
    }

    private void muatDaftar(LofApp app, long idToko, int gen) {
        String hariIni = LocalDate.now(WIB).toString();
        muatJualanAktif(app, idToko, gen);
        app.api()
                .pesananMitra(idToko, "pending", hariIni, 1)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(HalamanPesananMitra dataMenunggu) {
                                if (gen != generasi) {
                                    return;
                                }
                                app.api()
                                        .pesananMitra(idToko, "history", hariIni, 1)
                                        .enqueue(
                                                new ApiCallback<>() {
                                                    @Override
                                                    public void sukses(HalamanPesananMitra r) {
                                                        if (gen == generasi) {
                                                            terima(daftar(dataMenunggu), daftar(r));
                                                        }
                                                    }

                                                    @Override
                                                    public void gagal(ApiError e) {
                                                        if (gen == generasi) {
                                                            gagalMuat(e);
                                                        }
                                                    }
                                                });
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (gen == generasi) {
                                    gagalMuat(e);
                                }
                            }
                        });
    }

    private static List<PesananMitraDto> daftar(@Nullable HalamanPesananMitra h) {
        return h == null || h.data == null ? Collections.emptyList() : h.data;
    }

    private void terima(List<PesananMitraDto> m, List<PesananMitraDto> r) {
        menunggu.setValue(m);
        riwayat.setValue(r);
        hitungan.setValue(PesananMitra.hitung(m, r));
        terakhirBerhasil = System.currentTimeMillis();
        luringSejak.setValue(null);
        galatAwal.setValue(null);
        status.setValue(Status.SIAP);
        jadwalkan();
    }

    private void gagalMuat(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (e.kode() == 403 || e.kode() == 404) {
            // Toko tersimpan sudah tidak bisa diakses (misalnya kasir dicabut): ambil ulang nanti.
            ((LofApp) getApplication()).sesi().simpanToko(0);
        }
        if (status.getValue() == Status.SIAP) {
            // PRD-10: pertahankan daftar terakhir beserta jam pembaruannya.
            luringSejak.setValue(terakhirBerhasil);
        } else {
            galatAwal.setValue(e.pesan());
            status.setValue(Status.GAGAL);
        }
        jadwalkan();
    }

    private void jadwalkan() {
        handler.removeCallbacks(pantau);
        if (terlihat) {
            handler.postDelayed(pantau, JEDA_PANTAU_MS);
        }
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(pantau);
    }
}
