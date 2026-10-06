package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.PromosiToko;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PromosiDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

/** M22 Promosikan toko: tarif paket, iklan toko, lalu beli. Hanya pemilik. */
public class PromosiTokoViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL,
        BUKAN_PEMILIK
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<PromosiDto.Halaman> halaman = new MutableLiveData<>();
    public final MutableLiveData<String> paket = new MutableLiveData<>(PromosiDto.PRIORITAS);
    public final MutableLiveData<Integer> hari = new MutableLiveData<>(PromosiToko.HARI_AWAL);
    public final MutableLiveData<Boolean> menyimpan = new MutableLiveData<>(false);
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private long idToko;
    private boolean sudahMuat;

    public PromosiTokoViewModel(@NonNull Application app) {
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
                        muatDaftar();
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

    public void pilihPaket(@NonNull String kode) {
        paket.setValue(kode);
    }

    public void ubahHari(int selisih) {
        Integer h = hari.getValue();
        hari.setValue(PromosiToko.batasiHari((h == null ? PromosiToko.HARI_AWAL : h) + selisih));
    }

    /** Tarif paket terpilih dari server; 0 sebelum data dimuat. */
    public long tarif() {
        PromosiDto.Paket p = paketTerpilih();
        return p == null ? 0 : p.pricePerDayRupiah;
    }

    @Nullable
    public PromosiDto.Paket paketTerpilih() {
        PromosiDto.Halaman h = halaman.getValue();
        if (h == null || h.packages == null) {
            return null;
        }
        for (PromosiDto.Paket p : h.packages) {
            if (p.code != null && p.code.equals(paket.getValue())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Tanggal mulai yang akan dipakai server: hari ini, atau sehari setelah iklan paket yang sama
     * berakhir kalau masih tayang.
     */
    @NonNull
    public LocalDate mulai() {
        LocalDate hariIni = LocalDate.now(PesananMasukViewModel.WIB);
        LocalDate mulai = hariIni;
        PromosiDto.Halaman h = halaman.getValue();
        List<PromosiDto> daftar = h == null || h.data == null ? Collections.emptyList() : h.data;
        for (PromosiDto p : daftar) {
            if (p.paket == null || !p.paket.equals(paket.getValue()) || p.endsOn == null) {
                continue;
            }
            try {
                LocalDate setelah = LocalDate.parse(p.endsOn).plusDays(1);
                if (setelah.isAfter(mulai)) {
                    mulai = setelah;
                }
            } catch (DateTimeParseException ignored) {
                // Tanggal rusak dari server tidak menggeser perkiraan.
            }
        }
        return mulai;
    }

    public void aktifkan(@Nullable String kalimat) {
        if (Boolean.TRUE.equals(menyimpan.getValue()) || idToko == 0) {
            return;
        }
        String kode = paket.getValue();
        Integer h = hari.getValue();
        String isi = kalimat == null ? null : kalimat.trim();
        boolean banner = PromosiDto.BANNER.equals(kode);
        PromosiDto.Body body =
                new PromosiDto.Body(
                        kode,
                        h == null ? PromosiToko.HARI_AWAL : h,
                        banner && isi != null && !isi.isEmpty() ? isi : null);
        menyimpan.setValue(true);
        LofApp app = getApplication();
        app.api()
                .beliPromosi(idToko, body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PromosiDto> data) {
                                menyimpan.setValue(false);
                                pesan.setValue(
                                        new Peristiwa<>(app.getString(R.string.m22_berhasil)));
                                muatDaftar();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                menyimpan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    private void muatDaftar() {
        LofApp app = getApplication();
        app.api()
                .promosiToko(idToko)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(PromosiDto.Halaman data) {
                                if (data == null || data.packages == null) {
                                    gagalMuat(ApiError.jaringan());
                                    return;
                                }
                                halaman.setValue(data);
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
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
