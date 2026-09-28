package id.lifeoffoods.ui.pesanan;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.Kandungan;
import id.lifeoffoods.data.Keranjang;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * K12 dan K13 Ringkasan pesanan. Total dan peringatan alergi selalu dari POST /orders/preview,
 * bukan dihitung di HP, supaya angka yang dilihat pembeli sama dengan yang disimpan server. Di K13
 * jumlah bisa diubah; tiap perubahan meminta pratinjau ulang.
 */
public class RingkasanViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    /** Satu baris item. Judul dan harga disimpan supaya baris berjumlah 0 tetap tampil. */
    public static final class Baris {
        public final long id;
        public final String judul;
        public final long harga;

        /** Harga normal untuk dicoret; 0 kalau tidak ada atau tidak lebih mahal. */
        public final long hargaNormal;

        public final int qty;

        Baris(long id, String judul, long harga, long hargaNormal, int qty) {
            this.id = id;
            this.judul = judul;
            this.harga = harga;
            this.hargaNormal = hargaNormal > harga ? hargaNormal : 0;
            this.qty = qty;
        }
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<PesananDto> pratinjau = new MutableLiveData<>();
    public final MutableLiveData<List<Baris>> baris = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> peringatan = new MutableLiveData<>();
    public final MutableLiveData<String> pesanGagal = new MutableLiveData<>();
    public final MutableLiveData<Boolean> mengirim = new MutableLiveData<>(false);
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Long>> dibuat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    public final Keranjang keranjang = new Keranjang();

    private final Map<Long, String> judul = new LinkedHashMap<>();
    private final Map<Long, Long> harga = new LinkedHashMap<>();
    private final Map<Long, Long> hargaNormal = new LinkedHashMap<>();
    private String bayar = PesananBody.BAYAR_TUNAI;
    private boolean sudahMulai;
    private int permintaan;

    public RingkasanViewModel(@NonNull Application app) {
        super(app);
    }

    /** Isi dari K10/K11. Stok sebenarnya diperiksa server; di sini cukup batas 5 per item. */
    public void mulai(@Nullable long[] id, @Nullable int[] qty) {
        if (sudahMulai) {
            return;
        }
        sudahMulai = true;
        if (id != null && qty != null) {
            for (int i = 0; i < Math.min(id.length, qty.length); i++) {
                keranjang.daftarkan(id[i], 0, Keranjang.MAKS_PER_ITEM);
                keranjang.atur(id[i], qty[i]);
            }
        }
        muatPratinjau();
    }

    public void muatPratinjau() {
        Map<Long, Integer> terpilih = keranjang.terpilih();
        if (terpilih.isEmpty()) {
            susunBaris();
            status.setValue(Status.SIAP);
            pratinjau.setValue(null);
            return;
        }
        int ke = ++permintaan;
        if (pratinjau.getValue() == null) {
            status.setValue(Status.MEMUAT);
        }
        app().api()
                .pratinjauPesanan(PesananBody.dari(terpilih))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananDto> data) {
                                if (ke != permintaan) {
                                    return; // Jawaban lama; jumlah sudah berubah lagi.
                                }
                                PesananDto p = data == null ? null : data.data;
                                if (p == null) {
                                    status.setValue(Status.GAGAL);
                                    return;
                                }
                                if (p.items != null) {
                                    for (PesananDto.Item it : p.items) {
                                        judul.put(it.listingId, it.title);
                                        harga.put(it.listingId, it.unitPriceRupiah);
                                        if (it.originalValueRupiah != null) {
                                            hargaNormal.put(it.listingId, it.originalValueRupiah);
                                        }
                                    }
                                }
                                pesanGagal.setValue(null);
                                pratinjau.setValue(p);
                                peringatan.setValue(teksPeringatan(p));
                                susunBaris();
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (ke != permintaan) {
                                    return;
                                }
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                if (e.kode() == 422) {
                                    // Stok habis, beda toko, atau batas jumlah: tampilkan alasan
                                    // server dan matikan tombol pesan sampai jumlah diubah.
                                    pesanGagal.setValue(e.pesan());
                                    susunBaris();
                                    status.setValue(
                                            pratinjau.getValue() == null
                                                    ? Status.GAGAL
                                                    : Status.SIAP);
                                    return;
                                }
                                status.setValue(Status.GAGAL);
                                pesanGagal.setValue(e.pesan());
                            }
                        });
    }

    public void tambah(long id) {
        if (keranjang.tambah(id)) {
            muatPratinjau();
        }
    }

    public void kurang(long id) {
        if (keranjang.kurang(id, 0)) {
            muatPratinjau();
        }
    }

    public void pilihBayar(String cara) {
        bayar = cara;
    }

    public String caraBayar() {
        return bayar;
    }

    /** Tombol "Buat pesanan". Dimatikan selama terkirim supaya tidak ada pesanan dobel. */
    public void buat(@Nullable String catatan) {
        if (Boolean.TRUE.equals(mengirim.getValue()) || keranjang.terpilih().isEmpty()) {
            return;
        }
        PesananBody body = PesananBody.dari(keranjang.terpilih());
        String c = catatan == null ? "" : catatan.trim();
        body.note = c.isEmpty() ? null : c;
        body.paymentMethod = bayar;
        mengirim.setValue(true);
        app().api()
                .buatPesanan(body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananDto> data) {
                                mengirim.setValue(false);
                                if (data == null || data.data == null) {
                                    galat.setValue(
                                            new Peristiwa<>(
                                                    app().getString(R.string.k12_gagal_buat)));
                                    return;
                                }
                                dibuat.setValue(new Peristiwa<>(data.data.id));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                mengirim.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                galat.setValue(new Peristiwa<>(e.pesan()));
                                // Stok bisa sudah berubah; minta total terbaru.
                                muatPratinjau();
                            }
                        });
    }

    private void susunBaris() {
        List<Baris> isi = new ArrayList<>();
        for (Map.Entry<Long, String> e : judul.entrySet()) {
            long id = e.getKey();
            Long h = harga.get(id);
            Long n = hargaNormal.get(id);
            isi.add(
                    new Baris(
                            id,
                            e.getValue(),
                            h == null ? 0 : h,
                            n == null ? 0 : n,
                            keranjang.qty(id)));
        }
        baris.setValue(isi);
    }

    @Nullable
    private static String teksPeringatan(PesananDto p) {
        if (p.allergenWarnings == null || p.allergenWarnings.isEmpty()) {
            return null;
        }
        // Server sudah menyaring ke alergi di profil pembeli; gabungkan per alergen.
        List<ListingDto.Alergen> daftar = new ArrayList<>();
        Set<String> kode = new HashSet<>();
        for (PesananDto.PeringatanAlergen w : p.allergenWarnings) {
            if (kode.add(w.code + "/" + w.presence)) {
                ListingDto.Alergen a = new ListingDto.Alergen();
                a.code = w.code;
                a.name = w.name;
                a.presence = w.presence;
                daftar.add(a);
            }
        }
        Set<String> semua = new HashSet<>();
        for (ListingDto.Alergen a : daftar) {
            semua.add(a.code);
        }
        return Kandungan.peringatan(daftar, semua);
    }

    private LofApp app() {
        return getApplication();
    }
}
