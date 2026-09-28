package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.PesananDto;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Label status pesanan untuk pil K14 (dan nanti kartu K15). Java murni supaya bisa diuji. */
public final class StatusPesanan {

    public enum Jenis {
        /** Jam ambil sudah mulai: pil dengan titik lime. */
        SIAP,
        MENUNGGU,
        SELESAI,
        BATAL,
        TIDAK_DIAMBIL
    }

    private StatusPesanan() {}

    public static Jenis jenis(@Nullable String status, @Nullable String pickupStart, Instant kini) {
        if (PesananDto.STATUS_SELESAI.equals(status)) {
            return Jenis.SELESAI;
        }
        if (PesananDto.STATUS_BATAL.equals(status)) {
            return Jenis.BATAL;
        }
        if (PesananDto.STATUS_TIDAK_DIAMBIL.equals(status)) {
            return Jenis.TIDAK_DIAMBIL;
        }
        Instant mulai = waktu(pickupStart);
        return mulai == null || !kini.isBefore(mulai) ? Jenis.SIAP : Jenis.MENUNGGU;
    }

    public static String label(Jenis j) {
        switch (j) {
            case SIAP:
                return "Siap diambil";
            case MENUNGGU:
                return "Menunggu jam ambil";
            case SELESAI:
                return "Sudah diambil";
            case BATAL:
                return "Dibatalkan";
            default:
                return "Tidak diambil";
        }
    }

    @Nullable
    private static Instant waktu(@Nullable String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
