package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.OffsetDateTime;
import org.junit.Test;

public class AkunPembeliTest {

    private static final OffsetDateTime SEKARANG =
            OffsetDateTime.parse("2026-09-21T20:40:00+07:00");

    @Test
    public void waktuNotifikasiSepertiFigmaK17() {
        assertEquals(
                "Baru saja", AkunPembeli.waktuNotifikasi("2026-09-21T20:39:40+07:00", SEKARANG));
        assertEquals(
                "5 menit lalu", AkunPembeli.waktuNotifikasi("2026-09-21T20:35:00+07:00", SEKARANG));
        assertEquals(
                "1 jam lalu", AkunPembeli.waktuNotifikasi("2026-09-21T19:30:00+07:00", SEKARANG));
        assertEquals(
                "Kemarin, 20.12",
                AkunPembeli.waktuNotifikasi("2026-09-20T20:12:00+07:00", SEKARANG));
        assertEquals("", AkunPembeli.waktuNotifikasi(null, SEKARANG));
    }

    @Test
    public void rupiahRingkasUntukUbinHemat() {
        assertEquals("Rp412rb", AkunPembeli.rupiahRingkas(412_500));
        assertEquals("Rp500", AkunPembeli.rupiahRingkas(500));
        assertEquals("Rp1,2jt", AkunPembeli.rupiahRingkas(1_250_000));
        assertEquals("Rp3jt", AkunPembeli.rupiahRingkas(3_000_000));
        assertEquals("Rp0", AkunPembeli.rupiahRingkas(0));
    }

    @Test
    public void anggotaSejak() {
        assertEquals("Maret 2026", AkunPembeli.bulanTahun("2026-03-05T10:00:00+07:00"));
        assertNull(AkunPembeli.bulanTahun(null));
    }
}
