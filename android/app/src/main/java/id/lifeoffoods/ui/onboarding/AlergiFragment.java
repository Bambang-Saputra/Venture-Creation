package id.lifeoffoods.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.databinding.FragmentAlergiBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/**
 * K05 Alergi dan pantangan, langkah 2 dari 2. Chip dibangun dari GET /allergens supaya alergen baru
 * cukup ditambah di SeederAlergen tanpa rilis ulang aplikasi. "Lewati" tidak memanggil API (kontrak
 * API, PUT /me/allergens).
 */
public class AlergiFragment extends Fragment {

    /** Argumen navigasi: true kalau dibuka dari profil (K18-K20), bukan dari onboarding. */
    public static final String ARG_DARI_PROFIL = "dari_profil";

    private FragmentAlergiBinding binding;
    private AlergiViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAlergiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(AlergiViewModel.class);
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);

        binding.kepala.judul.setText(R.string.k05_judul_bar);
        binding.kepala.subjudul.setText(R.string.k05_langkah);
        binding.kepala.subjudul.setVisibility(dariProfil() ? View.GONE : View.VISIBLE);
        // Dari profil: tidak ada "Lewati", Simpan kembali ke layar sebelumnya.
        binding.tombolLewati.setVisibility(dariProfil() ? View.GONE : View.VISIBLE);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolLewati.setOnClickListener(v -> selesai());
        binding.tombolSimpan.setOnClickListener(v -> vm.simpan());

        vm.muat();
        vm.status.observe(getViewLifecycleOwner(), this::tampilkanStatus);
        vm.daftar.observe(getViewLifecycleOwner(), this::bangunChip);
        vm.menyimpan.observe(
                getViewLifecycleOwner(),
                menyimpan -> {
                    binding.tombolSimpan.setEnabled(
                            !menyimpan && vm.status.getValue() == AlergiViewModel.Status.SIAP);
                    binding.tombolLewati.setEnabled(!menyimpan);
                    binding.progres.setVisibility(menyimpan ? View.VISIBLE : View.GONE);
                });
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.selesai.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        selesai();
                    }
                });
        vm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });
    }

    private void tampilkanStatus(AlergiViewModel.Status s) {
        boolean siap = s == AlergiViewModel.Status.SIAP;
        binding.progresMuat.setVisibility(
                s == AlergiViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.gagalMuat.setVisibility(
                s == AlergiViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        // Simpan tanpa daftar berarti mengosongkan pilihan tersimpan; cegah sampai daftar termuat.
        binding.tombolSimpan.setEnabled(siap && !Boolean.TRUE.equals(vm.menyimpan.getValue()));
    }

    private void bangunChip(@Nullable List<AlergenDto> daftar) {
        binding.grupAlergi.removeAllViews();
        binding.grupDiet.removeAllViews();
        if (daftar == null) {
            return;
        }
        for (AlergenDto a : daftar) {
            ChipGroup grup =
                    AlergenDto.TIPE_DIET.equals(a.type) ? binding.grupDiet : binding.grupAlergi;
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_pilihan, grup, false);
            chip.setText(a.name);
            chip.setChecked(vm.terpilih(a.code));
            chip.setOnCheckedChangeListener((c, dipilih) -> vm.pilih(a.code, dipilih));
            grup.addView(chip);
        }
        tampilkanJudul(binding.judulAlergi, binding.grupAlergi);
        tampilkanJudul(binding.judulDiet, binding.grupDiet);
    }

    private static void tampilkanJudul(View judul, ChipGroup grup) {
        judul.setVisibility(grup.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void selesai() {
        if (dariProfil()) {
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }
        ((MainActivity) requireActivity()).selesaiOnboarding();
    }

    /** Dibuka dari K18/K19/K20 untuk mengubah alergi, bukan bagian onboarding. */
    private boolean dariProfil() {
        return getArguments() != null && getArguments().getBoolean(ARG_DARI_PROFIL, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
