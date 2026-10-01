package id.lifeoffoods.ui.filter;

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
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.databinding.FragmentFilterBinding;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/**
 * K09 Filter (PRD-04). Dibuka dari kolom cari K07 dengan filter yang sedang dipakai sebagai
 * argumen; "Tampilkan N jualan" mengirim hasilnya ke K07 lewat setFragmentResult. Profil alergi
 * tidak pernah ditulis dari sini (kriteria 3).
 */
public class FilterFragment extends Fragment {

    private FragmentFilterBinding binding;
    private FilterViewModel vm;

    /** true selama view diselaraskan dari ViewModel, supaya listener chip tidak ikut terpicu. */
    private boolean menyelaraskan;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(FilterViewModel.class);
        SisiAman.atas(binding.kepala);
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);

        binding.chipJarak1.setText(getString(R.string.k09_jarak_km, 1));
        binding.chipJarak3.setText(getString(R.string.k09_jarak_km, 3));
        binding.chipJarak5.setText(getString(R.string.k09_jarak_km, 5));

        binding.tombolTutup.setOnClickListener(v -> tutup());
        binding.tombolAturUlang.setOnClickListener(v -> vm.aturUlang());
        binding.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolTampilkan.setOnClickListener(v -> terapkan());

        pasangJenis(binding.chipTas, FilterJualan.TIPE_TAS);
        pasangJenis(binding.chipMenu, FilterJualan.TIPE_MENU);
        binding.chipHalal.setOnCheckedChangeListener(
                (c, dipilih) -> {
                    if (!menyelaraskan) {
                        vm.ubahHalal(dipilih);
                    }
                });
        pasangJarak(binding.chipJarak1, 1);
        pasangJarak(binding.chipJarak3, 3);
        pasangJarak(binding.chipJarak5, 5);
        pasangJam(binding.chipJamSampai20, FilterJualan.JAM_SAMPAI_20);
        pasangJam(binding.chipJam2022, FilterJualan.JAM_20_22);
        binding.saklarSembunyikan.setOnCheckedChangeListener(
                (s, nyala) -> {
                    if (!menyelaraskan) {
                        vm.ubahSembunyikan(nyala);
                    }
                });
        binding.barisSembunyikan.setOnClickListener(v -> binding.saklarSembunyikan.toggle());

        vm.muat(FilterBundle.dari(getArguments()));
        vm.daftarAlergen.observe(getViewLifecycleOwner(), this::bangunChipAlergen);
        vm.filter.observe(getViewLifecycleOwner(), this::selaraskan);
        vm.status.observe(
                getViewLifecycleOwner(),
                s -> {
                    binding.progresAlergen.setVisibility(
                            s == FilterViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
                    binding.gagalAlergen.setVisibility(
                            s == FilterViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
                });
        vm.adaLokasi.observe(
                getViewLifecycleOwner(),
                ada -> binding.bagianJarak.setVisibility(ada ? View.VISIBLE : View.GONE));
        vm.jumlah.observe(getViewLifecycleOwner(), this::tampilkanJumlah);
    }

    private void pasangJenis(Chip chip, String tipe) {
        chip.setOnCheckedChangeListener(
                (c, dipilih) -> {
                    if (menyelaraskan) {
                        return;
                    }
                    if (!vm.ubahJenis(tipe, dipilih)) {
                        // Jenis terakhir tidak boleh dilepas: centang lagi dan jelaskan.
                        menyelaraskan = true;
                        chip.setChecked(true);
                        menyelaraskan = false;
                        Snackbar.make(
                                        binding.getRoot(),
                                        R.string.k09_jenis_minimal,
                                        Snackbar.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void pasangJarak(Chip chip, int km) {
        chip.setOnCheckedChangeListener(
                (c, dipilih) -> {
                    if (!menyelaraskan) {
                        vm.pilihJarak(km, dipilih);
                    }
                });
    }

    private void pasangJam(Chip chip, String jam) {
        chip.setOnCheckedChangeListener(
                (c, dipilih) -> {
                    if (!menyelaraskan) {
                        vm.pilihJam(jam, dipilih);
                    }
                });
    }

    private void bangunChipAlergen(@Nullable List<AlergenDto> daftar) {
        binding.grupAlergi.removeAllViews();
        if (daftar == null) {
            return;
        }
        for (AlergenDto a : daftar) {
            Chip chip =
                    (Chip)
                            getLayoutInflater()
                                    .inflate(R.layout.item_chip_pilihan, binding.grupAlergi, false);
            chip.setText(FilterJualan.labelTanpa(a.name));
            chip.setTag(a.code);
            chip.setOnCheckedChangeListener(
                    (c, dipilih) -> {
                        if (!menyelaraskan) {
                            vm.ubahAlergen(a, dipilih);
                        }
                    });
            binding.grupAlergi.addView(chip);
        }
        selaraskan(vm.filter.getValue());
    }

    /** Samakan semua chip dan saklar dengan filter di ViewModel tanpa memicu listener. */
    private void selaraskan(@Nullable FilterJualan f) {
        if (f == null || binding == null) {
            return;
        }
        menyelaraskan = true;
        binding.chipTas.setChecked(f.tas);
        binding.chipMenu.setChecked(f.menu);
        for (int i = 0; i < binding.grupAlergi.getChildCount(); i++) {
            Chip chip = (Chip) binding.grupAlergi.getChildAt(i);
            chip.setChecked(f.alergen.containsKey((String) chip.getTag()));
            // Saklar mati: pilihan tetap tersimpan, tapi tampak redup karena tidak dipakai.
            chip.setAlpha(f.sembunyikanAlergi ? 1f : 0.5f);
        }
        binding.chipHalal.setChecked(f.halal);
        binding.chipJarak1.setChecked(f.radiusKm != null && f.radiusKm == 1);
        binding.chipJarak3.setChecked(f.radiusKm != null && f.radiusKm == 3);
        binding.chipJarak5.setChecked(f.radiusKm != null && f.radiusKm == 5);
        binding.chipJamSampai20.setChecked(FilterJualan.JAM_SAMPAI_20.equals(f.jam));
        binding.chipJam2022.setChecked(FilterJualan.JAM_20_22.equals(f.jam));
        binding.saklarSembunyikan.setChecked(f.sembunyikanAlergi);
        menyelaraskan = false;

        if (!f.sembunyikanAlergi) {
            binding.ketSembunyikan.setText(R.string.k09_sembunyikan_mati);
        } else if (f.alergen.isEmpty()) {
            binding.ketSembunyikan.setText(R.string.k09_sembunyikan_kosong);
        } else {
            binding.ketSembunyikan.setText(
                    getString(R.string.k09_sembunyikan_nyala, f.daftarNamaAlergen()));
        }
        FilterJualan bawaan = vm.bawaanProfil();
        binding.tombolAturUlang.setEnabled(bawaan == null || !f.equals(bawaan));
    }

    private void tampilkanJumlah(@Nullable Integer n) {
        if (n == null) {
            binding.tombolTampilkan.setText(R.string.k09_tampilkan_memuat);
        } else if (n == 0) {
            binding.tombolTampilkan.setText(R.string.k09_tampilkan_kosong);
        } else {
            binding.tombolTampilkan.setText(getString(R.string.k09_tampilkan, n));
        }
    }

    private void terapkan() {
        FilterJualan f = vm.filter.getValue();
        if (f != null) {
            getParentFragmentManager().setFragmentResult(FilterBundle.HASIL, FilterBundle.ke(f));
        }
        tutup();
    }

    private void tutup() {
        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
