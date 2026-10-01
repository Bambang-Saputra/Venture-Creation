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

    /** PRD-05 kriteria 5: kata "halal" tidak pernah tampil tanpa keterangan. */
    @Test
    public void labelHalalPersisPrd() {
        assertEquals(
                "Bersertifikat halal · No. ID00110012345",
                Kandungan.labelHalal("certified", " ID00110012345 "));
        assertEquals("Bersertifikat halal", Kandungan.labelHalal("certified", null));
        assertEquals(
                "Klaim mitra, belum bersertifikat", Kandungan.labelHalal("self_claim", "ID001"));
        assertEquals("Status halal tidak disebutkan", Kandungan.labelHalal("not_stated", null));
        assertEquals("Status halal tidak disebutkan", Kandungan.labelHalal(null, null));
    }

    /** PRD-05 kriteria 6: baris K11 ditandai kalau alergennya ada di profil. */
    @Test
    public void cocokProfilTermasukMungkin() {
        Set<String> profil = Set.of("kacang_tanah", "susu");
        assertEquals(
                "Cocok dengan alergimu: susu dan kacang tanah",
                Kandungan.cocokProfil(Arrays.asList(SUSU, GLUTEN, KACANG), profil));
        assertNull(Kandungan.cocokProfil(List.of(GLUTEN), profil));
        assertNull(Kandungan.cocokProfil(List.of(SUSU), Collections.emptySet()));
        assertNull(Kandungan.cocokProfil(null, profil));
    }
}
