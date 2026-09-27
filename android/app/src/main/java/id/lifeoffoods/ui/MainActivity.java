package id.lifeoffoods.ui;

import android.os.Bundle;
import androidx.annotation.NavigationRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.databinding.ActivityMainBinding;

/**
 * Satu-satunya activity. Graf navigasi dipilih sesuai peran (peta layar, catatan K01): nav_awal
 * untuk yang belum masuk, nav_konsumen atau nav_mitra setelah masuk.
 */
public class MainActivity extends AppCompatActivity {

    private NavController nav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        nav = host.getNavController();

        // Setelah rotasi atau proses dipulihkan, NavController memulihkan grafnya sendiri.
        if (savedInstanceState == null) {
            SesiPengguna sesi = ((LofApp) getApplication()).sesi();
            nav.setGraph(sesi.sudahMasuk() ? grafUntuk(sesi.peran()) : R.navigation.nav_awal);
        }
    }

    /** Dari K01: masuk ke alur konsumen atau mitra. Belum menyimpan sesi; itu tugas K03/M02. */
    public void bukaAlur(String peran) {
        nav.setGraph(grafUntuk(peran));
    }

    /** Kembali ke K01, misalnya setelah logout atau token ditolak (401). */
    public void kembaliKeAwal() {
        nav.setGraph(R.navigation.nav_awal);
    }

    @NavigationRes
    private static int grafUntuk(@Nullable String peran) {
        return SesiPengguna.PERAN_MITRA.equals(peran)
                ? R.navigation.nav_mitra
                : R.navigation.nav_konsumen;
    }
}
