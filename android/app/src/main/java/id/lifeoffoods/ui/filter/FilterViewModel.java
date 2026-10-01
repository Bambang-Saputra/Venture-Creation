package id.lifeoffoods.ui.filter;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.HalamanListing;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.Terbungkus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * K09 Filter (PRD-04). Pilihan disimpan di sini supaya tidak hilang saat layar diputar. Profil
 * dibaca hanya sebagai isian awal dan untuk "Atur ulang"; tidak ada yang ditulis ke profil
 * (kriteria 3).
 */
public class FilterViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<FilterJualan> filter = new MutableLiveData<>(new FilterJualan());

    /** Alergen bertipe allergen dari GET /allergens (kriteria 4). */
    public final MutableLiveData<List<AlergenDto>> daftarAlergen =
            new MutableLiveData<>(Collections.emptyList());

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);

    /** Jumlah jualan yang cocok; null selama dihitung atau kalau gagal. */
    public final MutableLiveData<Integer> jumlah = new MutableLiveData<>(null);

    /** Jarak hanya bisa dipakai kalau profil punya koordinat (radius_km butuh lat/lng). */
    public final MutableLiveData<Boolean> adaLokasi = new MutableLiveData<>(false);

    @Nullable private Double lat;
    @Nullable private Double lng;
    @Nullable private FilterJualan bawaanProfil;
    private boolean dariArgumen;
    private boolean sudahMuat;
    private int urutanHitung;

    public FilterViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dipanggil tiap onViewCreated; argumen hanya dipakai sekali supaya rotasi tidak menimpa. */
    public void muat(@Nullable FilterJualan awal) {
        if (sudahMuat) {
            return;
        }
        if (awal != null) {
            filter.setValue(awal.salin());
            dariArgumen = true;
        }
        muatUlang();
    }

    public void muatUlang() {
        sudahMuat = true;
        status.setValue(Status.MEMUAT);
        app().api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse me) {
                                pakaiProfil(me);
                                muatAlergen();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Filter tetap bisa dipakai tanpa profil; hanya isian awal hilang.
                                muatAlergen();
                            }
                        });
    }

    private void pakaiProfil(@Nullable MeResponse me) {
        if (me == null) {
            return;
        }
        bawaanProfil = FilterJualan.dariProfil(FilterJualan.alergenProfil(me.allergens));
        if (!dariArgumen) {
            filter.setValue(bawaanProfil.salin());
        }
        if (me.consumerProfile != null) {
            lat = me.consumerProfile.latitude;
            lng = me.consumerProfile.longitude;
        }
        boolean lokasi = lat != null && lng != null;
        adaLokasi.setValue(lokasi);
        if (!lokasi && filter.getValue() != null && filter.getValue().radiusKm != null) {
            FilterJualan f = filter.getValue().salin();
            f.radiusKm = null;
            filter.setValue(f);
        }
    }

    private void muatAlergen() {
        app().api()
                .daftarAlergen()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<AlergenDto>> data) {
                                List<AlergenDto> hanyaAlergen = new ArrayList<>();
                                if (data != null && data.data != null) {
                                    for (AlergenDto a : data.data) {
                                        if (AlergenDto.TIPE_ALERGEN.equals(a.type)) {
                                            hanyaAlergen.add(a);
                                        }
                                    }
                                }
                                daftarAlergen.setValue(hanyaAlergen);
                                status.setValue(Status.SIAP);
                                hitung();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                status.setValue(Status.GAGAL);
                                hitung();
                            }
                        });
    }

    // ===== Perubahan pilihan =====

    /** Mengembalikan false kalau jenis terakhir mau dilepas (chip harus dicentang lagi). */
    public boolean ubahJenis(String tipe, boolean dipilih) {
        FilterJualan f = salinan();
        boolean diterima = f.ubahJenis(tipe, dipilih);
        terapkan(f);
        return diterima;
    }

    public void ubahAlergen(AlergenDto a, boolean dipilih) {
        FilterJualan f = salinan();
        if (dipilih) {
            f.alergen.put(a.code, a.name);
        } else {
            f.alergen.remove(a.code);
        }
        terapkan(f);
    }

    public void ubahSembunyikan(boolean nyala) {
        FilterJualan f = salinan();
        f.sembunyikanAlergi = nyala;
        terapkan(f);
    }

    public void ubahHalal(boolean nyala) {
        FilterJualan f = salinan();
        f.halal = nyala;
        terapkan(f);
    }

    /** Ketuk chip jarak yang sama lagi untuk melepasnya. */
    public void pilihJarak(int km, boolean dipilih) {
        FilterJualan f = salinan();
        if (dipilih) {
            f.radiusKm = km;
        } else if (f.radiusKm != null && f.radiusKm == km) {
            f.radiusKm = null;
        }
        terapkan(f);
    }

    public void pilihJam(String jam, boolean dipilih) {
        FilterJualan f = salinan();
        if (dipilih) {
            f.jam = jam;
        } else if (jam.equals(f.jam)) {
            f.jam = null;
        }
        terapkan(f);
    }

    /** Kembali ke isian profil: semua jenis, alergi profil, tanpa jarak, jam, atau halal. */
    public void aturUlang() {
        terapkan(bawaanProfil == null ? new FilterJualan() : bawaanProfil.salin());
    }

    @Nullable
    public FilterJualan bawaanProfil() {
        return bawaanProfil;
    }

    private FilterJualan salinan() {
        FilterJualan f = filter.getValue();
        return f == null ? new FilterJualan() : f.salin();
    }

    private void terapkan(FilterJualan f) {
        if (f.equals(filter.getValue())) {
            return;
        }
        filter.setValue(f);
        hitung();
    }

    /** GET /listings per_page=1 hanya untuk total. Jawaban lama yang datang terlambat diabaikan. */
    private void hitung() {
        FilterJualan f = filter.getValue();
        if (f == null) {
            return;
        }
        int urutan = ++urutanHitung;
        jumlah.setValue(null);
        Map<String, String> q = f.query(lat, lng);
        q.put("per_page", "1");
        app().api()
                .daftarListingTersaring(q, f.tipe(), f.alergenDikirim())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(HalamanListing data) {
                                if (urutan == urutanHitung) {
                                    jumlah.setValue(data == null ? 0 : data.total);
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (urutan == urutanHitung) {
                                    jumlah.setValue(null);
                                }
                            }
                        });
    }

    private LofApp app() {
        return getApplication();
    }
}
