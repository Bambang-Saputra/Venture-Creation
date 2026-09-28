package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.ListingDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class KandunganTest {

    private static ListingDto.Alergen a(String kode, String nama, String presence) {
        ListingDto.Alergen x = new ListingDto.Alergen();
        x.code = kode;
        x.name = nama;
        x.presence = presence;
        return x;
    }

    private static final ListingDto.Alergen SUSU = a("susu", "Susu", "contains");
    private static final ListingDto.Alergen GLUTEN = a("gluten", "Gluten", "contains");
    private static final ListingDto.Alergen KACANG =
            a("kacang_tanah", "Kacang tanah", "may_contain");

    @Test
    public void ringkasSepertiBarisMenuFigma() {
        assertEquals("Mengandung susu, gluten", Kandungan.ringkas(Arrays.asList(SUSU, GLUTEN)));
        assertEquals("Mungkin mengandung kacang tanah", Kandungan.ringkas(List.of(KACANG)));
        assertEquals(
                "Mengandung susu. Mungkin mengandung kacang tanah",
                Kandungan.ringkas(Arrays.asList(SUSU, KACANG)));
        assertEquals("", Kandungan.ringkas(null));
    }

    @Test
    public void gabungTigaKata() {
        assertEquals(
                "susu, telur, dan gluten", Kandungan.gabung(List.of("susu", "telur", "gluten")));
    }

    @Test
    public void peringatanHanyaUntukAlergiDiProfilTermasukMungkin() {
        List<ListingDto.Alergen> jualan = Arrays.asList(SUSU, GLUTEN, KACANG);
        assertNull(Kandungan.peringatan(jualan, Set.of("telur")));
        assertNull(Kandungan.peringatan(jualan, Collections.emptySet()));
        assertEquals(
                "Dapur mitra juga mengolah kacang tanah. Ini cocok dengan alergi di profilmu, jadi kami"
                        + " beri tanda.",
                Kandungan.peringatan(jualan, Set.of("kacang_tanah")));
        assertTrue(
                Kandungan.peringatan(jualan, Set.of("susu"))
                        .startsWith("Jualan ini mengandung susu."));
    }

    @Test
    public void hanyaMungkin() {
        assertTrue(Kandungan.hanyaMungkin(List.of(KACANG)));
        assertFalse(Kandungan.hanyaMungkin(Arrays.asList(SUSU, KACANG)));
        assertFalse(Kandungan.hanyaMungkin(null));
    }
}
