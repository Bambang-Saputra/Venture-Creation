package id.lifeoffoods.ui.peta;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.HalamanListing;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * K08 Peta versi daftar (ADR-0005): jualan yang bisa dipesan, urut jarak dari lokasi di profil,
 * dengan pilihan radius. Alergi di profil tetap disaring seperti beranda.
 */
public class PetaViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<ListingDto>> daftar =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> adaBerikutnya = new MutableLiveData<>(false);

    /** null = semua jarak. */
    public final MutableLiveData<Integer> radius = new MutableLiveData<>(null);

    /** true: tab Peta (bawaan, seperti Figma); false: tab Daftar. */
    public final MutableLiveData<Boolean> tabPeta = new MutableLiveData<>(true);

    /** Toko yang penandanya dipilih di tab Peta; 0 berarti ambil toko terdekat. */
    public long tokoTerpilih;

    /** Posisi HP diminta sekali per ViewModel, bukan setiap rotasi layar. */
    public boolean posisiDiminta;

    public final MutableLiveData<String> area = new MutableLiveData<>();
    public final MutableLiveData<Boolean> adaLokasi = new MutableLiveData<>(false);
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    @Nullable private Double lat;
    @Nullable private Double lng;

    /** true setelah posisi HP diterima; profil tidak lagi menimpa lat/lng. */
    private boolean posisiPerangkat;

    @Nullable private FilterJualan filter;
    private int halaman;
    private int urutan;
    private boolean memuatLagi;

    public PetaViewModel(@NonNull Application app) {
        super(app);
    }

    @Nullable
    public Double lat() {
        return lat;
    }

    @Nullable
    public Double lng() {
        return lng;
    }

    /** Pertama kali: ambil lokasi dan alergi dari profil, lalu daftar. */
    public void muat() {
        if (filter != null) {
            return;
        }
        status.setValue(Status.MEMUAT);
        LofApp app = getApplication();
        app.api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse me) {
                                pakaiProfil(me);
                                muatHalaman(1);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                // Tanpa profil: tetap tampilkan daftar tanpa jarak dan tanpa
                                // saringan alergi, sama dengan beranda saat /me gagal.
                                pakaiProfil(null);
                                muatHalaman(1);
                            }
                        });
    }

    public void muatUlang() {
        if (filter == null) {
            muat();
            return;
        }
        status.setValue(Status.MEMUAT);
        muatHalaman(1);
    }

    /**
     * Posisi HP dari GPS/jaringan (izin diberi di K06). Lebih tepat daripada area profil, jadi
     * menggantikannya; daftar dimuat ulang kalau sudah pernah dimuat.
     */
    public void pakaiPosisiPerangkat(double latBaru, double lngBaru) {
        posisiPerangkat = true;
        lat = latBaru;
        lng = lngBaru;
        adaLokasi.setValue(true);
        area.setValue(getApplication().getString(id.lifeoffoods.R.string.k08_posisi_sekarang));
        if (filter != null) {
            status.setValue(Status.MEMUAT);
            muatHalaman(1);
        }
    }

    public void pilihRadius(@Nullable Integer km) {
        if (java.util.Objects.equals(km, radius.getValue())) {
            return;
        }
        radius.setValue(km);
        if (filter != null) {
            filter.radiusKm = km;
            status.setValue(Status.MEMUAT);
            muatHalaman(1);
        }
    }

    public void muatLagi() {
        if (!memuatLagi && Boolean.TRUE.equals(adaBerikutnya.getValue())) {
            memuatLagi = true;
            muatHalaman(halaman + 1);
        }
    }

    private void pakaiProfil(@Nullable MeResponse me) {
        filter =
                FilterJualan.dariProfil(
                        me == null ? null : FilterJualan.alergenProfil(me.allergens));
        filter.radiusKm = radius.getValue();
        MeResponse.ConsumerProfile p = me == null ? null : me.consumerProfile;
        if (p != null && !posisiPerangkat) {
            area.setValue(p.areaLabel);
            lat = p.latitude;
            lng = p.longitude;
        }
        adaLokasi.setValue(lat != null && lng != null);
    }

    private void muatHalaman(int nomor) {
        FilterJualan f = filter;
        if (f == null) {
            return;
        }
        int ini = ++urutan;
        Map<String, String> q = f.query(lat, lng);
        q.put("page", Integer.toString(nomor));
        LofApp app = getApplication();
        app.api()
                .daftarListingTersaring(q, f.tipe(), f.alergenDikirim())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(HalamanListing h) {
                                if (ini != urutan) {
                                    return;
                                }
                                memuatLagi = false;
                                List<ListingDto> gabung = new ArrayList<>();
                                if (nomor > 1 && daftar.getValue() != null) {
                                    gabung.addAll(daftar.getValue());
                                }
                                if (h != null && h.data != null) {
                                    gabung.addAll(h.data);
                                }
                                halaman = nomor;
                                daftar.setValue(gabung);
                                adaBerikutnya.setValue(h != null && h.adaBerikutnya());
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (ini != urutan) {
                                    return;
                                }
                                memuatLagi = false;
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                if (nomor > 1) {
                                    pesan.setValue(new Peristiwa<>(e.pesan()));
                                    return;
                                }
                                galat.setValue(e.pesan());
                                status.setValue(Status.GAGAL);
                            }
                        });
    }
}
