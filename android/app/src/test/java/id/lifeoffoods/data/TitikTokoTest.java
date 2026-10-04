package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.ListingDto;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TitikTokoTest {

    private static ListingDto jualan(long id, long toko, long harga, Double lat, Double lng) {
        ListingDto l = new ListingDto();
        l.id = id;
        l.priceRupiah = harga;
        l.store = new ListingDto.Toko();
        l.store.id = toko;
        l.store.latitude = lat;
        l.store.longitude = lng;
        return l;
    }

    @Test
    public void satuTitikPerTokoDenganHargaTermurah() {
        ListingDto mahal = jualan(1, 5, 25000, -6.22, 106.80);
        ListingDto murah = jualan(2, 5, 18000, -6.22, 106.80);
        ListingDto lain = jualan(3, 7, 20000, -6.23, 106.81);

        List<TitikToko> t = TitikToko.dari(Arrays.asList(mahal, lain, murah));

        assertEquals(2, t.size());
        assertSame(murah, t.get(0).jualan);
        assertEquals(2, t.get(0).jumlah);
        assertSame(lain, t.get(1).jualan);
        assertEquals(1, t.get(1).jumlah);
    }

    @Test
    public void tokoTanpaKoordinatDilewati() {
        List<TitikToko> t =
                TitikToko.dari(
                        Arrays.asList(
                                jualan(1, 5, 18000, null, null), jualan(2, 6, 9000, -6.2, null)));
        assertTrue(t.isEmpty());
        assertTrue(TitikToko.dari(null).isEmpty());
    }

    @Test
    public void zoomMengecilSaatRadiusMembesar() {
        assertTrue(TitikToko.zoomUntuk(1) > TitikToko.zoomUntuk(3));
        assertTrue(TitikToko.zoomUntuk(3) > TitikToko.zoomUntuk(5));
    }
}
