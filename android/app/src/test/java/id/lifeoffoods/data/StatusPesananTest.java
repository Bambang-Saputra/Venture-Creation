package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class StatusPesananTest {

    private static final String MULAI = "2026-09-18T20:30:00+07:00";
    private static final Instant SEBELUM = Instant.parse("2026-09-18T13:00:00Z");
    private static final Instant SESUDAH = Instant.parse("2026-09-18T13:31:00Z");

    @Test
    public void menungguSampaiJamAmbilMulai() {
        assertEquals(
                StatusPesanan.Jenis.MENUNGGU,
                StatusPesanan.jenis("pending_pickup", MULAI, SEBELUM));
        assertEquals(
                StatusPesanan.Jenis.SIAP, StatusPesanan.jenis("pending_pickup", MULAI, SESUDAH));
        assertEquals("Siap diambil", StatusPesanan.label(StatusPesanan.Jenis.SIAP));
    }

    @Test
    public void statusAkhirTidakBergantungJam() {
        assertEquals(StatusPesanan.Jenis.SELESAI, StatusPesanan.jenis("completed", MULAI, SEBELUM));
        assertEquals(StatusPesanan.Jenis.BATAL, StatusPesanan.jenis("cancelled", MULAI, SESUDAH));
        assertEquals(
                StatusPesanan.Jenis.TIDAK_DIAMBIL, StatusPesanan.jenis("no_show", MULAI, SESUDAH));
    }

    @Test
    public void jamTidakTerbacaDianggapSiap() {
        assertEquals(StatusPesanan.Jenis.SIAP, StatusPesanan.jenis("pending_pickup", "x", SEBELUM));
    }
}
