package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ValidasiProfilTest {

    @Test
    public void namaDirapikan() {
        assertEquals("Dara Renata", ValidasiProfil.nama("  Dara   Renata "));
    }

    @Test
    public void namaTerlaluPendekAtauKosongDitolak() {
        assertNull(ValidasiProfil.nama("D"));
        assertNull(ValidasiProfil.nama("   "));
        assertNull(ValidasiProfil.nama(null));
    }

    @Test
    public void namaTerlaluPanjangDitolak() {
        assertNull(ValidasiProfil.nama("a".repeat(ValidasiProfil.NAMA_MAKS + 1)));
        assertEquals(ValidasiProfil.NAMA_MAKS, ValidasiProfil.nama("a".repeat(120)).length());
    }

    @Test
    public void emailOpsional() {
        assertTrue(ValidasiProfil.emailSah(""));
        assertTrue(ValidasiProfil.emailSah(null));
        assertTrue(ValidasiProfil.emailSah(" dara.renata@email.com "));
    }

    @Test
    public void emailSalahKetikDitolak() {
        assertFalse(ValidasiProfil.emailSah("dara@email"));
        assertFalse(ValidasiProfil.emailSah("dara.email.com"));
        assertFalse(ValidasiProfil.emailSah("dara @email.com"));
    }

    @Test
    public void kosongTidakDikirim() {
        assertNull(ValidasiProfil.kosongJadiNull("  "));
        assertEquals("SCBD", ValidasiProfil.kosongJadiNull(" SCBD "));
    }
}
