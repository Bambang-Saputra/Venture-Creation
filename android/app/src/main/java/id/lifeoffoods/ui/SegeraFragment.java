package id.lifeoffoods.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.databinding.FragmentSegeraBinding;
import id.lifeoffoods.ui.umum.KeluarAkun;
import id.lifeoffoods.ui.umum.SisiAman;

/**
 * Penanda layar yang belum dibuat. Ganti tujuan di graf navigasi dengan fragment sungguhan saat
 * layarnya dikerjakan, lalu hapus kelas ini kalau sudah tidak dipakai.
 */
public class SegeraFragment extends Fragment {

    public static final String ARG_KODE = "kode_layar";
    public static final String ARG_JUDUL = "judul";

    private FragmentSegeraBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSegeraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        SisiAman.atasBawah(binding.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        // Kode layar (K16, M08, ...) hanya berguna untuk developer; responden tidak perlu
        // melihatnya.
        binding.kodeLayar.setVisibility(View.GONE);
        binding.judulLayar.setText(args.getString(ARG_JUDUL, ""));

        // Penanda yang dibuka dari layar lain (misalnya K08 dari beranda) cukup kembali. Penanda
        // yang berdiri di tempat beranda, dan K18 Profil, dipakai untuk keluar dari akun.
        LofApp app = (LofApp) requireActivity().getApplication();
        NavController nav = NavHostFragment.findNavController(this);
        boolean bisaKembali =
                nav.getPreviousBackStackEntry() != null && !"K18".equals(args.getString(ARG_KODE));
        if (bisaKembali) {
            binding.tombolKembali.setText(R.string.kembali);
            binding.tombolKembali.setOnClickListener(v -> nav.popBackStack());
            return;
        }
        boolean masuk = app.sesi().sudahMasuk();
        binding.tombolKembali.setText(masuk ? R.string.segera_keluar : R.string.segera_kembali);
        binding.tombolKembali.setOnClickListener(
                v -> {
                    if (masuk) {
                        KeluarAkun.tanya(this);
                    } else {
                        KeluarAkun.jalankan(this);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
