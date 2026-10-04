package id.lifeoffoods.data;

import androidx.annotation.Nullable;

import id.lifeoffoods.data.api.model.TokoDetailDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Logika tampilan M14 Profil toko dan M15 Pengaturan toko, tanpa Android supaya bisa dites. */
public final class ProfilToko {

    /** Senin dulu, seperti di Figma M14. Angka mengikuti day_of_week API (0 = Minggu). */
    private static final int[] URUTAN_HARI = {1, 2, 3, 4, 5, 6, 0};

    private static final String[] NAMA_HARI = {
        "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
    };

    private ProfilToko() {}

    /** Satu baris jam operasional, contoh "Senin sampai Jumat" dengan "07.00 sampai 21.00". */
    public static final class BarisJam {
        public final String hari;

        /** "07.00 sampai 21.00", atau null kalau tutup. */
        @Nullable public final String jam;

        BarisJam(String hari, @Nullable String jam) {
            this.hari = hari;
            this.jam = jam;
        }
    }

    /**
     * Gabungkan hari berurutan dengan jam yang sama. Hari yang tidak ada di data dianggap tutup.
     * Daftar kosong berarti jam operasional belum diisi sama sekali.
     */
    public static List<BarisJam> ringkasJam(@Nullable List<TokoDetailDto.JamToko> data) {
        List<BarisJam> hasil = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return hasil;
        }
        Map<Integer, String> perHari = new HashMap<>();
        for (TokoDetailDto.JamToko j : data) {
            perHari.put(j.dayOfWeek, jamHari(j));
        }

        int mulai = 0;
        while (mulai < URUTAN_HARI.length) {
            String jam = perHari.get(URUTAN_HARI[mulai]);
            int akhir = mulai;
            while (akhir + 1 < URUTAN_HARI.length
                    && Objects.equals(perHari.get(URUTAN_HARI[akhir + 1]), jam)) {
                akhir++;
            }
            hasil.add(new BarisJam(labelHari(mulai, akhir), jam));
            mulai = akhir + 1;
        }
        return hasil;
    }

    @Nullable
    private static String jamHari(TokoDetailDto.JamToko j) {
        if (j.isClosed || j.openTime == null || j.closeTime == null) {
            return null;
        }
        return FormatTampilan.jamToko(j.openTime)
                + " sampai "
                + FormatTampilan.jamToko(j.closeTime);
    }

    private static String labelHari(int mulai, int akhir) {
        String awal = NAMA_HARI[URUTAN_HARI[mulai]];
        if (mulai == akhir) {
            return awal;
        }
        String ujung = NAMA_HARI[URUTAN_HARI[akhir]];
        return akhir - mulai == 1 ? awal + " dan " + ujung : awal + " sampai " + ujung;
    }

    /**
     * "6281299887766" menjadi "0812-9988-7766". Nomor yang tidak dikenal dikembalikan apa adanya.
     */
    public static String nomorHp(@Nullable String nomor) {
        if (nomor == null || !nomor.startsWith("62") || nomor.length() < 9) {
            return nomor == null ? "" : nomor;
        }
        String lokal = "0" + nomor.substring(2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lokal.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(lokal.charAt(i));
        }
        return sb.toString();
    }

    /** "4,8 (180)" seperti chip di Figma M14. Null kalau belum ada ulasan. */
    @Nullable
    public static String rating(@Nullable Double rataRata, int jumlah) {
        if (rataRata == null || jumlah <= 0) {
            return null;
        }
        return String.format(Locale.ROOT, "%.1f", rataRata).replace('.', ',') + " (" + jumlah + ")";
    }
}
