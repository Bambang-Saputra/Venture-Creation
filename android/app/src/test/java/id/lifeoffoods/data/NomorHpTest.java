package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NomorHpTest {

    @Test
    public void semuaBentukMasukanDinormalisasi() {
        assertEquals("6281234567890", NomorHp.normalisasi("081234567890"));
        assertEquals("6281234567890", NomorHp.normalisasi("+62 812-3456-7890"));
        assertEquals("6281234567890", NomorHp.normalisasi("6281234567890"));
        assertEquals("6281234567890", NomorHp.normalisasi("812 3456 7890"));
    }

    @Test
    public void bukanNomorSelulerDitolak() {
        assertNull(NomorHp.normalisasi(null));
        assertNull(NomorHp.normalisasi(""));
        assertNull(NomorHp.normalisasi("12345"));
        // Nomor telepon rumah Jakarta, bukan seluler.
        assertNull(NomorHp.normalisasi("0215551234"));
        assertNull(NomorHp.normalisasi("0812345"));
        assertNull(NomorHp.normalisasi("08123456789012"));
    }

    @Test
    public void tampilanUntukK03() {
        assertEquals("+62 812-3456-7890", NomorHp.tampilan("6281234567890"));
        assertEquals("+62 812-9988-776", NomorHp.tampilan("628129988776"));
    }
}
