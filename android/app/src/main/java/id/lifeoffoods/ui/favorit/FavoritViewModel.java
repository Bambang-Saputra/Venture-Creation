package id.lifeoffoods.ui.favorit;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.FavoritDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.ProfilBody;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * K16 Favorit (F-22), tab Mitra. GET /me untuk lokasi dan sakelar notifikasi, lalu GET /favorites
 * dengan lat/lng. Hapus dari favorit langsung hilang dari daftar dan bisa dibatalkan dari Snackbar.
 */
public class FavoritViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<FavoritDto>> daftar =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> kabari = new MutableLiveData<>(false);
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();

    /** Toko baru saja dihapus: Snackbar dengan tombol Batalkan. */
    public final MutableLiveData<Peristiwa<FavoritDto>> terhapus = new MutableLiveData<>();

    /** Jualan toko yang diketuk: buka K10 (tas) atau K11 (menu). */
    public final MutableLiveData<Peristiwa<ListingDto>> bukaJualan = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    @Nullable private Double lat;
    @Nullable private Double lng;
    private int urutan;
    private int posisiTerhapus = -1;

    public FavoritViewModel(@NonNull Application app) {
        super(app);
    }

    private LofApp app() {
        return getApplication();
    }

    /** Dari onStart: favorit bisa berubah di K10/K11 selama layar ini tertutup. */
    public void muat() {
        if (status.getValue() != Status.SIAP) {
            status.setValue(Status.MEMUAT);
        }
        app().api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse me) {
                                MeResponse.ConsumerProfile p =
                                        me == null ? null : me.consumerProfile;
                                if (p != null) {
                                    lat = p.latitude;
                                    lng = p.longitude;
                                    kabari.setValue(p.notifyFavoriteStore);
                                }
                                muatDaftar();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                // Tanpa profil: daftar tetap tampil, hanya tanpa jarak.
                                muatDaftar();
                            }
                        });
    }

    private void muatDaftar() {
        Map<String, String> q = new HashMap<>();
        if (lat != null && lng != null) {
            q.put("lat", String.format(Locale.US, "%.6f", lat));
            q.put("lng", String.format(Locale.US, "%.6f", lng));
        }
        int ini = ++urutan;
        app().api()
                .daftarFavorit(q)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<FavoritDto>> data) {
                                if (ini != urutan) {
                                    return;
                                }
                                daftar.setValue(
                                        data == null || data.data == null
                                                ? new ArrayList<>()
                                                : new ArrayList<>(data.data));
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (ini != urutan) {
                                    return;
                                }
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                if (status.getValue() == Status.SIAP) {
                                    pesan.setValue(new Peristiwa<>(e.pesan()));
                                } else {
                                    galat.setValue(e.pesan());
                                    status.setValue(Status.GAGAL);
                                }
                            }
                        });
    }

    public void hapus(FavoritDto f) {
        List<FavoritDto> lama = daftar.getValue();
        int posisi = lama == null ? -1 : lama.indexOf(f);
        posisiTerhapus = posisi;
        List<FavoritDto> baru = lama == null ? new ArrayList<>() : new ArrayList<>(lama);
        baru.remove(f);
        daftar.setValue(baru);
        app().api()
                .hapusFavorit(f.id)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Void data) {
                                terhapus.setValue(new Peristiwa<>(f));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                sisipkan(f, posisi);
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    /** Tombol Batalkan di Snackbar: simpan lagi dan kembalikan ke posisi lamanya. */
    public void batalHapus(FavoritDto f) {
        int posisi = posisiTerhapus;
        sisipkan(f, posisi);
        app().api()
                .tambahFavorit(new FavoritDto.Body(f.id))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Void data) {}

                            @Override
                            public void gagal(ApiError e) {
                                List<FavoritDto> l = new ArrayList<>(daftarAtauKosong());
                                l.remove(f);
                                daftar.setValue(l);
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    private void sisipkan(FavoritDto f, int posisi) {
        List<FavoritDto> l = new ArrayList<>(daftarAtauKosong());
        if (l.contains(f)) {
            return;
        }
        l.add(posisi < 0 || posisi > l.size() ? l.size() : posisi, f);
        daftar.setValue(l);
    }

    private List<FavoritDto> daftarAtauKosong() {
        List<FavoritDto> l = daftar.getValue();
        return l == null ? Collections.emptyList() : l;
    }

    /** Sakelar di bawah daftar: sama dengan "Mitra favorit memasang jualan" di K20. */
    public void ubahKabari(boolean nyala) {
        if (Boolean.valueOf(nyala).equals(kabari.getValue())) {
            return;
        }
        kabari.setValue(nyala);
        ProfilBody body = new ProfilBody();
        body.notifyFavoriteStore = nyala;
        app().api()
                .ubahProfil(body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse data) {}

                            @Override
                            public void gagal(ApiError e) {
                                kabari.setValue(!nyala);
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    /** Ketuk kartu: buka jualan yang tersedia (tas lebih dulu, lalu menu satuan). */
    public void buka(FavoritDto f) {
        Map<String, String> q = new HashMap<>();
        q.put("store_id", Long.toString(f.id));
        q.put("per_page", "20");
        app().api()
                .daftarListing(q)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<ListingDto>> data) {
                                ListingDto pilih = null;
                                if (data != null && data.data != null) {
                                    for (ListingDto l : data.data) {
                                        if (ListingDto.TIPE_TAS.equals(l.type)) {
                                            pilih = l;
                                            break;
                                        }
                                        if (pilih == null) {
                                            pilih = l;
                                        }
                                    }
                                }
                                if (pilih == null) {
                                    pesan.setValue(
                                            new Peristiwa<>(
                                                    getApplication()
                                                            .getString(
                                                                    R.string.k16_belum_ada,
                                                                    f.name)));
                                } else {
                                    bukaJualan.setValue(new Peristiwa<>(pilih));
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }
}
