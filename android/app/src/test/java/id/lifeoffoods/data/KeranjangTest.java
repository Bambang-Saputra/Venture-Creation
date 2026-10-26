package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeranjangTest {

    @Test
    public void hanyaDibatasiStok() {
        Keranjang k = new Keranjang();
        k.daftarkan(1, 18000, 2);
        k.daftarkan(2, 9000, 20);
        assertTrue(k.tambah(1));
        assertTrue(k.tambah(1));
        assertFalse("stok cuma 2", k.tambah(1));
        for (int i = 0; i < 25; i++) {
            k.tambah(2);
        }
        assertEquals("tidak ada batas per item selain stok", 20, k.qty(2));
        assertEquals(22, k.totalQty());
        assertEquals(2 * 18000 + 20 * 9000, k.totalRupiah());
    }

    @Test
    public void tidakAdaBatasJumlahItemPerPesanan() {
        Keranjang k = new Keranjang();
        for (long id = 1; id <= 3; id++) {
            k.daftarkan(id, 1000, 9);
            k.atur(id, 9);
        }
        assertEquals(27, k.totalQty());
        assertFalse("stok item 3 sudah habis dipilih", k.bisaTambah(3));
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
