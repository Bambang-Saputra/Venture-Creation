package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class FilterJualanTest {

    private static Map<String, String> profil() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("kacang_tanah", "Kacang tanah");
        return m;
    }

    @Test
    public void alergiProfilTercentangSaatDibuka() {
        FilterJualan f = FilterJualan.dariProfil(profil());
        assertTrue(f.alergiAktif());
        assertEquals(Collections.singletonList("kacang_tanah"), f.alergenDikirim());
        // Semua jenis dipilih berarti type[] tidak dikirim.
        assertNull(f.tipe());
        assertTrue(f.query(null, null).isEmpty());
    }

    @Test
    public void saklarMatiBerartiAlergenTidakDikirim() {
        FilterJualan f = FilterJualan.dariProfil(profil());
        f.sembunyikanAlergi = false;
        assertFalse(f.alergiAktif());
        assertNull(f.alergenDikirim());
        assertNull(FilterJualan.dariProfil(null).alergenDikirim());
    }

    @Test
    public void salinanTidakMengubahAsli() {
        FilterJualan asli = FilterJualan.dariProfil(profil());
        FilterJualan salinan = asli.salin();
        salinan.alergen.put("susu", "Susu dan produk susu");
        salinan.halal = true;
        assertEquals(1, asli.alergen.size());
        assertFalse(asli.halal);
        assertFalse(asli.equals(salinan));
        assertEquals(asli, asli.salin());
    }

    @Test
    public void jenisTerakhirTidakBisaDilepas() {
        FilterJualan f = new FilterJualan();
        assertTrue(f.ubahJenis(FilterJualan.TIPE_TAS, false));
        assertEquals(Collections.singletonList(FilterJualan.TIPE_MENU), f.tipe());
        assertFalse(f.ubahJenis(FilterJualan.TIPE_MENU, false));
        assertTrue(f.menu);
    }

    @Test
    public void queryJarakButuhKoordinat() {
        FilterJualan f = new FilterJualan();
        f.radiusKm = 3;
        f.halal = true;
        assertFalse(f.query(null, null).containsKey("radius_km"));
        Map<String, String> q = f.query(-6.2253, 106.8087);
        assertEquals("3", q.get("radius_km"));
        assertEquals("-6.225300", q.get("lat"));
        assertEquals("1", q.get("halal"));
    }

    @Test
    public void queryJamAmbil() {
        FilterJualan f = new FilterJualan();
        f.jam = FilterJualan.JAM_SAMPAI_20;
        assertEquals("20:00", f.query(null, null).get("pickup_until"));
        assertFalse(f.query(null, null).containsKey("pickup_from"));
        f.jam = FilterJualan.JAM_20_22;
        Map<String, String> q = f.query(null, null);
        assertEquals("20:00", q.get("pickup_from"));
        assertEquals("22:00", q.get("pickup_until"));
    }

    @Test
    public void labelDanDaftarNama() {
        assertEquals("Tanpa kacang tanah", FilterJualan.labelTanpa("Kacang tanah"));
        assertEquals("", FilterJualan.labelTanpa(null));
        FilterJualan f = FilterJualan.dariProfil(profil());
        assertEquals("kacang tanah", f.daftarNamaAlergen());
        f.alergen.put("susu", "Susu");
        assertEquals("kacang tanah dan susu", f.daftarNamaAlergen());
        f.alergen.put("telur", "Telur");
        assertEquals("kacang tanah, susu, dan telur", f.daftarNamaAlergen());
    }

    @Test
    public void jumlahTambahanDihitungDariBawaanProfil() {
        FilterJualan bawaan = FilterJualan.dariProfil(profil());
        FilterJualan f = bawaan.salin();
        assertEquals(0, f.jumlahTambahan(bawaan));
        f.halal = true;
        f.radiusKm = 1;
        f.ubahJenis(FilterJualan.TIPE_MENU, false);
        assertEquals(3, f.jumlahTambahan(bawaan));
        f.alergen.put("susu", "Susu");
        assertEquals(4, f.jumlahTambahan(bawaan));
        assertEquals(Arrays.asList(1, 3, 5), FilterJualan.PILIHAN_JARAK);
    }
}
