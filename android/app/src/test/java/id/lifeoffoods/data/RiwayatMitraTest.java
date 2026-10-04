package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;

public class RiwayatMitraTest {

    private static final LocalDate HARI_INI = LocalDate.of(2026, 9, 21);

    @Test
    public void judulHariSepertiFigma() {
        assertEquals("Hari ini", RiwayatMitra.judulHari(HARI_INI, HARI_INI));
        assertEquals("Kemarin", RiwayatMitra.judulHari(HARI_INI.minusDays(1), HARI_INI));
        assertEquals("2 hari lalu", RiwayatMitra.judulHari(HARI_INI.minusDays(2), HARI_INI));
        assertEquals("6 hari lalu", RiwayatMitra.judulHari(HARI_INI.minusDays(6), HARI_INI));
        assertEquals(
                "Senin, 14 September", RiwayatMitra.judulHari(HARI_INI.minusDays(7), HARI_INI));
        assertEquals(
                "Rabu, 31 Desember 2025",
                RiwayatMitra.judulHari(LocalDate.of(2025, 12, 31), HARI_INI));
    }

    @Test
    public void waktuTransaksiSaldo() {
        assertEquals("Hari ini, 20.34", RiwayatMitra.waktu("2026-09-21T20:34:00+07:00", HARI_INI));
        assertEquals("Kemarin, 09.00", RiwayatMitra.waktu("2026-09-20T02:00:00+00:00", HARI_INI));
        assertEquals("", RiwayatMitra.waktu(null, HARI_INI));
        assertEquals("Sabtu, 19 September", RiwayatMitra.hariTanggal(LocalDate.of(2026, 9, 19)));
    }

    @Test
    public void tanggalDibacaDalamWib() {
        // 17.30 UTC = 00.30 WIB keesokan harinya.
        assertEquals(LocalDate.of(2026, 9, 21), RiwayatMitra.tanggal("2026-09-20T17:30:00+00:00"));
        assertEquals(LocalDate.of(2026, 9, 20), RiwayatMitra.tanggal("2026-09-20T20:00:00+07:00"));
        assertNull(RiwayatMitra.tanggal("bukan tanggal"));
        assertNull(RiwayatMitra.tanggal(null));
    }

    @Test
    public void kelompokBaruSaatTanggalBerganti() {
        assertTrue(RiwayatMitra.awalKelompok(null, "2026-09-20T20:00:00+07:00"));
        assertFalse(
                RiwayatMitra.awalKelompok(
                        "2026-09-20T21:00:00+07:00", "2026-09-20T18:00:00+07:00"));
        assertTrue(
                RiwayatMitra.awalKelompok(
                        "2026-09-20T18:00:00+07:00", "2026-09-19T21:00:00+07:00"));
    }
}
