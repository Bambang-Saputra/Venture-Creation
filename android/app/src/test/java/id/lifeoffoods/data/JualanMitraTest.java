package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.JualanMitraDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class JualanMitraTest {

    @Test
    public void saklarHanyaNyalaSaatAktifDanTerkunciSaatHabisAtauLewat() {
        assertTrue(JualanMitra.saklarNyala("active"));
        assertFalse(JualanMitra.saklarNyala("paused"));
        assertTrue(JualanMitra.saklarBisaDiubah("paused"));
        assertTrue(JualanMitra.saklarBisaDiubah("draft"));
        assertFalse(JualanMitra.saklarBisaDiubah("sold_out"));
        assertFalse(JualanMitra.saklarBisaDiubah("expired"));
    }

    @Test
    public void nadaPilMengikutiStatus() {
        assertEquals(JualanMitra.Nada.HIJAU, JualanMitra.nada("active"));
        assertEquals(JualanMitra.Nada.KUNING, JualanMitra.nada("paused"));
        assertEquals(JualanMitra.Nada.KUNING, JualanMitra.nada("draft"));
        assertEquals(JualanMitra.Nada.ABU, JualanMitra.nada("sold_out"));
    }

    @Test
    public void potensiMenjumlahkanHargaKaliJumlah() {
        assertEquals(
                72000 + 27000,
                JualanMitra.potensi(Arrays.asList(new long[] {18000, 4}, new long[] {9000, 3})));
        assertEquals(0, JualanMitra.potensi(Collections.emptyList()));
    }

    @Test
    public void dapurKacangMenambahKacangSebagaiMungkinMengandung() {
        List<JualanMitraDto.Alergen> a = JualanMitra.alergen(Arrays.asList("susu", "telur"), true);
        assertEquals(3, a.size());
        assertEquals("susu", a.get(0).code);
        assertEquals("contains", a.get(0).presence);
        assertEquals("kacang_tanah", a.get(2).code);
        assertEquals("may_contain", a.get(2).presence);
    }

    @Test
    public void kacangYangDipilihTetapMengandungWalauDapurKacang() {
        List<JualanMitraDto.Alergen> a =
                JualanMitra.alergen(Collections.singletonList("kacang_tanah"), true);
        assertEquals(1, a.size());
        assertEquals("contains", a.get(0).presence);
    }

    @Test
    public void labelHalalMempertahankanSertifikat() {
        assertNull(JualanMitra.labelHalal(true, "certified"));
        assertEquals("self_claim", JualanMitra.labelHalal(true, "not_stated"));
        assertEquals("not_stated", JualanMitra.labelHalal(false, "certified"));
    }

    @Test
    public void jamDibacaDariKolomTimeDanDitulisUntukApi() {
        assertEquals(19 * 60, JualanMitra.menit("19:00:00"));
        assertEquals(20 * 60 + 30, JualanMitra.menit("20:30"));
        assertEquals(-1, JualanMitra.menit("25:00"));
        assertEquals(-1, JualanMitra.menit(null));
        assertEquals("07:05", JualanMitra.jamApi(7 * 60 + 5));
    }

    @Test
    public void tasCampurWajibJudulHargaDanKandungan() {
        JualanMitra.Periksa p =
                JualanMitra.periksaTas(true, " ", 500, 400L, "", 4, 1140, 1260, true);
        assertFalse(p.sah());
        assertEquals(Integer.valueOf(JualanMitra.G_JUDUL), p.galat.get("title"));
        assertEquals(Integer.valueOf(JualanMitra.G_HARGA), p.galat.get("price_rupiah"));
        assertEquals(
                Integer.valueOf(JualanMitra.G_HARGA_NORMAL), p.galat.get("original_value_rupiah"));
        assertEquals(Integer.valueOf(JualanMitra.G_KANDUNGAN), p.galat.get("ingredients_text"));
    }

    @Test
    public void tasDariCetakanCukupJumlahJamDanAlergen() {
        assertTrue(JualanMitra.periksaTas(false, null, 0, null, null, 4, 1140, 1260, true).sah());
        JualanMitra.Periksa p =
                JualanMitra.periksaTas(false, null, 0, null, null, 0, 1260, 1140, false);
        assertEquals(Integer.valueOf(JualanMitra.G_JUMLAH), p.galat.get("qty_total"));
        assertEquals(Integer.valueOf(JualanMitra.G_JAM), p.galat.get("pickup_end"));
        assertEquals(Integer.valueOf(JualanMitra.G_ALERGEN), p.galat.get("allergens"));
    }

    @Test
    public void menuWajibSatuItemDanAlergenTiapItem() {
        assertTrue(JualanMitra.periksaMenu(2, 1140, 1260, true).sah());
        JualanMitra.Periksa p = JualanMitra.periksaMenu(0, 1140, 1260, false);
        assertEquals(Integer.valueOf(JualanMitra.G_ITEM), p.galat.get("items"));
        assertEquals(Integer.valueOf(JualanMitra.G_ALERGEN), p.galat.get("allergens"));
    }
}
