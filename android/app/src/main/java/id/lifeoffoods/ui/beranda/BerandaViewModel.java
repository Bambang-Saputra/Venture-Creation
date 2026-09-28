package id.lifeoffoods.ui.beranda;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    public final MutableLiveData<List<ListingDto>> terdekat =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<String> kategori = new MutableLiveData<>(null);

    /** true kalau profil punya koordinat, jadi daftar benar-benar diurutkan dari yang terdekat. */
    public final MutableLiveData<Boolean> urutJarak = new MutableLiveData<>(false);

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    @Nullable private Double lat;
    @Nullable private Double lng;
    private boolean sudahMuat;

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

    private void pakaiProfil(@Nullable MeResponse me) {
        if (me == null) {
            return;
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
        muatSegeraTutup();
        muatTerdekat();
        muatLencana();
    }

    private void muatSegeraTutup() {
        Map<String, String> q = queryDasar();
        q.put("ends_within_minutes", Integer.toString(BATAS_SEGERA_TUTUP));
        q.put("per_page", "10");
        app().api()
                .daftarListing(q)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<ListingDto>> data) {
                                segeraTutup.setValue(isi(data));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Bagian ini disembunyikan saja; daftar utama yang menampilkan
                                // galat.
                                segeraTutup.setValue(Collections.emptyList());
                            }
                        });
    }

    private void muatTerdekat() {
        Map<String, String> q = queryDasar();
        String k = kategori.getValue();
        if (k != null) {
            q.put("category", k);
        }
        status.setValue(Status.MEMUAT);
        app().api()
                .daftarListing(q)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<ListingDto>> data) {
                                // Jawaban kategori lama yang datang terlambat diabaikan.
                                if (!Objects.equals(k, kategori.getValue())) {
                                    return;
                                }
                                terdekat.setValue(isi(data));
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (Objects.equals(k, kategori.getValue())) {
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

    private Map<String, String> queryDasar() {
        Map<String, String> q = new HashMap<>();
        if (lat != null && lng != null) {
            q.put("lat", String.format(Locale.US, "%.6f", lat));
            q.put("lng", String.format(Locale.US, "%.6f", lng));
        }
        return q;
    }

    private static List<ListingDto> isi(@Nullable Terbungkus<List<ListingDto>> data) {
        return data == null || data.data == null ? Collections.emptyList() : data.data;
    }

    private LofApp app() {
        return getApplication();
    }
}
