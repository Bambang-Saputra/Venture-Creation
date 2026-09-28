package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeranjangTest {

    @Test
    public void batasStokDanLimaPerItem() {
        Keranjang k = new Keranjang();
        k.daftarkan(1, 18000, 2);
        k.daftarkan(2, 9000, 20);
        assertTrue(k.tambah(1));
        assertTrue(k.tambah(1));
        assertFalse("stok cuma 2", k.tambah(1));
        for (int i = 0; i < 7; i++) {
            k.tambah(2);
        }
        assertEquals("maks 5 per item", 5, k.qty(2));
        assertEquals(7, k.totalQty());
        assertEquals(2 * 18000 + 5 * 9000, k.totalRupiah());
    }

    @Test
    public void batasSepuluhItemPerPesanan() {
        Keranjang k = new Keranjang();
        for (long id = 1; id <= 3; id++) {
            k.daftarkan(id, 1000, 9);
            k.atur(id, 5);
        }
        assertEquals(10, k.totalQty());
        assertEquals(0, k.qty(3));
        assertFalse(k.bisaTambah(3));
    }

    @Test
    public void kurangTidakDiBawahMinimal() {
        Keranjang k = new Keranjang();
        k.daftarkan(1, 1000, 3);
        k.atur(1, 1);
        assertFalse("K10 minimal 1 tas", k.kurang(1, 1));
        assertTrue(k.kurang(1, 0));
        assertEquals(0, k.totalQty());
        assertTrue(k.terpilih().isEmpty());
    }

    @Test
    public void daftarUlangMempertahankanJumlahDalamBatasBaru() {
        Keranjang k = new Keranjang();
        k.daftarkan(1, 1000, 5);
        k.atur(1, 4);
        k.daftarkan(1, 1000, 2);
        assertEquals(2, k.qty(1));
    }
}
