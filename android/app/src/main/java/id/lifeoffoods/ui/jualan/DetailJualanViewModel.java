package id.lifeoffoods.ui.jualan;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.Keranjang;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.FavoritDto;
import id.lifeoffoods.data.api.model.ListingDetailDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dipakai K10 Detail tas kejutan dan K11 Detail menu satuan. Keduanya memuat detail satu jualan
 * (toko, jam, kandungan), alergi di profil untuk kotak peringatan, status favorit toko, dan menu
 * satuan toko itu: K10 memakainya untuk tautan "Lihat menu satuan", K11 untuk daftar pilihan.
 */
public class DetailJualanViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<ListingDetailDto> detail = new MutableLiveData<>();
    public final MutableLiveData<List<ListingDto>> menuToko =
            new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Set<String>> alergiProfil =
            new MutableLiveData<>(Collections.emptySet());

    /** Null kalau profil atau toko belum punya koordinat. */
    public final MutableLiveData<Double> jarakKm = new MutableLiveData<>();

    public final MutableLiveData<Boolean> favorit = new MutableLiveData<>(false);

    /** Berubah tiap kali jumlah di keranjang berubah; nilainya total qty. */
    public final MutableLiveData<Integer> keranjangBerubah = new MutableLiveData<>(0);

    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    public final Keranjang keranjang = new Keranjang();

    private long idJualan;
    private boolean sudahMuat;
    private boolean menuSatuan;
    @Nullable private Double latSaya;
    @Nullable private Double lngSaya;

    public DetailJualanViewModel(@NonNull Application app) {
        super(app);
    }

    /**
     * @param menuSatuan true di K11: semua menu satuan toko masuk keranjang, jualan yang dibuka
     *     langsung berisi 1. Di K10 hanya tas yang dibuka, minimal 1.
     */
    public void muat(long id, boolean menuSatuan) {
        if (sudahMuat && id == idJualan) {
            return;
        }
        sudahMuat = true;
        idJualan = id;
        this.menuSatuan = menuSatuan;
        muatUlang();
    }

    public void muatUlang() {
        status.setValue(Status.MEMUAT);
        muatProfil();
        app().api()
                .detailListing(idJualan)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<ListingDetailDto> data) {
                                if (data == null || data.data == null) {
                                    status.setValue(Status.GAGAL);
                                    return;
                                }
                                ListingDetailDto d = data.data;
                                keranjang.daftarkan(
                                        d.id, d.priceRupiah, d.isAvailable ? d.qtyRemaining : 0);
                                if (keranjang.qty(d.id) == 0) {
                                    keranjang.atur(d.id, 1);
                                }
                                detail.setValue(d);
                                hitungJarak();
                                keranjangBerubah.setValue(keranjang.totalQty());
                                status.setValue(Status.SIAP);
                                if (d.store != null) {
                                    muatMenuToko(d.store.id);
                                    muatFavorit(d.store.id);
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                status.setValue(Status.GAGAL);
                            }
                        });
    }

    private void muatProfil() {
        if (!app().sesi().sudahMasuk()) {
            return;
        }
        app().api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse me) {
                                Set<String> kode = new HashSet<>();
                                if (me != null && me.allergens != null) {
                                    for (MeResponse.Alergen a : me.allergens) {
                                        kode.add(a.code);
                                    }
                                }
                                if (me != null && me.consumerProfile != null) {
                                    latSaya = me.consumerProfile.latitude;
                                    lngSaya = me.consumerProfile.longitude;
                                }
                                alergiProfil.setValue(kode);
                                hitungJarak();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                }
                            }
                        });
    }

    private void muatMenuToko(long storeId) {
        Map<String, String> q = new HashMap<>();
        q.put("store_id", Long.toString(storeId));
        q.put("type[]", ListingDto.TIPE_MENU);
        q.put("per_page", "50");
        app().api()
                .daftarListing(q)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<ListingDto>> data) {
                                List<ListingDto> isi =
                                        data == null || data.data == null
                                                ? new ArrayList<>()
                                                : new ArrayList<>(data.data);
                                if (menuSatuan) {
                                    susunMenu(isi);
                                }
                                menuToko.setValue(isi);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // K10 cukup tanpa tautan; K11 tetap bisa memesan jualan yang
                                // dibuka.
                                if (menuSatuan) {
                                    List<ListingDto> isi = new ArrayList<>();
                                    susunMenu(isi);
                                    menuToko.setValue(isi);
                                }
                            }
                        });
    }

    /** K11: jualan yang dibuka selalu ada dan paling atas, meski sudah habis. */
    private void susunMenu(List<ListingDto> isi) {
        ListingDetailDto d = detail.getValue();
        if (d != null) {
            boolean ada = false;
            for (int i = 0; i < isi.size(); i++) {
                if (isi.get(i).id == d.id) {
                    isi.add(0, isi.remove(i));
                    ada = true;
                    break;
                }
            }
            if (!ada) {
                isi.add(0, d);
            }
        }
        for (ListingDto l : isi) {
            if (d == null || l.id != d.id) {
                keranjang.daftarkan(l.id, l.priceRupiah, l.qtyRemaining);
            }
        }
        keranjangBerubah.setValue(keranjang.totalQty());
    }

    private void muatFavorit(long storeId) {
        if (!app().sesi().sudahMasuk()) {
            return;
        }
        app().api()
                .favorit()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<FavoritDto>> data) {
                                boolean ada = false;
                                if (data != null && data.data != null) {
                                    for (FavoritDto f : data.data) {
                                        ada |= f.id == storeId;
                                    }
                                }
                                favorit.setValue(ada);
                            }

                            @Override
                            public void gagal(ApiError e) {}
                        });
    }

    /** Ketuk hati: langsung berubah, dikembalikan kalau server menolak. */
    public void ubahFavorit() {
        ListingDetailDto d = detail.getValue();
        if (d == null || d.store == null) {
            return;
        }
        boolean jadi = !Boolean.TRUE.equals(favorit.getValue());
        favorit.setValue(jadi);
        ApiCallback<Void> balik =
                new ApiCallback<>() {
                    @Override
                    public void sukses(Void data) {}

                    @Override
                    public void gagal(ApiError e) {
                        favorit.setValue(!jadi);
                        if (e.perluMasukUlang()) {
                            sesiBerakhir.setValue(new Peristiwa<>(true));
                        } else {
                            galat.setValue(new Peristiwa<>(e.pesan()));
                        }
                    }
                };
        if (jadi) {
            app().api().tambahFavorit(new FavoritDto.Body(d.store.id)).enqueue(balik);
        } else {
            app().api().hapusFavorit(d.store.id).enqueue(balik);
        }
    }

    public void tambah(long id) {
        if (keranjang.tambah(id)) {
            keranjangBerubah.setValue(keranjang.totalQty());
        }
    }

    public void kurang(long id) {
        // K10 tidak bisa di bawah 1 tas; K11 boleh 0 per item.
        if (keranjang.kurang(id, menuSatuan ? 0 : 1)) {
            keranjangBerubah.setValue(keranjang.totalQty());
        }
    }

    public int minimal() {
        return menuSatuan ? 0 : 1;
    }

    private void hitungJarak() {
        ListingDetailDto d = detail.getValue();
        if (d == null || d.store == null) {
            return;
        }
        jarakKm.setValue(
                FormatTampilan.jarakKm(latSaya, lngSaya, d.store.latitude, d.store.longitude));
    }

    private LofApp app() {
        return getApplication();
    }
}
