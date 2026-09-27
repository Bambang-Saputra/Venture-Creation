package id.lifeoffoods.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import id.lifeoffoods.databinding.FragmentSegeraBinding;

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
        binding.kodeLayar.setText(args.getString(ARG_KODE, ""));
        binding.judulLayar.setText(args.getString(ARG_JUDUL, ""));
        binding.tombolKembali.setOnClickListener(
                v -> ((MainActivity) requireActivity()).kembaliKeAwal());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
