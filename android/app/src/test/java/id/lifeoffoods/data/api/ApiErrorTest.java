package id.lifeoffoods.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApiErrorTest {

    @Test
    public void galatValidasiMembacaPesanDanField() {
        ApiError galat =
                ApiError.dariRespons(
                        422,
                        "{\"message\":\"Nomor HP tidak valid.\","
                                + "\"errors\":{\"phone\":[\"Nomor HP tidak valid.\",\"Lain\"]}}");

        assertEquals(422, galat.kode());
        assertEquals("Nomor HP tidak valid.", galat.pesan());
        assertEquals("Nomor HP tidak valid.", galat.pesanField("phone"));
        assertNull(galat.pesanField("code"));
        assertFalse(galat.perluMasukUlang());
    }

    @Test
    public void galatTanpaErrorsTetapPunyaPesan() {
        ApiError galat = ApiError.dariRespons(409, "{\"message\":\"Kode ini sudah dipakai.\"}");

        assertEquals("Kode ini sudah dipakai.", galat.pesan());
        assertNull(galat.pesanField("code"));
    }

    @Test
    public void bodyBukanJsonMemakaiPesanUmum() {
        ApiError galat = ApiError.dariRespons(502, "<html>ngrok</html>");

        assertEquals(502, galat.kode());
        assertTrue(galat.pesan().startsWith("Terjadi kesalahan"));
    }

    @Test
    public void bodyKosongMemakaiPesanUmum() {
        assertTrue(ApiError.dariRespons(500, null).pesan().startsWith("Terjadi kesalahan"));
        assertTrue(ApiError.dariRespons(500, "").pesan().startsWith("Terjadi kesalahan"));
    }

    @Test
    public void status401BerartiMasukUlang() {
        assertTrue(
                ApiError.dariRespons(401, "{\"message\":\"Unauthenticated.\"}").perluMasukUlang());
    }

    @Test
    public void galatJaringanBerkode0() {
        ApiError galat = ApiError.jaringan();

        assertEquals(ApiError.TANPA_JARINGAN, galat.kode());
        assertTrue(galat.pesan().startsWith("Tidak bisa terhubung"));
    }
}
