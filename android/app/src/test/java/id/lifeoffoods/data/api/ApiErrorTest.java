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

    /** Pesan persis dari PastikanAkunAktif dan OtpController (PRD-01 kriteria 8 dan 10). */
    @Test
    public void status403DibedakanDariPesannya() {
        ApiError nonaktif =
                ApiError.dariRespons(
                        403,
                        "{\"message\":\"Akun ini dinonaktifkan. Hubungi tim Life of Foods.\"}");
        assertTrue(nonaktif.perluMasukUlang());
        assertFalse(nonaktif.salahHalaman());

        ApiError salah =
                ApiError.dariRespons(
                        403,
                        "{\"message\":\"Nomor ini terdaftar sebagai mitra. Masuk lewat halaman"
                                + " mitra.\"}");
        assertTrue(salah.salahHalaman());
        assertFalse(salah.perluMasukUlang());

        // 403 biasa, misalnya kasir membuka laporan mingguan: bukan alasan keluar.
        ApiError kasir = ApiError.dariRespons(403, "{\"message\":\"Hanya pemilik toko.\"}");
        assertFalse(kasir.perluMasukUlang());
        assertFalse(kasir.salahHalaman());
    }

    @Test
    public void galatJaringanBerkode0() {
        ApiError galat = ApiError.jaringan();

        assertEquals(ApiError.TANPA_JARINGAN, galat.kode());
        assertTrue(galat.pesan().startsWith("Tidak bisa terhubung"));
    }
}
