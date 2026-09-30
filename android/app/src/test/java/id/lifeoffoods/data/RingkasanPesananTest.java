package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PratinjauPesananDto;
import java.util.List;
import org.junit.Test;

public class RingkasanPesananTest {

    private static PratinjauPesananDto pratinjau(String json) {
        return new Gson().fromJson(json, PratinjauPesananDto.class);
    }

    @Test
    public void peringatanMemakaiJudulJualanDanTandaMungkin() {
        PratinjauPesananDto p =
                pratinjau(
                        "{\"items\":[{\"listing_id\":31,\"title\":\"Tas Pastry Sore\",\"qty\":1}],"
                                + "\"allergen_warnings\":[{\"listing_id\":31,\"code\":\"susu\","
                                + "\"name\":\"Susu\",\"presence\":\"may_contain\","
                                + "\"severity\":\"severe\"}]}");
        List<RingkasanPesanan.Peringatan> w = RingkasanPesanan.peringatan(p);
        assertEquals(1, w.size());
        assertEquals("Tas Pastry Sore", w.get(0).judul);
        assertEquals("susu", w.get(0).alergen);
        assertTrue(w.get(0).mungkin);
        assertTrue(w.get(0).berat);
    }

    @Test
    public void peringatanGandaHanyaSekali() {
        PratinjauPesananDto p =
                pratinjau(
                        "{\"items\":[{\"listing_id\":41,\"title\":\"Croissant\",\"qty\":1}],"
                                + "\"allergen_warnings\":["
                                + "{\"listing_id\":41,\"code\":\"susu\",\"name\":\"Susu\","
                                + "\"presence\":\"contains\",\"severity\":\"avoid\"},"
                                + "{\"listing_id\":41,\"code\":\"susu\",\"name\":\"Susu\","
                                + "\"presence\":\"contains\",\"severity\":\"avoid\"}]}");
        List<RingkasanPesanan.Peringatan> w = RingkasanPesanan.peringatan(p);
        assertEquals(1, w.size());
        assertFalse(w.get(0).mungkin);
        assertFalse(w.get(0).berat);
    }

    @Test
    public void tanpaPeringatanHasilnyaKosong() {
        assertTrue(RingkasanPesanan.peringatan(null).isEmpty());
        assertTrue(RingkasanPesanan.peringatan(pratinjau("{\"items\":[]}")).isEmpty());
    }

    @Test
    public void batasTigaPesananDikenaliDariPesan() {
        ApiError e =
                ApiError.dariRespons(
                        422,
                        "{\"message\":\"Kamu masih punya 3 pesanan yang belum diambil.\","
                                + "\"errors\":{\"items\":[\"Kamu masih punya 3 pesanan yang"
                                + " belum diambil. Ambil dulu sebelum memesan lagi.\"]}}");
        assertTrue(RingkasanPesanan.batasPesananAktif(e));
    }

    @Test
    public void stokHabisBukanBatasPesanan() {
        ApiError e =
                ApiError.dariRespons(
                        422,
                        "{\"message\":\"Tas Pastry Sore sudah habis.\","
                                + "\"errors\":{\"items\":[\"Tas Pastry Sore sudah habis.\"]}}");
        assertFalse(RingkasanPesanan.batasPesananAktif(e));
        assertEquals("Tas Pastry Sore sudah habis.", RingkasanPesanan.pesanItems(e));
    }

    @Test
    public void pesanUmumKalauTidakAdaGalatItems() {
        ApiError e = ApiError.dariRespons(422, "{\"message\":\"Data tidak valid.\"}");
        assertEquals("Data tidak valid.", RingkasanPesanan.pesanItems(e));
    }
}
