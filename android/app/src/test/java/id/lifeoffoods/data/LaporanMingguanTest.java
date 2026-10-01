package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import org.junit.Test;

public class LaporanMingguanTest {

    @Test
    public void seninDariHariApaPun() {
        LocalDate senin = LocalDate.of(2026, 9, 14);
        assertEquals(senin, LaporanMingguan.senin(senin));
        assertEquals(senin, LaporanMingguan.senin(LocalDate.of(2026, 9, 17)));
        assertEquals(senin, LaporanMingguan.senin(LocalDate.of(2026, 9, 20)));
    }

    @Test
    public void rentangSatuBulanLintasBulanLintasTahun() {
        assertEquals(
                "8 sampai 14 September 2026",
                LaporanMingguan.rentang(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14)));
        assertEquals(
                "28 September sampai 4 Oktober 2026",
                LaporanMingguan.rentang(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)));
        assertEquals(
                "29 Desember 2025 sampai 4 Januari 2026",
                LaporanMingguan.rentang(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)));
    }

    @Test
    public void ribuDanTinggiBatang() {
        assertEquals("42", LaporanMingguan.ribu(42000));
        assertEquals("<1", LaporanMingguan.ribu(400));
        assertEquals("0", LaporanMingguan.ribu(0));
        assertEquals("1.240", LaporanMingguan.ribu(1_240_000));

        assertEquals(610, LaporanMingguan.maks(Arrays.asList(310L, null, 610L, 0L)));
        // Hari tidak dicatat tidak punya batang; hari bernilai kecil tetap terlihat.
        assertEquals(0, LaporanMingguan.tinggi(null, 610, 100, 4));
        assertEquals(0, LaporanMingguan.tinggi(0L, 610, 100, 4));
        assertEquals(4, LaporanMingguan.tinggi(1L, 610, 100, 4));
        assertEquals(100, LaporanMingguan.tinggi(610L, 610, 100, 4));
        assertEquals(51, LaporanMingguan.tinggi(310L, 610, 100, 4));

        assertEquals(1f, LaporanMingguan.porsi(952, 952), 0f);
        assertEquals(0f, LaporanMingguan.porsi(0, 952), 0f);
    }
}
