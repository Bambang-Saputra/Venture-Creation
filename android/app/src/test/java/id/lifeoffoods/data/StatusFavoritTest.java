package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.FavoritDto;
import org.junit.Test;

public class StatusFavoritTest {

    private static FavoritDto toko() {
        FavoritDto f = new FavoritDto();
        f.id = 5;
        f.name = "Kopi Kalyan";
        return f;
    }

    @Test
    public void tasDidahulukanDaripadaMenu() {
        FavoritDto f = toko();
        f.availableBags = 2;
        f.hasMenuAvailable = true;
        StatusFavorit s = StatusFavorit.dari(f);
        assertEquals(StatusFavorit.Jenis.TAS, s.jenis);
        assertEquals(2, s.jumlah);
        assertTrue(s.tersedia());
    }

    @Test
    public void menuSaatTasHabis() {
        FavoritDto f = toko();
        f.hasMenuAvailable = true;
        assertEquals(StatusFavorit.Jenis.MENU, StatusFavorit.dari(f).jenis);
    }

    @Test
    public void tanpaStokMemakaiJamBiasa() {
        FavoritDto f = toko();
        f.usualPublishTime = "19:00";
        StatusFavorit s = StatusFavorit.dari(f);
        assertEquals(StatusFavorit.Jenis.BIASANYA, s.jenis);
        assertEquals("19.00", s.jam);
        assertFalse(s.tersedia());
    }

    @Test
    public void tanpaStokDanJamBiasaKosong() {
        assertEquals(StatusFavorit.Jenis.KOSONG, StatusFavorit.dari(toko()).jenis);
    }

    @Test
    public void tutupSementaraMengalahkanStok() {
        FavoritDto f = toko();
        f.availableBags = 3;
        f.isTemporarilyClosed = true;
        assertEquals(StatusFavorit.Jenis.TUTUP, StatusFavorit.dari(f).jenis);
    }
}
