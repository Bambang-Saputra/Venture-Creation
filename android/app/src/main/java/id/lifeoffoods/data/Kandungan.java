package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.ListingDto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Teks kandungan alergen untuk K10 dan baris menu K11. Java murni supaya aturan peringatan bisa
 * diuji unit: salah tulis di sini berarti pembeli alergi tidak diberi tanda.
 */
public final class Kandungan {

    public static final String HALAL_SERTIFIKAT = "certified";
    public static final String HALAL_KLAIM = "self_claim";

    private Kandungan() {}

    /**
     * Label halal persis PRD-05 kriteria 5. Kata "halal" tidak pernah tampil tanpa keterangan:
     * certified = "Bersertifikat halal · No. {nomor}", self_claim = "Klaim mitra, belum
     * bersertifikat", selain itu (not_stated, kosong, nilai tak dikenal) = "Status halal tidak
     * disebutkan". Daftar K11 tidak membawa nomor sertifikat, jadi nomornya hanya tampil kalau ada.
     */
    public static String labelHalal(@Nullable String label, @Nullable String nomorSertifikat) {
        if (HALAL_SERTIFIKAT.equals(label)) {
            String no = nomorSertifikat == null ? "" : nomorSertifikat.trim();
            return no.isEmpty() ? "Bersertifikat halal" : "Bersertifikat halal · No. " + no;
        }
        if (HALAL_KLAIM.equals(label)) {
            return "Klaim mitra, belum bersertifikat";
        }
        return "Status halal tidak disebutkan";
    }

    /**
     * Baris K11 (PRD-05 kriteria 6): nama alergen jualan yang ada di profil pembeli, termasuk
     * may_contain. Null kalau tidak ada yang cocok.
     */
    @Nullable
    public static String cocokProfil(
            @Nullable List<ListingDto.Alergen> alergen, @Nullable Set<String> profil) {
        if (alergen == null || profil == null || profil.isEmpty()) {
            return null;
        }
        List<String> nama = new ArrayList<>();
        for (ListingDto.Alergen a : alergen) {
            if (profil.contains(a.code)) {
                nama.add(a.name == null ? a.code : a.name.toLowerCase(Locale.ROOT));
            }
        }
        return nama.isEmpty() ? null : "Cocok dengan alergimu: " + gabung(nama);
    }

    /**
     * Baris di bawah nama menu K11, seperti Figma: "Mengandung susu, gluten", atau "Mungkin
     * mengandung kacang" kalau semuanya may_contain. Keduanya ada: "Mengandung susu. Mungkin
     * mengandung kacang". Kosong kalau tidak ada alergen tercatat.
     */
    public static String ringkas(@Nullable List<ListingDto.Alergen> alergen) {
        List<String> pasti = new ArrayList<>();
        List<String> mungkin = new ArrayList<>();
        pisah(alergen, pasti, mungkin);
        StringBuilder sb = new StringBuilder();
        if (!pasti.isEmpty()) {
            sb.append("Mengandung ").append(String.join(", ", pasti));
        }
        if (!mungkin.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append("Mungkin mengandung ").append(String.join(", ", mungkin));
        }
        return sb.toString();
    }

    /** true kalau semua alergen yang tercatat hanya may_contain (warna teks peringatan K11). */
    public static boolean hanyaMungkin(@Nullable List<ListingDto.Alergen> alergen) {
        if (alergen == null || alergen.isEmpty()) {
            return false;
        }
        for (ListingDto.Alergen a : alergen) {
            if (!ListingDto.Alergen.MUNGKIN.equals(a.presence)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kotak peringatan K10: alergen jualan yang ada di profil pembeli, termasuk may_contain (untuk
     * alergi, ragu berarti diberi tanda). Null kalau tidak ada yang cocok.
     */
    @Nullable
    public static String peringatan(
            @Nullable List<ListingDto.Alergen> alergen, @Nullable Set<String> profil) {
        if (alergen == null || profil == null || profil.isEmpty()) {
            return null;
        }
        List<String> pasti = new ArrayList<>();
        List<String> mungkin = new ArrayList<>();
        List<ListingDto.Alergen> cocok = new ArrayList<>();
        for (ListingDto.Alergen a : alergen) {
            if (profil.contains(a.code)) {
                cocok.add(a);
            }
        }
        if (cocok.isEmpty()) {
            return null;
        }
        pisah(cocok, pasti, mungkin);
        StringBuilder sb = new StringBuilder();
        if (!pasti.isEmpty()) {
            sb.append("Jualan ini mengandung ").append(gabung(pasti)).append(". ");
        }
        if (!mungkin.isEmpty()) {
            sb.append("Dapur mitra juga mengolah ").append(gabung(mungkin)).append(". ");
        }
        return sb.append("Ini cocok dengan alergi di profilmu, jadi kami beri tanda.").toString();
    }

    private static void pisah(
            @Nullable Collection<ListingDto.Alergen> alergen,
            List<String> pasti,
            List<String> mungkin) {
        if (alergen == null) {
            return;
        }
        for (ListingDto.Alergen a : alergen) {
            String nama = a.name == null ? a.code : a.name.toLowerCase(Locale.ROOT);
            (ListingDto.Alergen.MUNGKIN.equals(a.presence) ? mungkin : pasti).add(nama);
        }
    }

    /** "susu", "susu dan telur", "susu, telur, dan gluten". */
    static String gabung(List<String> kata) {
        int n = kata.size();
        if (n == 1) {
            return kata.get(0);
        }
        if (n == 2) {
            return kata.get(0) + " dan " + kata.get(1);
        }
        return String.join(", ", kata.subList(0, n - 1)) + ", dan " + kata.get(n - 1);
    }
}
