package id.lifeoffoods.ui.mitra;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import id.lifeoffoods.R;
import id.lifeoffoods.databinding.IncludeNavMitraBinding;

/**
 * Nav mitra (Beranda, Catat, Pesanan, Toko). Beranda = M05, tujuan awal; Toko = M14. Tab lain
 * dibuka tepat di atas M05, jadi back stack tidak menumpuk saat berpindah tab dan tombol kembali
 * selalu ke M05.
 */
final class NavMitra {

    private NavMitra() {}

    /**
     * @param aktif id tujuan tab yang sedang terbuka; tab itu ditandai terpilih.
     */
    static void pasang(Fragment f, IncludeNavMitraBinding nav, @IdRes int aktif) {
        nav.tabBeranda.setSelected(aktif == R.id.m05_dashboard);
        nav.tabCatat.setSelected(aktif == R.id.m06_catat);
        nav.tabPesanan.setSelected(aktif == R.id.m11_pesanan);
        // M10 dibuka dari M14 ("Kelola jualan"), jadi tetap terhitung tab Toko.
        nav.tabToko.setSelected(aktif == R.id.m14_toko || aktif == R.id.m10_kelola);
        nav.tabBeranda.setOnClickListener(v -> buka(f, R.id.m05_dashboard, aktif));
        nav.tabCatat.setOnClickListener(v -> buka(f, R.id.m06_catat, aktif));
        nav.tabPesanan.setOnClickListener(v -> buka(f, R.id.m11_pesanan, aktif));
        nav.tabToko.setOnClickListener(v -> buka(f, R.id.m14_toko, aktif));
    }

    /** Tombol "Kelola jualan" di keadaan kosong M11; sama dengan tab Toko. */
    static void bukaKelola(Fragment f) {
        buka(f, R.id.m10_kelola, R.id.m11_pesanan);
    }

    private static void buka(Fragment f, @IdRes int tujuan, @IdRes int aktif) {
        if (tujuan == aktif) {
            return;
        }
        NavController nav = NavHostFragment.findNavController(f);
        if (tujuan == R.id.m05_dashboard && nav.popBackStack(R.id.m05_dashboard, false)) {
            return;
        }
        nav.navigate(
                tujuan,
                null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.m05_dashboard, false)
                        .setLaunchSingleTop(true)
                        .build());
    }
}
