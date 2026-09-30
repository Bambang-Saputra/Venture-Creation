package id.lifeoffoods.data.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class PesananBodyTest {

    private final Gson gson = new Gson();

    @Test
    public void pratinjauHanyaMengirimItem() {
        PesananBody body = PesananBody.dari(new long[] {31, 32}, new int[] {1, 2});
        assertEquals(
                "{\"items\":[{\"listing_id\":31,\"qty\":1},{\"listing_id\":32,\"qty\":2}]}",
                gson.toJson(body));
    }

    @Test
    public void jumlahNolDilewati() {
        PesananBody body = PesananBody.dari(new long[] {31, 32}, new int[] {0, 3});
        assertEquals(1, body.items.size());
        assertEquals(32, body.items.get(0).listingId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void panjangArgumenHarusSama() {
        PesananBody.dari(new long[] {31}, new int[] {1, 2});
    }

    @Test
    public void catatanKosongTidakDikirim() {
        PesananBody body =
                PesananBody.dari(new long[] {31}, new int[] {1})
                        .untukDibuat("   ", PesananBody.BAYAR_TUNAI);
        assertNull(body.note);
        String json = gson.toJson(body);
        assertFalse(json.contains("note"));
        assertTrue(json.contains("\"payment_method\":\"cash\""));
    }

    @Test
    public void catatanDirapikanDanDipotong() {
        PesananBody dasar = PesananBody.dari(new long[] {31}, new int[] {1});
        assertEquals(
                "tanpa gula", dasar.untukDibuat("  tanpa gula \n", PesananBody.BAYAR_QRIS).note);

        StringBuilder panjang = new StringBuilder();
        for (int i = 0; i < 350; i++) {
            panjang.append('a');
        }
        String dipotong = dasar.untukDibuat(panjang.toString(), PesananBody.BAYAR_TUNAI).note;
        assertEquals(PesananBody.MAKS_CATATAN, dipotong.length());
    }

    @Test
    public void untukDibuatTidakMengubahPratinjau() {
        PesananBody dasar = PesananBody.dari(new long[] {31}, new int[] {1});
        dasar.untukDibuat("catatan", PesananBody.BAYAR_TUNAI);
        assertNull(dasar.note);
        assertNull(dasar.paymentMethod);
    }

    @Test
    public void kodeAktifHanyaSaatMenungguDiambil() {
        PesananDto p = new PesananDto();
        p.status = PesananDto.MENUNGGU_DIAMBIL;
        p.pickupCode = "LF7Q2K";
        assertTrue(p.kodeAktif());

        p.status = PesananDto.SELESAI;
        assertFalse(p.kodeAktif());

        p.status = PesananDto.MENUNGGU_DIAMBIL;
        p.pickupCode = null;
        assertFalse(p.kodeAktif());
    }
}
