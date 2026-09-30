package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aturan M09, M10, M16, M17 (PRD-09), Java murni supaya bisa diuji unit: status jualan, saklar,
 * potensi pemasukan, alergen yang dikirim, dan pemeriksaan formulir sebelum menekan "Terbitkan".
 */
public final class JualanMitra {

    /** Kode alergen kacang tanah untuk saklar "Dapur juga mengolah kacang". */
    public static final String KODE_KACANG = "kacang_tanah";

    public static final int JUMLAH_MAKS = 200;
    public static final long HARGA_MIN_TAS = 1000;

    public static final String HALAL_SERTIFIKAT = "certified";
    public static final String HALAL_KLAIM = "self_claim";
    public static final String HALAL_TIDAK = "not_stated";

    private JualanMitra() {}

    /** Warna pil status. */
    public enum Nada {
        HIJAU,
        ABU,
        KUNING
    }

    public static Nada nada(@Nullable String status) {
        if (JualanMitraDto.AKTIF.equals(status)) {
            return Nada.HIJAU;
        }
        if (JualanMitraDto.DIJEDA.equals(status) || JualanMitraDto.DRAF.equals(status)) {
            return Nada.KUNING;
        }
        return Nada.ABU;
    }

    /** Saklar menyala hanya kalau jualan aktif. */
    public static boolean saklarNyala(@Nullable String status) {
        return JualanMitraDto.AKTIF.equals(status);
    }

    /** Habis atau lewat tidak bisa dinyalakan lagi; pasang jualan baru. */
    public static boolean saklarBisaDiubah(@Nullable String status) {
        return JualanMitraDto.AKTIF.equals(status)
                || JualanMitraDto.DIJEDA.equals(status)
                || JualanMitraDto.DRAF.equals(status);
    }

    /** Harga jual x jumlah, dijumlahkan untuk semua item (M16). */
    public static long potensi(Collection<long[]> hargaDanJumlah) {
        long total = 0;
        for (long[] hj : hargaDanJumlah) {
            total += hj[0] * hj[1];
        }
        return total;
    }

    /**
     * Alergen yang dikirim: pilihan mitra sebagai "contains", ditambah kacang tanah sebagai
     * "may_contain" kalau dapur juga mengolah kacang dan kacang belum dipilih. Urutan pilihan
     * dipertahankan.
     */
    public static List<JualanMitraDto.Alergen> alergen(
            Collection<String> dipilih, boolean dapurKacang) {
        Map<String, String> hasil = new LinkedHashMap<>();
        for (String k : dipilih) {
            hasil.put(k, JualanMitraDto.Alergen.MENGANDUNG);
        }
        if (dapurKacang && !hasil.containsKey(KODE_KACANG)) {
            hasil.put(KODE_KACANG, JualanMitraDto.Alergen.MUNGKIN);
        }
        List<JualanMitraDto.Alergen> daftar = new ArrayList<>();
        for (Map.Entry<String, String> e : hasil.entrySet()) {
            daftar.add(new JualanMitraDto.Alergen(e.getKey(), e.getValue()));
        }
        return daftar;
    }

    /**
     * Label halal yang dikirim. Saklar menyala: pakai label sertifikat kalau cetakan memilikinya
     * (null = bawaan server), selain itu klaim sendiri. Saklar mati: tidak dinyatakan.
     */
    @Nullable
    public static String labelHalal(boolean nyala, @Nullable String bawaan) {
        if (!nyala) {
            return HALAL_TIDAK;
        }
        return HALAL_SERTIFIKAT.equals(bawaan) ? null : HALAL_KLAIM;
    }

    public static boolean halalBawaan(@Nullable String label) {
        return HALAL_SERTIFIKAT.equals(label) || HALAL_KLAIM.equals(label);
    }

    /** "19:00:00" atau "19:00" menjadi menit sejak tengah malam; -1 kalau tidak terbaca. */
    public static int menit(@Nullable String jam) {
        if (jam == null || jam.length() < 5 || jam.charAt(2) != ':') {
            return -1;
        }
        try {
            int j = Integer.parseInt(jam.substring(0, 2));
            int m = Integer.parseInt(jam.substring(3, 5));
            return j < 24 && m < 60 ? j * 60 + m : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Menit sejak tengah malam menjadi "HH:mm" untuk body API. */
    public static String jamApi(int menit) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d", menit / 60, menit % 60);
    }

    /** Hasil pemeriksaan formulir. Kunci galat = nama field API supaya sama dengan galat 422. */
    public static final class Periksa {
        public final Map<String, Integer> galat = new LinkedHashMap<>();

        public boolean sah() {
            return galat.isEmpty();
        }
    }

    /** Kode galat (dipetakan ke string di fragment). */
    public static final int G_JUDUL = 1;

    public static final int G_HARGA = 2;
    public static final int G_HARGA_NORMAL = 3;
    public static final int G_KANDUNGAN = 4;
    public static final int G_JAM = 5;
    public static final int G_ALERGEN = 6;
    public static final int G_JUMLAH = 7;
    public static final int G_ITEM = 8;

    /**
     * M09. Tas dari cetakan cukup jumlah, jam, dan pernyataan alergen. Tas campur juga wajib judul,
     * harga (min Rp1.000), harga normal tidak di bawah harga, dan kandungan.
     */
    public static Periksa periksaTas(
            boolean campur,
            @Nullable String judul,
            long harga,
            @Nullable Long hargaNormal,
            @Nullable String kandungan,
            int jumlah,
            int mulai,
            int akhir,
            boolean alergenDinyatakan) {
        Periksa p = new Periksa();
        if (campur) {
            if (judul == null || judul.trim().isEmpty()) {
                p.galat.put("title", G_JUDUL);
            }
            if (harga < HARGA_MIN_TAS) {
                p.galat.put("price_rupiah", G_HARGA);
            }
            if (hargaNormal != null && hargaNormal < harga) {
                p.galat.put("original_value_rupiah", G_HARGA_NORMAL);
            }
            if (kandungan == null || kandungan.trim().isEmpty()) {
                p.galat.put("ingredients_text", G_KANDUNGAN);
            }
        }
        periksaUmum(p, jumlah, mulai, akhir, alergenDinyatakan);
        return p;
    }

    /** M16. Minimal satu item berjumlah lebih dari nol, dan alergen tiap item sudah dinyatakan. */
    public static Periksa periksaMenu(
            int jumlahItemDipilih, int mulai, int akhir, boolean semuaAlergenDinyatakan) {
        Periksa p = new Periksa();
        if (jumlahItemDipilih == 0) {
            p.galat.put("items", G_ITEM);
        }
        if (mulai < 0 || akhir <= mulai) {
            p.galat.put("pickup_end", G_JAM);
        }
        if (!semuaAlergenDinyatakan) {
            p.galat.put("allergens", G_ALERGEN);
        }
        return p;
    }

    private static void periksaUmum(
            Periksa p, int jumlah, int mulai, int akhir, boolean alergenDinyatakan) {
        if (jumlah < 1 || jumlah > JUMLAH_MAKS) {
            p.galat.put("qty_total", G_JUMLAH);
        }
        if (mulai < 0 || akhir <= mulai) {
            p.galat.put("pickup_end", G_JAM);
        }
        if (!alergenDinyatakan) {
            p.galat.put("allergens", G_ALERGEN);
        }
    }
}
