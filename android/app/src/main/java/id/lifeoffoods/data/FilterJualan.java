package id.lifeoffoods.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.MeResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pilihan filter K09 yang dipakai K07 (PRD-04). Kelas Java murni supaya aturan query bisa diuji
 * tanpa Android. Mengubah filter tidak pernah mengubah profil alergi (kriteria 3): profil hanya
 * dipakai sebagai isian awal lewat {@link #dariProfil}.
 */
public final class FilterJualan {

    public static final String TIPE_TAS = "surprise_bag";
    public static final String TIPE_MENU = "menu_item";

    /** Jam ambil "Sekarang sampai 20.00". */
    public static final String JAM_SAMPAI_20 = "sampai_20";

    /** Jam ambil "20.00 sampai 22.00". */
    public static final String JAM_20_22 = "20_22";

    /** Pilihan jarak di Figma K09 (km). */
    public static final List<Integer> PILIHAN_JARAK =
            Collections.unmodifiableList(Arrays.asList(1, 3, 5));

    public boolean tas = true;
    public boolean menu = true;

    /** Kode alergen yang dihindari, ke nama tampilannya, sesuai urutan GET /allergens. */
    public final LinkedHashMap<String, String> alergen = new LinkedHashMap<>();

    /** Saklar "Sembunyikan yang bertanda alergi". Mati = alergen tidak dikirim ke API. */
    public boolean sembunyikanAlergi = true;

    public boolean halal;

    /** null = tanpa batas jarak. */
    @Nullable public Integer radiusKm;

    /** null, {@link #JAM_SAMPAI_20}, atau {@link #JAM_20_22}. */
    @Nullable public String jam;

    /** Isian awal: semua jenis, alergen dari profil tercentang (kriteria 2), sisanya kosong. */
    @NonNull
    public static FilterJualan dariProfil(@Nullable Map<String, String> alergenProfil) {
        FilterJualan f = new FilterJualan();
        if (alergenProfil != null) {
            f.alergen.putAll(alergenProfil);
        }
        return f;
    }

    /**
     * Alergen profil (GET /me) yang bertipe allergen. Pola makan (diet) tidak ikut: jualan belum
     * punya penanda diet (kontrak API bagian 12), jadi tidak bisa difilter.
     */
    @NonNull
    public static Map<String, String> alergenProfil(@Nullable List<MeResponse.Alergen> daftar) {
        Map<String, String> m = new LinkedHashMap<>();
        if (daftar != null) {
            for (MeResponse.Alergen a : daftar) {
                if (a != null && a.code != null && !AlergenDto.TIPE_DIET.equals(a.type)) {
                    m.put(a.code, a.name == null ? a.code : a.name);
                }
            }
        }
        return m;
    }

    @NonNull
    public FilterJualan salin() {
        FilterJualan f = new FilterJualan();
        f.tas = tas;
        f.menu = menu;
        f.alergen.putAll(alergen);
        f.sembunyikanAlergi = sembunyikanAlergi;
        f.halal = halal;
        f.radiusKm = radiusKm;
        f.jam = jam;
        return f;
    }

    /**
     * Ubah satu jenis jualan. Jenis terakhir tidak bisa dilepas, karena tanpa jenis sama sekali
     * daftarnya pasti kosong. Mengembalikan false kalau ditolak.
     */
    public boolean ubahJenis(@NonNull String tipe, boolean dipilih) {
        if (!dipilih && !(TIPE_TAS.equals(tipe) ? menu : tas)) {
            return false;
        }
        if (TIPE_TAS.equals(tipe)) {
            tas = dipilih;
        } else {
            menu = dipilih;
        }
        return true;
    }

    /** true kalau ada alergen yang benar-benar dikirim ke API (penanda K07, kriteria 5). */
    public boolean alergiAktif() {
        return sembunyikanAlergi && !alergen.isEmpty();
    }

    /** Nilai type[]; null kalau keduanya dipilih, supaya query sama dengan tanpa filter. */
    @Nullable
    public List<String> tipe() {
        if (tas && menu) {
            return null;
        }
        return Collections.singletonList(tas ? TIPE_TAS : TIPE_MENU);
    }

    /** Nilai exclude_allergens[]; null kalau tidak ada yang dihindari atau saklarnya mati. */
    @Nullable
    public List<String> alergenDikirim() {
        return alergiAktif() ? new ArrayList<>(alergen.keySet()) : null;
    }

    /**
     * Query tunggal untuk GET /listings. radius_km butuh lat/lng (validasi API), jadi tanpa
     * koordinat jarak diabaikan.
     */
    @NonNull
    public Map<String, String> query(@Nullable Double lat, @Nullable Double lng) {
        Map<String, String> q = new HashMap<>();
        boolean adaLokasi = lat != null && lng != null;
        if (adaLokasi) {
            q.put("lat", String.format(Locale.US, "%.6f", lat));
            q.put("lng", String.format(Locale.US, "%.6f", lng));
            if (radiusKm != null) {
                q.put("radius_km", Integer.toString(radiusKm));
            }
        }
        if (halal) {
            q.put("halal", "1");
        }
        if (JAM_SAMPAI_20.equals(jam)) {
            q.put("pickup_until", "20:00");
        } else if (JAM_20_22.equals(jam)) {
            q.put("pickup_from", "20:00");
            q.put("pickup_until", "22:00");
        }
        return q;
    }

    /** Label chip K09 dari nama GET /allergens: "Kacang tanah" jadi "Tanpa kacang tanah". */
    @NonNull
    public static String labelTanpa(@Nullable String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            return "";
        }
        String n = nama.trim();
        return "Tanpa " + n.substring(0, 1).toLowerCase(Locale.ROOT) + n.substring(1);
    }

    /** "kacang tanah", "kacang tanah dan susu", "kacang tanah, susu, dan telur" (huruf kecil). */
    @NonNull
    public String daftarNamaAlergen() {
        List<String> nama = new ArrayList<>();
        for (String n : alergen.values()) {
            if (n != null && !n.trim().isEmpty()) {
                nama.add(n.trim().toLowerCase(Locale.ROOT));
            }
        }
        int jumlah = nama.size();
        if (jumlah == 0) {
            return "";
        }
        if (jumlah == 1) {
            return nama.get(0);
        }
        if (jumlah == 2) {
            return nama.get(0) + " dan " + nama.get(1);
        }
        return String.join(", ", nama.subList(0, jumlah - 1)) + ", dan " + nama.get(jumlah - 1);
    }

    /** Jumlah filter di luar bawaan profil, untuk lencana di kolom cari K07. */
    public int jumlahTambahan(@Nullable FilterJualan bawaan) {
        int n = 0;
        if (!tas || !menu) {
            n++;
        }
        if (halal) {
            n++;
        }
        if (radiusKm != null) {
            n++;
        }
        if (jam != null) {
            n++;
        }
        boolean alergenBeda =
                bawaan == null
                        ? !alergen.isEmpty()
                        : !alergen.keySet().equals(bawaan.alergen.keySet());
        if (alergenBeda || !sembunyikanAlergi) {
            n++;
        }
        return n;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FilterJualan)) {
            return false;
        }
        FilterJualan f = (FilterJualan) o;
        return tas == f.tas
                && menu == f.menu
                && sembunyikanAlergi == f.sembunyikanAlergi
                && halal == f.halal
                && alergen.keySet().equals(f.alergen.keySet())
                && Objects.equals(radiusKm, f.radiusKm)
                && Objects.equals(jam, f.jam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tas, menu, sembunyikanAlergi, halal, alergen.keySet(), radiusKm, jam);
    }
}
