package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import id.lifeoffoods.data.api.model.PesananRingkasDto;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DaftarPesananTest {

    @Test
    public void jenisMengikutiStatusApi() {
        assertEquals(DaftarPesanan.Jenis.MENUNGGU, DaftarPesanan.jenis("pending_pickup"));
        assertEquals(DaftarPesanan.Jenis.SELESAI, DaftarPesanan.jenis("completed"));
        assertEquals(DaftarPesanan.Jenis.DIBATALKAN, DaftarPesanan.jenis("cancelled"));
        assertEquals(DaftarPesanan.Jenis.TIDAK_DIAMBIL, DaftarPesanan.jenis("no_show"));
    }

    @Test
    public void statusTakDikenalDianggapMenunggu() {
        assertEquals(DaftarPesanan.Jenis.MENUNGGU, DaftarPesanan.jenis(null));
        assertEquals(DaftarPesanan.Jenis.MENUNGGU, DaftarPesanan.jenis("baru"));
    }

    @Test
    public void selisihHariMemakaiTanggalZonaServer() {
        LocalDate hariIni = LocalDate.of(2026, 9, 18);
        assertEquals(
                Long.valueOf(0), DaftarPesanan.selisihHari("2026-09-18T20:30:00+07:00", hariIni));
        assertEquals(
                Long.valueOf(1), DaftarPesanan.selisihHari("2026-09-17T20:10:00+07:00", hariIni));
        assertEquals(
                Long.valueOf(-1), DaftarPesanan.selisihHari("2026-09-19T08:00:00+07:00", hariIni));
        // 23.30 WIB tetap tanggal 18 walau di UTC sudah lewat tengah hari; jangan geser ke tanggal
        // lain.
        assertEquals(
                Long.valueOf(0), DaftarPesanan.selisihHari("2026-09-18T23:30:00+07:00", hariIni));
    }

    @Test
    public void selisihHariKosongKalauTakTerbaca() {
        LocalDate hariIni = LocalDate.of(2026, 9, 18);
        assertNull(DaftarPesanan.selisihHari(null, hariIni));
        assertNull(DaftarPesanan.selisihHari("", hariIni));
        assertNull(DaftarPesanan.selisihHari("kemarin", hariIni));
    }

    @Test
    public void tabAktifMengurutkanJamAmbilTerdekatDiAtas() {
        PesananRingkasDto malam = baris(1, "2026-09-18T20:30:00+07:00");
        PesananRingkasDto sore = baris(2, "2026-09-18T19:00:00+07:00");
        PesananRingkasDto rusak = baris(3, null);
        PesananRingkasDto besok = baris(4, "2026-09-19T08:00:00+07:00");

        List<PesananRingkasDto> hasil =
                DaftarPesanan.urutAktif(Arrays.asList(malam, rusak, besok, sore));

        assertEquals(2, hasil.get(0).id);
        assertEquals(1, hasil.get(1).id);
        assertEquals(4, hasil.get(2).id);
        assertEquals(3, hasil.get(3).id);
    }

    private static PesananRingkasDto baris(long id, String mulai) {
        PesananRingkasDto p = new PesananRingkasDto();
        p.id = id;
        p.pickupStart = mulai;
        return p;
    }
}
