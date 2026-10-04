package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TautanPetaTest {

    @Test
    public void pinTokoDenganLabel() {
        assertEquals(
                "geo:-6.2297,106.8583?q=-6.2297,106.8583(Roti%20Sari%20Bakery)",
                TautanPeta.toko(-6.2297, 106.8583, "Roti Sari Bakery", null));
    }

    @Test
    public void tanpaKoordinatMemakaiAlamat() {
        assertEquals(
                "geo:0,0?q=Jl.%20Tebet%20Raya%20No.%2012",
                TautanPeta.toko(null, null, "Roti Sari", "Jl. Tebet Raya No. 12"));
        assertNull(TautanPeta.toko(null, null, "Roti Sari", null));
    }

    @Test
    public void sekitarPembeli() {
        assertEquals("geo:-6.2253,106.8087?z=15", TautanPeta.sekitar(-6.2253, 106.8087, "SCBD"));
        assertEquals(
                "geo:0,0?q=SCBD%2C%20Jakarta", TautanPeta.sekitar(null, null, "SCBD, Jakarta"));
        assertNull(TautanPeta.sekitar(null, null, null));
    }
}
