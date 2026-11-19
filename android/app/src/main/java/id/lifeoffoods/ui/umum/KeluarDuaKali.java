package id.lifeoffoods.ui.umum;

import android.os.SystemClock;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import id.lifeoffoods.R;

/**
 * Di layar akar (K01, K07, M05) tombol kembali pertama hanya memberi tahu, yang kedua dalam dua
 * detik baru menutup aplikasi. Layar yang dibuka dari layar lain tetap kembali seperti biasa.
 */
public final class KeluarDuaKali {

    private static final long JEDA_MS = 2000;

    private KeluarDuaKali() {}

    public static void pasang(Fragment f) {
        OnBackPressedCallback cb =
                new OnBackPressedCallback(true) {
                    private long terakhir;

                    @Override
                    public void handleOnBackPressed() {
                        if (NavHostFragment.findNavController(f).getPreviousBackStackEntry()
                                != null) {
                            // Bukan layar akar: biarkan navigasi biasa yang menangani.
                            setEnabled(false);
                            f.requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            setEnabled(true);
                            return;
                        }
                        long sekarang = SystemClock.elapsedRealtime();
                        if (sekarang - terakhir < JEDA_MS) {
                            f.requireActivity().finish();
                        } else {
                            terakhir = sekarang;
                            Toast.makeText(
                                            f.requireContext(),
                                            R.string.keluar_tekan_lagi,
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                };
        f.requireActivity().getOnBackPressedDispatcher().addCallback(f.getViewLifecycleOwner(), cb);
    }
}
