package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.FavoritDto;

/**
 * Chip status di kartu K16 (Figma): "2 tas tersedia", "Menu satuan tersedia", "Belum ada tas hari
 * ini", "Biasanya pasang jam 19.00", ditambah "Tutup sementara" yang tidak ada di Figma.
 */
public final class StatusFavorit {

    public enum Jenis {
        TAS,
        MENU,
        TUTUP,
        BIASANYA,
        KOSONG
    }

    public final Jenis jenis;

    /** Jumlah tas untuk TAS. */
    public final int jumlah;

    /** "19.00" untuk BIASANYA. */
    @Nullable public final String jam;

    private StatusFavorit(Jenis jenis, int jumlah, @Nullable String jam) {
        this.jenis = jenis;
        this.jumlah = jumlah;
        this.jam = jam;
    }

    /** true untuk chip hijau (ada yang bisa dipesan sekarang). */
    public boolean tersedia() {
        return jenis == Jenis.TAS || jenis == Jenis.MENU;
    }

    public static StatusFavorit dari(FavoritDto f) {
        if (f.isTemporarilyClosed) {
            return new StatusFavorit(Jenis.TUTUP, 0, null);
        }
        if (f.availableBags > 0) {
            return new StatusFavorit(Jenis.TAS, f.availableBags, null);
        }
        if (f.hasMenuAvailable) {
            return new StatusFavorit(Jenis.MENU, 0, null);
        }
        String jam = f.usualPublishTime;
        if (jam != null && jam.length() >= 5) {
            return new StatusFavorit(Jenis.BIASANYA, 0, jam.substring(0, 5).replace(':', '.'));
        }
        return new StatusFavorit(Jenis.KOSONG, 0, null);
    }
}
