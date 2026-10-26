package id.lifeoffoods.ui.jualan;

import androidx.fragment.app.Fragment;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.model.ListingDto;
import java.util.Collections;
import java.util.Map;

/**
 * "Ambil sekarang" dari Beranda: langsung ke K12 (tas) atau K13 (menu satuan) dengan jumlah 1,
 * tanpa lewat detail. Peringatan alergi dan harga terbaru tetap diperiksa di ringkasan.
 */
public final class BeliLangsung {

    private BeliLangsung() {}

    public static void satu(Fragment f, ListingDto l) {
        Map<Long, Integer> terpilih = Collections.singletonMap(l.id, 1);
        BersamaDetail.lanjutKeRingkasan(
                f,
                ListingDto.TIPE_MENU.equals(l.type)
                        ? R.id.k13_ringkasan_menu
                        : R.id.k12_ringkasan_tas,
                terpilih,
                Collections.singletonList(l));
    }
}
