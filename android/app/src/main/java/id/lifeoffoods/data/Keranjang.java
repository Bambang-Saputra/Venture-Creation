package id.lifeoffoods.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jumlah yang dipilih di K10 dan K11 sebelum ringkasan pesanan. Batasnya sama dengan server
 * (kontrak API POST /orders/preview): maksimal 5 per item, 10 item per pesanan, dan tidak melebihi
 * stok.
 */
public final class Keranjang {

    public static final int MAKS_PER_ITEM = 5;
    public static final int MAKS_TOTAL = 10;

    private static final class Baris {
        final long harga;
        final int stok;
        int qty;

        Baris(long harga, int stok) {
            this.harga = harga;
            this.stok = stok;
        }
    }

    private final Map<Long, Baris> baris = new LinkedHashMap<>();

    /** Daftarkan jualan yang bisa dipilih. Jumlah yang sudah ada dipertahankan. */
    public void daftarkan(long id, long harga, int stok) {
        Baris lama = baris.get(id);
        Baris baru = new Baris(harga, Math.max(0, stok));
        if (lama != null) {
            baru.qty = Math.min(lama.qty, batasItem(baru));
        }
        baris.put(id, baru);
    }

    public int qty(long id) {
        Baris b = baris.get(id);
        return b == null ? 0 : b.qty;
    }

    public boolean bisaTambah(long id) {
        Baris b = baris.get(id);
        return b != null && b.qty < batasItem(b) && totalQty() < MAKS_TOTAL;
    }

    public boolean bisaKurang(long id, int minimal) {
        Baris b = baris.get(id);
        return b != null && b.qty > minimal;
    }

    public boolean tambah(long id) {
        if (!bisaTambah(id)) {
            return false;
        }
        baris.get(id).qty++;
        return true;
    }

    public boolean kurang(long id, int minimal) {
        if (!bisaKurang(id, minimal)) {
            return false;
        }
        baris.get(id).qty--;
        return true;
    }

    /** Isi awal, dipotong ke batas yang berlaku. */
    public void atur(long id, int qty) {
        Baris b = baris.get(id);
        if (b == null) {
            return;
        }
        int sisaTotal = MAKS_TOTAL - (totalQty() - b.qty);
        b.qty = Math.max(0, Math.min(qty, Math.min(batasItem(b), sisaTotal)));
    }

    public int totalQty() {
        int n = 0;
        for (Baris b : baris.values()) {
            n += b.qty;
        }
        return n;
    }

    public long totalRupiah() {
        long t = 0;
        for (Baris b : baris.values()) {
            t += b.harga * b.qty;
        }
        return t;
    }

    /** Jualan yang dipilih (qty > 0), urut sesuai pendaftaran. Untuk dikirim ke K12/K13. */
    public Map<Long, Integer> terpilih() {
        Map<Long, Integer> hasil = new LinkedHashMap<>();
        for (Map.Entry<Long, Baris> e : baris.entrySet()) {
            if (e.getValue().qty > 0) {
                hasil.put(e.getKey(), e.getValue().qty);
            }
        }
        return hasil;
    }

    private static int batasItem(Baris b) {
        return Math.min(MAKS_PER_ITEM, b.stok);
    }
}
