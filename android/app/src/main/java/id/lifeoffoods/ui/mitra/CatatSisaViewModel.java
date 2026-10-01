package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.CatatSisa;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.CatatSisaBody;
import id.lifeoffoods.data.api.model.CatatanSisaDto;
import id.lifeoffoods.data.api.model.RingkasanTokoDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * M06 Catat sisa (per item) dan M19 Catat sisa timbang (PRD-12): satu layar dengan segmen mode.
 * Isian disimpan di sini supaya berpindah mode atau memutar layar tidak menghapus ketikan.
 */
public class CatatSisaViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    /** Item di luar daftar produk ("Roti tawar sisa"). */
    public static final class ItemLain {
        public final String label;
        public final int jumlah;
        public final int gram;
        public final long nilaiSatuan;

        public ItemLain(String label, int jumlah, int gram, long nilaiSatuan) {
            this.label = label;
            this.jumlah = jumlah;
            this.gram = gram;
            this.nilaiSatuan = nilaiSatuan;
        }
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<String> mode = new MutableLiveData<>(CatatanSisaDto.PER_ITEM);
    public final MutableLiveData<CatatanSisaDto> catatan = new MutableLiveData<>();
    public final MutableLiveData<List<ItemLain>> itemLain =
            new MutableLiveData<>(Collections.emptyList());

    /** Memicu gambar ulang ringkasan dan baris saat angka berubah. */
    public final MutableLiveData<Long> versi = new MutableLiveData<>(0L);

    public final MutableLiveData<Boolean> menyimpan = new MutableLiveData<>(false);

    public final MutableLiveData<String> galatAwal = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();

    /** true = lanjut ke M09 setelah tersimpan. */
    public final MutableLiveData<Peristiwa<Boolean>> tersimpan = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    /** "21:00" dari /summary untuk pil di header; null = pil disembunyikan. */
    public final MutableLiveData<String> jamTutup = new MutableLiveData<>();

    /**
     * Pilihan tujuan per produk disembunyikan sampai mitra mengetuk "Atur tujuan", supaya layar
     * tetap ringkas seperti Figma. Selalu tampil kalau ada produk yang tujuannya bukan "dibuang".
     */
    public final MutableLiveData<Boolean> tampilTujuan = new MutableLiveData<>(false);

    private final Map<Long, Integer> jumlah = new HashMap<>();
    private final Map<Long, Integer> gram = new HashMap<>();
    private final Map<Long, String> tujuan = new HashMap<>();
    private String tanggal;
    private boolean sudahMuat;

    public CatatSisaViewModel(@NonNull Application app) {
        super(app);
    }

    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        status.setValue(Status.MEMUAT);
        tanggal = LocalDate.now(PesananMasukViewModel.WIB).toString();
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        muatJamTutup(app, idToko);
                        app.api()
                                .catatanSisa(idToko, tanggal)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<CatatanSisaDto> data) {
                                                if (data == null || data.data == null) {
                                                    gagalMuat(ApiError.jaringan());
                                                    return;
                                                }
                                                terima(data.data);
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

    /** Pil "Tutup 21.00" hanya pelengkap: kalau gagal, pil tidak tampil dan layar tetap jalan. */
    private void muatJamTutup(LofApp app, long idToko) {
        app.api()
                .ringkasanToko(idToko)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<RingkasanTokoDto> data) {
                                jamTutup.setValue(
                                        data == null || data.data == null
                                                ? null
                                                : data.data.closesAt);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                jamTutup.setValue(null);
                            }
                        });
    }

    public void aturTujuan(boolean tampil) {
        tampilTujuan.setValue(tampil || adaTujuanLain());
        berubah();
    }

    /** Ada produk yang disumbangkan, dimakan karyawan, atau terjual sebagai surplus. */
    public boolean adaTujuanLain() {
        for (String t : tujuan.values()) {
            if (!CatatSisa.DIBUANG.equals(t)) {
                return true;
            }
        }
        return false;
    }

    private void gagalMuat(ApiError e) {
        sudahMuat = false;
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        galatAwal.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }

    /** Isi awal dari server: catatan hari ini kalau sudah ada, atau semua produk dengan 0. */
    private void terima(CatatanSisaDto c) {
        jumlah.clear();
        gram.clear();
        tujuan.clear();
        if (c.products != null) {
            for (CatatanSisaDto.Produk p : c.products) {
                jumlah.put(p.productId, p.qty);
                if (p.weightGram != null) {
                    gram.put(p.productId, p.weightGram);
                }
                tujuan.put(p.productId, p.disposition == null ? CatatSisa.DIBUANG : p.disposition);
            }
        }
        List<ItemLain> lain = new ArrayList<>();
        if (c.otherItems != null) {
            for (CatatanSisaDto.ItemLain i : c.otherItems) {
                int q = i.qty == null ? 0 : i.qty;
                long satuan = q > 0 ? i.valueRupiah / q : 0;
                lain.add(new ItemLain(i.label, q, i.weightGram == null ? 0 : i.weightGram, satuan));
            }
        }
        itemLain.setValue(lain);
        if (c.isRecorded && c.method != null) {
            mode.setValue(c.method);
        }
        if (adaTujuanLain()) {
            tampilTujuan.setValue(true);
        }
        catatan.setValue(c);
        berubah();
    }

    public boolean terkunci() {
        CatatanSisaDto c = catatan.getValue();
        return c != null && c.isLocked;
    }

    public void pilihMode(String m) {
        mode.setValue(m);
        berubah();
    }

    public boolean timbang() {
        return CatatanSisaDto.TIMBANG.equals(mode.getValue());
    }

    public int jumlah(long id) {
        Integer j = jumlah.get(id);
        return j == null ? 0 : j;
    }

    public int gram(long id) {
        Integer g = gram.get(id);
        return g == null ? 0 : g;
    }

    public String tujuan(long id) {
        String t = tujuan.get(id);
        return t == null ? CatatSisa.DIBUANG : t;
    }

    public void ubahJumlah(long id, boolean tambah) {
        jumlah.put(
                id, Math.max(0, Math.min(CatatSisa.JUMLAH_MAKS, jumlah(id) + (tambah ? 1 : -1))));
        berubah();
    }

    /** Dari kolom berat; -1 (tidak terbaca) disimpan sebagai 0 dan ditandai di kolom. */
    public void ubahGram(long id, int g) {
        gram.put(id, Math.max(0, g));
        berubah();
    }

    public void ubahTujuan(long id, String t) {
        tujuan.put(id, t);
        berubah();
    }

    public void tambahItemLain(ItemLain i) {
        List<ItemLain> d = new ArrayList<>(itemLain.getValue());
        d.add(i);
        itemLain.setValue(d);
        berubah();
    }

    public void hapusItemLain(int posisi) {
        List<ItemLain> d = new ArrayList<>(itemLain.getValue());
        if (posisi >= 0 && posisi < d.size()) {
            d.remove(posisi);
            itemLain.setValue(d);
            berubah();
        }
    }

    /** Nilai terbuang saat mengetik (mode per item, PRD-12 kriteria 9). */
    public long nilaiTerbuang() {
        return CatatSisa.nilaiTerbuang(barisHitung());
    }

    public int itemTerbuang() {
        return CatatSisa.itemTerbuang(barisHitung());
    }

    /** Berat terbuang yang diketik (mode timbang). */
    public long gramTerbuang() {
        long total = 0;
        CatatanSisaDto c = catatan.getValue();
        if (c != null && c.products != null) {
            for (CatatanSisaDto.Produk p : c.products) {
                if (CatatSisa.terbuang(tujuan(p.productId))) {
                    total += gram(p.productId);
                }
            }
        }
        for (ItemLain i : itemLain.getValue()) {
            total += i.gram;
        }
        return total;
    }

    private List<CatatSisa.Baris> barisHitung() {
        List<CatatSisa.Baris> b = new ArrayList<>();
        CatatanSisaDto c = catatan.getValue();
        if (c != null && c.products != null) {
            for (CatatanSisaDto.Produk p : c.products) {
                b.add(
                        new CatatSisa.Baris(
                                p.unitValueRupiah, jumlah(p.productId), tujuan(p.productId)));
            }
        }
        for (ItemLain i : itemLain.getValue()) {
            b.add(new CatatSisa.Baris(i.nilaiSatuan, i.jumlah, CatatSisa.DIBUANG));
        }
        return b;
    }

    public void simpan(boolean lanjutPasang) {
        if (Boolean.TRUE.equals(menyimpan.getValue()) || terkunci()) {
            return;
        }
        CatatanSisaDto c = catatan.getValue();
        if (c == null) {
            return;
        }
        boolean berat = timbang();
        List<CatatSisaBody.Item> items = new ArrayList<>();
        if (c.products != null) {
            for (CatatanSisaDto.Produk p : c.products) {
                CatatSisaBody.Item i = new CatatSisaBody.Item();
                i.productId = p.productId;
                if (berat) {
                    i.weightGram = gram(p.productId);
                } else {
                    i.qty = jumlah(p.productId);
                }
                i.disposition = tujuan(p.productId);
                items.add(i);
            }
        }
        for (ItemLain l : itemLain.getValue()) {
            CatatSisaBody.Item i = new CatatSisaBody.Item();
            i.label = l.label;
            if (berat) {
                i.weightGram = l.gram;
            } else {
                i.qty = l.jumlah;
            }
            i.unitValueRupiah = l.nilaiSatuan;
            i.disposition = CatatSisa.DIBUANG;
            items.add(i);
        }
        CatatSisaBody body =
                new CatatSisaBody(
                        tanggal, berat ? CatatanSisaDto.TIMBANG : CatatanSisaDto.PER_ITEM, items);
        body.note = c.note;
        menyimpan.setValue(true);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        app.api()
                                .simpanSisa(idToko, body)
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<CatatanSisaDto> data) {
                                                menyimpan.setValue(false);
                                                if (data != null && data.data != null) {
                                                    terima(data.data);
                                                }
                                                tersimpan.setValue(new Peristiwa<>(lanjutPasang));
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                gagalSimpan(e);
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        gagalSimpan(e);
                    }
                });
    }

    private void gagalSimpan(ApiError e) {
        menyimpan.setValue(false);
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        galat.setValue(new Peristiwa<>(e.pesan()));
        if (e.kode() == 409) {
            // Terkunci sejak layar dibuka: muat ulang supaya tampilan jadi baca saja.
            muatUlang();
        }
    }

    private void berubah() {
        Long v = versi.getValue();
        versi.setValue(v == null ? 1 : v + 1);
    }

    @Nullable
    public String tanggal() {
        return tanggal;
    }
}
