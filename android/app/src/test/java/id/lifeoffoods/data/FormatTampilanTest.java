package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FormatTampilanTest {

    @Test
    public void rupiahMemakaiTitikRibuan() {
        assertEquals("Rp18.000", FormatTampilan.rupiah(18000));
        assertEquals("Rp1.250.000", FormatTampilan.rupiah(1250000));
        assertEquals("Rp500", FormatTampilan.rupiah(500));
        assertEquals("Rp0", FormatTampilan.rupiah(0));
    }

    @Test
    public void jarakMeterDanKilometer() {
        assertEquals("380 m", FormatTampilan.jarak(0.38));
        assertEquals("10 m", FormatTampilan.jarak(0.001));
        assertEquals("1 km", FormatTampilan.jarak(0.998));
        assertEquals("1,2 km", FormatTampilan.jarak(1.24));
        assertNull(FormatTampilan.jarak(null));
    }

    @Test
    public void sisaWaktuSepertiFigma() {
        assertEquals("48 menit", FormatTampilan.sisaWaktu(48));
        assertEquals("1 jam 12 m", FormatTampilan.sisaWaktu(72));
        assertEquals("2 jam", FormatTampilan.sisaWaktu(120));
        assertEquals("0 menit", FormatTampilan.sisaWaktu(-3));
    }

    @Test
    public void rentangJamMengikutiZonaServer() {
        assertEquals(
                "20.30 sampai 21.00",
                FormatTampilan.rentangJam(
                        "2026-09-18T20:30:00+07:00", "2026-09-18T21:00:00+07:00"));
        assertEquals("", FormatTampilan.jam("bukan-tanggal"));
    }

    @Test
    public void jamTokoDanPersenHemat() {
        assertEquals("21.00", FormatTampilan.jamToko("21:00:00"));
        assertEquals("", FormatTampilan.jamToko(null));
        assertEquals(67, FormatTampilan.persenHemat(18000, 55000L));
        assertEquals(0, FormatTampilan.persenHemat(18000, null));
        assertEquals(0, FormatTampilan.persenHemat(20000, 18000L));
    }

    @Test
    public void jarakHaversineSekitarSatuKm() {
        // SCBD ke Senayan kira-kira 1,6 km garis lurus.
        Double km = FormatTampilan.jarakKm(-6.2253, 106.8087, -6.2272, 106.7947);
        assertEquals(1.56, km, 0.05);
        assertNull(FormatTampilan.jarakKm(null, 106.8, -6.2, 106.8));
    }

    @Test
    public void inisialDuaHurufPertama() {
        assertEquals("DR", FormatTampilan.inisial("Dara Renata"));
        assertEquals("DR", FormatTampilan.inisial("  dara   renata putri "));
        assertEquals("D", FormatTampilan.inisial("Dara"));
        assertEquals("", FormatTampilan.inisial(null));
    }
}
