package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import org.junit.Test;

public class PromosiTokoTest {

    @Test
    public void totalMemakaiTarifPerHariDanBatasHari() {
        assertEquals(35000, PromosiToko.total(5000, 7));
        assertEquals(10000, PromosiToko.total(10000, 0));
        assertEquals(300000, PromosiToko.total(10000, 45));
    }

    @Test
    public void rentangTanggalTanpaTahun() {
        LocalDate mulai = LocalDate.of(2026, 10, 6);
        assertEquals("6 Okt sampai 12 Okt", PromosiToko.rentang(mulai, 7));
        assertEquals("6 Okt", PromosiToko.rentang(mulai, 1));
        assertEquals("28 Okt sampai 3 Nov", PromosiToko.rentang(LocalDate.of(2026, 10, 28), 7));
    }

    @Test
    public void rentangDariJawabanApi() {
        assertEquals("6 Okt sampai 8 Okt", PromosiToko.rentang("2026-10-06", "2026-10-08"));
        assertNull(PromosiToko.rentang("bukan-tanggal", "2026-10-08"));
        assertNull(PromosiToko.rentang(null, "2026-10-08"));
    }
}
