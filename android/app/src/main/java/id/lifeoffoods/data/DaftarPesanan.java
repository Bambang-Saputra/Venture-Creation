package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.PesananRingkasDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aturan tampilan K15 Pesanan saya (PRD-08), Java murni supaya bisa diuji unit: jenis status untuk
 * warna pil, selisih hari untuk "Hari ini" / "Kemarin", dan urutan tab Aktif.
 */
public final class DaftarPesanan {

    /** Jenis pil status. Teksnya ada di strings.xml (k15_status_*). */
    public enum Jenis {
        MENUNGGU,
        SELESAI,
        DIBATALKAN,
        TIDAK_DIAMBIL
    }

    private DaftarPesanan() {}

    public static Jenis jenis(@Nullable String status) {
        if (PesananDto.SELESAI.equals(status)) {
            return Jenis.SELESAI;
        }
        if (PesananDto.DIBATALKAN.equals(status)) {
            return Jenis.DIBATALKAN;
        }
        if (PesananDto.TIDAK_DIAMBIL.equals(status)) {
            return Jenis.TIDAK_DIAMBIL;
        }
        return Jenis.MENUNGGU;
    }

    /**
     * Selisih hari kalender antara {@code iso} dan {@code hariIni}: 0 hari ini, 1 kemarin, -1
     * besok. Tanggal dibaca dalam zona waktu yang dikirim server (WIB). Null kalau tidak terbaca.
     */
    @Nullable
    public static Long selisihHari(@Nullable String iso, LocalDate hariIni) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            LocalDate tanggal = OffsetDateTime.parse(iso).toLocalDate();
            return ChronoUnit.DAYS.between(tanggal, hariIni);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Tab Aktif: jam ambil paling dekat di atas (PRD-08 kriteria 3). API mengurutkan dari pesanan
     * terbaru, jadi diurutkan ulang di sini. Tanggal yang tidak terbaca ditaruh paling bawah.
     */
    public static List<PesananRingkasDto> urutAktif(List<PesananRingkasDto> daftar) {
        List<PesananRingkasDto> hasil = new ArrayList<>(daftar);
        hasil.sort(
                Comparator.comparing(
                        (PesananRingkasDto p) -> waktu(p.pickupStart),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return hasil;
    }

    @Nullable
    private static OffsetDateTime waktu(@Nullable String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
