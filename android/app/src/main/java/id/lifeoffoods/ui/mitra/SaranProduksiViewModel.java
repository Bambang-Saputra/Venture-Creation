package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.SaranProduksiDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;

/**
 * M08 Saran produksi (PRD-14): saran untuk besok, "Pakai saran" dan "Abaikan" per produk. Status
 * diubah di layar lebih dulu, lalu dikembalikan kalau server menolak.
 */
public class SaranProduksiViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL,
        /** 403: saran produksi hanya untuk pemilik toko. */
        BUKAN_PEMILIK
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<SaranProduksiDto> saran = new MutableLiveData<>();
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private long idToko;
    private boolean sudahMuat;

    public SaranProduksiViewModel(@NonNull Application app) {
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
                                .saranProduksi(id)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<SaranProduksiDto> data) {
                                                if (data == null || data.data == null) {
                                                    gagalMuat(ApiError.jaringan());
                                                    return;
                                                }
                                                saran.setValue(data.data);
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

    /** "Pakai saran" atau "Abaikan" untuk satu produk. */
    public void tanggapi(SaranProduksiDto.Butir b, boolean pakai) {
        String tujuan = pakai ? SaranProduksiDto.DIPAKAI : SaranProduksiDto.DIABAIKAN;
        if (tujuan.equals(b.status)) {
            return;
        }
        kirim(b, tujuan);
    }

    /** Tombol bawah: pakai semua saran yang belum ditanggapi. */
    public void pakaiSemua() {
        SaranProduksiDto s = saran.getValue();
        if (s == null || s.items == null) {
            return;
        }
        for (SaranProduksiDto.Butir b : s.items) {
            if (SaranProduksiDto.BARU.equals(b.status)) {
                kirim(b, SaranProduksiDto.DIPAKAI);
            }
        }
    }

    private void kirim(SaranProduksiDto.Butir b, String tujuan) {
        String semula = b.status;
        ubahLokal(b, tujuan);
        String aksi = SaranProduksiDto.DIPAKAI.equals(tujuan) ? "accept" : "dismiss";
        LofApp app = getApplication();
        app.api()
                .tanggapiSaran(idToko, b.id, aksi)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<SaranProduksiDto.Tanggapan> data) {
                                // Status lokal sudah sama dengan yang disimpan server.
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                ubahLokal(b, semula);
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    /** Ubah status dan hitung ulang total hemat (saran yang diabaikan tidak dihitung). */
    private void ubahLokal(SaranProduksiDto.Butir b, @Nullable String status) {
        SaranProduksiDto s = saran.getValue();
        if (s == null) {
            return;
        }
        b.status = status;
        long total = 0;
        if (s.items != null) {
            for (SaranProduksiDto.Butir i : s.items) {
                if (!SaranProduksiDto.DIABAIKAN.equals(i.status)) {
                    total += i.estimatedSavingPerWeekRupiah;
                }
            }
        }
        s.totalSavingPerWeekRupiah = total;
        saran.setValue(s);
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
