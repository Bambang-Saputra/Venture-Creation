package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CatatSisaTest {

    @Test
    public void hanyaYangDibuangDihitungPemborosan() {
        List<CatatSisa.Baris> b =
                Arrays.asList(
                        new CatatSisa.Baris(7000, 6, "discarded"),
                        new CatatSisa.Baris(6500, 4, null),
                        new CatatSisa.Baris(8000, 2, "donated"),
                        new CatatSisa.Baris(5000, 0, "discarded"));
        assertEquals(7000 * 6 + 6500 * 4, CatatSisa.nilaiTerbuang(b));
        assertEquals(10, CatatSisa.itemTerbuang(b));
        assertTrue(CatatSisa.terbuang(null));
        assertFalse(CatatSisa.terbuang("staff_meal"));
    }

    @Test
    public void beratKgDibacaDenganKomaAtauTitik() {
        assertEquals(1200, CatatSisa.gram("1,2"));
        assertEquals(800, CatatSisa.gram("0.8"));
        assertEquals(2000, CatatSisa.gram(" 2 "));
        assertEquals(0, CatatSisa.gram(""));
        assertEquals(-1, CatatSisa.gram("dua"));
        assertEquals(-1, CatatSisa.gram("-1"));
        assertEquals(-1, CatatSisa.gram("1001"));
    }

    @Test
    public void gramDitulisSebagaiKg() {
        assertEquals("1,2 kg", CatatSisa.kg(1200));
        assertEquals("0,85 kg", CatatSisa.kg(850));
        assertEquals("3 kg", CatatSisa.kg(3000));
        assertEquals("0 kg", CatatSisa.kg(0));
        assertEquals("1 kg", CatatSisa.kg(1001));
        assertEquals("1,2", CatatSisa.isianKg(1200));
        assertEquals("", CatatSisa.isianKg(null));
    }
}
