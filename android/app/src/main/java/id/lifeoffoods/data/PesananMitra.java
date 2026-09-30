package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aturan tampilan M11 Pesanan masuk (PRD-10) dan M12 Cocokkan kode (PRD-11), Java murni supaya bisa
 * diuji unit.
 */
public final class PesananMitra {

    /** Panjang kode pickup. Sama dengan validasi server (size:6). */
    public static final int PANJANG_KODE = 6;

    public static final String ALERGI_BERAT = "severe";

    private PesananMitra() {}

    /** Hitungan ubin ringkasan M11. */
    public static final class Hitungan {
        public final int menunggu;
        public final int diambil;
        public final int tidakDiambil;

        public Hitungan(int menunggu, int diambil, int tidakDiambil) {
            this.menunggu = menunggu;
            this.diambil = diambil;
            this.tidakDiambil = tidakDiambil;
        }
    }

    /** "Tas Pastry Sore × 1, Danish keju × 2". */
    public static String ringkasItem(@Nullable List<PesananMitraDto.Butir> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PesananMitraDto.Butir b : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(b.title).append(" × ").append(b.qty);
        }
        return sb.toString();
    }

    /**
     * Alergi berat di depan supaya paling mencolok (PRD-10 kriteria 3). Urutan asli dipertahankan
     * di dalam tiap kelompok.
     */
    public static List<PesananMitraDto.Alergi> urutAlergi(
            @Nullable List<PesananMitraDto.Alergi> daftar) {
        List<PesananMitraDto.Alergi> berat = new ArrayList<>();
        List<PesananMitraDto.Alergi> lain = new ArrayList<>();
        if (daftar != null) {
            for (PesananMitraDto.Alergi a : daftar) {
                (ALERGI_BERAT.equals(a.severity) ? berat : lain).add(a);
            }
        }
        berat.addAll(lain);
        return berat;
    }

    public static boolean adaAlergiBerat(@Nullable List<PesananMitraDto.Alergi> daftar) {
        if (daftar != null) {
            for (PesananMitraDto.Alergi a : daftar) {
                if (ALERGI_BERAT.equals(a.severity)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Kode yang diketik kasir: huruf dan angka saja, kapital, paling banyak 6. Spasi atau tanda
     * hubung yang ikut tertempel dibuang.
     */
    public static String rapikanKode(@Nullable CharSequence ketikan) {
        if (ketikan == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ketikan.length() && sb.length() < PANJANG_KODE; i++) {
            char c = ketikan.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    /** Ubin "Menunggu / Diambil / Tidak diambil" dari daftar menunggu dan riwayat hari ini. */
    public static Hitungan hitung(
            @Nullable List<PesananMitraDto> menunggu, @Nullable List<PesananMitraDto> riwayat) {
        int diambil = 0;
        int tidak = 0;
        if (riwayat != null) {
            for (PesananMitraDto p : riwayat) {
                if (PesananDto.SELESAI.equals(p.status)) {
                    diambil++;
                } else if (PesananDto.TIDAK_DIAMBIL.equals(p.status)) {
                    tidak++;
                }
            }
        }
        return new Hitungan(menunggu == null ? 0 : menunggu.size(), diambil, tidak);
    }
}
