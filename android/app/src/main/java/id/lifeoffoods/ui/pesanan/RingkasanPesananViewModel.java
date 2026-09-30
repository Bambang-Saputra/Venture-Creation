package id.lifeoffoods.ui.pesanan;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.Keranjang;
import id.lifeoffoods.data.RingkasanPesanan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.PratinjauPesananDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * K12 Ringkasan tas kejutan dan K13 Ringkasan menu satuan (PRD-06). Pratinjau dihitung server (POST
 * /orders/preview) setiap kali isi berubah; aplikasi tidak menghitung ulang harga.
 */
public class RingkasanPesananViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<PratinjauPesananDto> pratinjau = new MutableLiveData<>();

    /** true selama POST /orders berjalan; tombol "Buat pesanan" mati (PRD-06 kriteria 11). */
    public final MutableLiveData<Boolean> membuat = new MutableLiveData<>(false);

    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();

    /** Stok habis atau jualan tak tersedia: kembali ke K10/K11 yang dimuat ulang, dengan pesan. */
    public final MutableLiveData<Peristiwa<String>> keDetail = new MutableLiveData<>();

    /** Batas 3 pesanan aktif: ke K15 supaya pesanan lama bisa diambil atau dibatalkan. */
    public final MutableLiveData<Peristiwa<String>> kePesananSaya = new MutableLiveData<>();

    /** Pesanan dibuat: id untuk K14. */
    public final MutableLiveData<Peristiwa<Long>> selesai = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    /** Jumlah per jualan; di K13 bisa diubah dengan stepper. */
    public final Keranjang keranjang = new Keranjang();

    private boolean sudahMulai;
    private boolean menuSatuan;
    private final List<Long> urutan = new ArrayList<>();

    /** Jumlah sebelum perubahan stepper terakhir, dikembalikan kalau server menolaknya. */
    @Nullable private long[] ubahanTerakhir;

    public RingkasanPesananViewModel(@NonNull Application app) {
        super(app);
    }

    /**
     * @param stok sisa stok per jualan dari K10/K11, boleh null; server tetap memeriksa.
     */
    public void mulai(long[] id, int[] qty, @Nullable int[] stok, boolean menuSatuan) {
        if (sudahMulai) {
            return;
        }
        sudahMulai = true;
        this.menuSatuan = menuSatuan;
        for (int i = 0; i < id.length; i++) {
            int s = stok != null && i < stok.length ? stok[i] : Keranjang.MAKS_PER_ITEM;
            // Harga di keranjang tidak dipakai untuk tampilan; total selalu dari pratinjau.
            keranjang.daftarkan(id[i], 0, Math.max(s, qty[i]));
            keranjang.atur(id[i], qty[i]);
            urutan.add(id[i]);
        }
        muatUlang();
    }

    public boolean menuSatuan() {
        return menuSatuan;
    }

    public void muatUlang() {
        status.setValue(Status.MEMUAT);
        minta(true);
    }

    /** K13: ubah jumlah satu item (minimal 1), lalu minta pratinjau baru. */
    public void ubah(long id, boolean tambah) {
        if (Boolean.TRUE.equals(membuat.getValue())) {
            return;
        }
        long[] sebelum = {id, keranjang.qty(id)};
        boolean berubah = tambah ? keranjang.tambah(id) : keranjang.kurang(id, 1);
        if (berubah) {
            ubahanTerakhir = sebelum;
            minta(false);
        }
    }

    private void minta(boolean awal) {
        app().api()
                .pratinjauPesanan(body())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PratinjauPesananDto> data) {
                                if (data == null || data.data == null) {
                                    status.setValue(Status.GAGAL);
                                    return;
                                }
                                ubahanTerakhir = null;
                                pratinjau.setValue(data.data);
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                } else if (e.kode() == 422 && awal) {
                                    // Stok sudah habis sejak detail dibuka.
                                    keDetail.setValue(
                                            new Peristiwa<>(RingkasanPesanan.pesanItems(e)));
                                } else if (e.kode() == 422) {
                                    kembalikanUbahan();
                                    galat.setValue(new Peristiwa<>(RingkasanPesanan.pesanItems(e)));
                                } else if (awal) {
                                    status.setValue(Status.GAGAL);
                                } else {
                                    kembalikanUbahan();
                                    galat.setValue(new Peristiwa<>(e.pesan()));
                                }
                            }
                        });
    }

    private void kembalikanUbahan() {
        if (ubahanTerakhir != null) {
            keranjang.atur(ubahanTerakhir[0], (int) ubahanTerakhir[1]);
            ubahanTerakhir = null;
            // Tampilkan ulang pratinjau lama supaya stepper kembali ke angka yang benar.
            pratinjau.setValue(pratinjau.getValue());
        }
    }

    /** Tombol "Buat pesanan". Ketukan kedua selama permintaan berjalan diabaikan. */
    public void buat(@Nullable String catatan, String metodeBayar) {
        if (Boolean.TRUE.equals(membuat.getValue()) || status.getValue() != Status.SIAP) {
            return;
        }
        membuat.setValue(true);
        app().api()
                .buatPesanan(body().untukDibuat(catatan, metodeBayar))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananDto> data) {
                                membuat.setValue(false);
                                if (data == null || data.data == null) {
                                    galat.setValue(new Peristiwa<>(ApiError.jaringan().pesan()));
                                    return;
                                }
                                PesananDto p = data.data;
                                app().sesi()
                                        .simpanPesanan(
                                                p.id,
                                                new Gson().toJson(p),
                                                System.currentTimeMillis());
                                selesai.setValue(new Peristiwa<>(p.id));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                membuat.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                } else if (RingkasanPesanan.batasPesananAktif(e)) {
                                    kePesananSaya.setValue(
                                            new Peristiwa<>(RingkasanPesanan.pesanItems(e)));
                                } else if (e.kode() == 422) {
                                    keDetail.setValue(
                                            new Peristiwa<>(RingkasanPesanan.pesanItems(e)));
                                } else {
                                    galat.setValue(new Peristiwa<>(e.pesan()));
                                }
                            }
                        });
    }

    private PesananBody body() {
        Map<Long, Integer> terpilih = keranjang.terpilih();
        long[] id = new long[terpilih.size()];
        int[] qty = new int[terpilih.size()];
        int i = 0;
        for (long l : urutan) {
            Integer q = terpilih.get(l);
            if (q != null) {
                id[i] = l;
                qty[i] = q;
                i++;
            }
        }
        return PesananBody.dari(id, qty);
    }

    private LofApp app() {
        return getApplication();
    }
}
