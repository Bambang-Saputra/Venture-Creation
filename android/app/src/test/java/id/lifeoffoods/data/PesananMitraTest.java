package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.PesananMitraDto;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PesananMitraTest {

    @Test
    public void ringkasItemMenulisJudulDanJumlah() {
        assertEquals(
                "Croissant mentega × 2, Danish keju × 1",
                PesananMitra.ringkasItem(
                        Arrays.asList(butir("Croissant mentega", 2), butir("Danish keju", 1))));
        assertEquals("", PesananMitra.ringkasItem(null));
    }

    @Test
    public void alergiBeratDitaruhPalingDepan() {
        List<PesananMitraDto.Alergi> hasil =
                PesananMitra.urutAlergi(
                        Arrays.asList(
                                alergi("susu", "avoid"),
                                alergi("kacang_tanah", "severe"),
                                alergi("gluten", "avoid"),
                                alergi("udang", "severe")));
        assertEquals("kacang_tanah", hasil.get(0).code);
        assertEquals("udang", hasil.get(1).code);
        assertEquals("susu", hasil.get(2).code);
        assertEquals("gluten", hasil.get(3).code);
        assertTrue(PesananMitra.urutAlergi(null).isEmpty());
    }

    @Test
    public void adaAlergiBeratHanyaKalauSevere() {
        assertTrue(
                PesananMitra.adaAlergiBerat(
                        Arrays.asList(alergi("a", "avoid"), alergi("b", "severe"))));
        assertFalse(PesananMitra.adaAlergiBerat(Arrays.asList(alergi("a", "avoid"))));
        assertFalse(PesananMitra.adaAlergiBerat(null));
    }

    @Test
    public void kodeDirapikanJadiEnamKarakterKapital() {
        assertEquals("LF7Q2K", PesananMitra.rapikanKode("lf7q2k"));
        assertEquals("LF7Q2K", PesananMitra.rapikanKode(" LF-7Q 2K "));
        assertEquals("LF7Q2K", PesananMitra.rapikanKode("LF7Q2KXYZ"));
        assertEquals("", PesananMitra.rapikanKode(null));
    }

    @Test
    public void hitunganRiwayatMemisahkanSelesaiDanTidakDiambil() {
        PesananMitra.Hitungan h =
                PesananMitra.hitung(
                        Arrays.asList(pesanan("pending_pickup"), pesanan("pending_pickup")),
                        Arrays.asList(
                                pesanan("completed"),
                                pesanan("completed"),
                                pesanan("no_show"),
                                pesanan("cancelled")));
        assertEquals(2, h.menunggu);
        assertEquals(2, h.diambil);
        assertEquals(1, h.tidakDiambil);
    }

    private static PesananMitraDto.Butir butir(String judul, int qty) {
        PesananMitraDto.Butir b = new PesananMitraDto.Butir();
        b.title = judul;
        b.qty = qty;
        return b;
    }

    private static PesananMitraDto.Alergi alergi(String kode, String tingkat) {
        PesananMitraDto.Alergi a = new PesananMitraDto.Alergi();
        a.code = kode;
        a.name = kode;
        a.severity = tingkat;
        return a;
    }

    private static PesananMitraDto pesanan(String status) {
        PesananMitraDto p = new PesananMitraDto();
        p.status = status;
        return p;
    }

    @Test
    public void qrK14BisaDibacaLagiOlehM12() {
        assertEquals("LOF:LF7Q2K", PesananMitra.isiQr("lf7q2k"));
        assertEquals("LF7Q2K", PesananMitra.kodeDariQr(PesananMitra.isiQr("LF7Q2K")));
        assertEquals("LF7Q2K", PesananMitra.kodeDariQr("  lof:lf7q2k "));
    }

    @Test
    public void qrLainDitolak() {
        assertNull(PesananMitra.kodeDariQr(null));
        assertNull(PesananMitra.kodeDariQr("LF7Q2K"));
        assertNull(PesananMitra.kodeDariQr("https://contoh.id/bayar"));
        assertNull(PesananMitra.kodeDariQr("LOF:LF7Q"));
        assertNull(PesananMitra.kodeDariQr("LOF:LF7Q2K9"));
        assertNull(PesananMitra.kodeDariQr("LOF:LF-7Q2K"));
    }
}
