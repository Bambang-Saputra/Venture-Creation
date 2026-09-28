package id.lifeoffoods.ui.awal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.databinding.FragmentPilihPeranBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;

/** K01 Pilih peran. Tidak memanggil API, hanya memilih graf navigasi. */
public class PilihPeranFragment extends Fragment {

    private FragmentPilihPeranBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPilihPeranBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) requireActivity();
        // Gradien mengisi sampai ke balik status bar dan navbar, seperti Figma; isinya diberi
        // jarak. Ikon sistem putih di atas latar hijau.
        SisiAman.atasBawah(binding.isi);
        SisiAman.ikonGelap(activity, false);
        binding.kartuKonsumen.setOnClickListener(
                v -> activity.bukaAlur(SesiPengguna.PERAN_KONSUMEN));
        binding.kartuMitra.setOnClickListener(v -> activity.bukaAlur(SesiPengguna.PERAN_MITRA));
        // Akun konsumen dibuat otomatis saat OTP pertama diverifikasi, jadi "Daftar" = alur
        // konsumen.
        binding.tautanDaftar.setOnClickListener(
                v -> activity.bukaAlur(SesiPengguna.PERAN_KONSUMEN));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
