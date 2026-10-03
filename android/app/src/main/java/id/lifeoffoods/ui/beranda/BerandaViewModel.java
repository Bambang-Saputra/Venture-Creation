package id.lifeoffoods.ui.beranda;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.HalamanListing;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * K07 Beranda. Urutannya: GET /me dulu (area dan koordinat pembeli), lalu dua daftar jualan dan
 * lencana lonceng sekaligus. Tanpa koordinat, daftar tetap tampil tanpa urutan jarak.
 */
public class BerandaViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    /** Batas "Tutup kurang dari satu jam" (menit). */
    static final int BATAS_SEGERA_TUTUP = 60;

    public final MutableLiveData<String> area = new MutableLiveData<>();
    public final MutableLiveData<String> inisial = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> adaNotifikasi = new MutableLiveData<>(false);
    public final MutableLiveData<List<ListingDto>> segeraTutup =
            new MutableLiveData<>(Collections.emptyList());

    /** Populer hari ini: dari jumlah pesanan, bukan iklan. */
    public final MutableLiveData<List<ListingDto>> populer =
            new MutableLiveData<>(Collections.emptyList());

    public final MutableLiveData<List<ListingDto>> terdekat =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<String> kategori = new MutableLiveData<>(null);

    /** true kalau profil punya koordinat, jadi daftar benar-benar diurutkan dari yang terdekat. */
    public final MutableLiveData<Boolean> urutJarak = new MutableLiveData<>(false);

    /**
     * Filter yang sedang dipakai kedua daftar. Awalnya alergi profil (filter bawaan K07, PRD-04);
     * diganti hasil K09 tanpa mengubah profil.
     */
    public final MutableLiveData<FilterJualan> filter = new MutableLiveData<>(new FilterJualan());

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    @Nullable private Double lat;
    @Nullable private Double lng;
    @Nullable private FilterJualan bawaanProfil;
    private boolean filterDariPengguna;
    private boolean sudahMuat;
    private int urutanSegera;
    private int urutanPopuler;
    private int urutanTerdekat;

    public BerandaViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dipanggil tiap onViewCreated; hanya permintaan pertama yang dikirim. */
    public void muat() {
        if (!sudahMuat) {
            muatUlang();
        }
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
                                muatDaftar();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                // Jualan tetap bisa dicari walau profil gagal dimuat.
                                muatDaftar();
                            }
                        });
    }

    /** Ketuk ubin kategori: pilih, atau ketuk lagi untuk melepas. */
    public void pilihKategori(String kode) {
        kategori.setValue(Objects.equals(kode, kategori.getValue()) ? null : kode);
        muatTerdekat();
    }

    /** Hasil K09. Kedua daftar dimuat ulang dengan filter baru. */
    public void pakaiFilter(@NonNull FilterJualan baru) {
        filterDariPengguna = true;
        if (baru.equals(filter.getValue())) {
            return;
        }
        filter.setValue(baru);
        muatPopuler();
        muatSegeraTutup();
        muatTerdekat();
    }

    /** Tombol "Perluas jarak" saat daftar kosong karena batas jarak. */
    public void perluasJarak() {
        FilterJualan f = filter.getValue();
        if (f != null && f.radiusKm != null) {
            FilterJualan baru = f.salin();
            baru.radiusKm = null;
            pakaiFilter(baru);
        }
    }

    @Nullable
    public FilterJualan bawaanProfil() {
        return bawaanProfil;
    }

    private void pakaiProfil(@Nullable MeResponse me) {
        if (me == null) {
            return;
        }
        bawaanProfil = FilterJualan.dariProfil(FilterJualan.alergenProfil(me.allergens));
        if (!filterDariPengguna) {
            filter.setValue(bawaanProfil.salin());
        }
        if (me.user != null) {
            inisial.setValue(FormatTampilan.inisial(me.user.name));
        }
        MeResponse.ConsumerProfile p = me.consumerProfile;
        if (p != null) {
            area.setValue(p.areaLabel);
            lat = p.latitude;
            lng = p.longitude;
            urutJarak.setValue(lat != null && lng != null);
        }
    }

    private void muatDaftar() {
        muatPopuler();
        muatSegeraTutup();
        muatTerdekat();
        muatLencana();
    }

    /**
     * Filter alergi dan filter K09 tetap berlaku: jualan yang populer tapi mengandung alergen
     * pembeli tidak ikut tampil.
     */
    private void muatPopuler() {
        FilterJualan f = filterSekarang();
        Map<String, String> q = f.query(lat, lng);
        q.put("sort", "popular");
        q.put("per_page", "10");
        int urutan = ++urutanPopuler;
        app().api()
                .daftarListingTersaring(q, f.tipe(), f.alergenDikirim())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(HalamanListing data) {
                                if (urutan == urutanPopuler) {
                                    populer.setValue(isi(data));
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (urutan == urutanPopuler) {
                                    populer.setValue(Collections.emptyList());
                                }
                            }
                        });
    }

    private void muatSegeraTutup() {
        FilterJualan f = filterSekarang();
        Map<String, String> q = f.query(lat, lng);
        q.put("ends_within_minutes", Integer.toString(BATAS_SEGERA_TUTUP));
        q.put("per_page", "10");
        int urutan = ++urutanSegera;
        app().api()
                .daftarListingTersaring(q, f.tipe(), f.alergenDikirim())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(HalamanListing data) {
                                if (urutan == urutanSegera) {
                                    segeraTutup.setValue(isi(data));
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Bagian ini disembunyikan saja; daftar utama yang menampilkan
                                // galat.
                                if (urutan == urutanSegera) {
                                    segeraTutup.setValue(Collections.emptyList());
                                }
                            }
                        });
    }

    private void muatTerdekat() {
        FilterJualan f = filterSekarang();
        Map<String, String> q = f.query(lat, lng);
        String k = kategori.getValue();
        if (k != null) {
            q.put("category", k);
        }
        // Jawaban kategori atau filter lama yang datang terlambat diabaikan.
        int urutan = ++urutanTerdekat;
        status.setValue(Status.MEMUAT);
        app().api()
                .daftarListingTersaring(q, f.tipe(), f.alergenDikirim())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(HalamanListing data) {
                                if (urutan != urutanTerdekat) {
                                    return;
                                }
                                terdekat.setValue(isi(data));
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (urutan == urutanTerdekat) {
                                    status.setValue(Status.GAGAL);
                                }
                            }
                        });
    }

    private void muatLencana() {
        if (!app().sesi().sudahMasuk()) {
            return;
        }
        app().api()
                .notifikasi(1)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(NotifikasiResponse data) {
                                adaNotifikasi.setValue(data != null && data.unreadCount > 0);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                adaNotifikasi.setValue(false);
                            }
                        });
    }

    private FilterJualan filterSekarang() {
        FilterJualan f = filter.getValue();
        return f == null ? new FilterJualan() : f;
    }

    private static List<ListingDto> isi(@Nullable HalamanListing data) {
        return data == null || data.data == null ? Collections.emptyList() : data.data;
    }

    private LofApp app() {
        return getApplication();
    }
}
