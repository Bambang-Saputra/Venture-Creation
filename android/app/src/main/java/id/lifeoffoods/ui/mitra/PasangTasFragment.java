package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.TemplateTasDto;
import id.lifeoffoods.databinding.FragmentPasangTasBinding;
import id.lifeoffoods.databinding.ItemPilihanTemplateBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.jualan.Stepper;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** M09 Pasang tas (PRD-09). Setelah terbit kembali ke M10 dengan segmen tas kejutan. */
public class PasangTasFragment extends Fragment {

    private FragmentPasangTasBinding binding;
    private PasangTasViewModel vm;
    private Chip chipTanpa;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPasangTasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PasangTasViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.aksi.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m09_judul_bar);
        binding.kepala.subjudul.setText(R.string.m09_subjudul);
        binding.kepala.subjudul.setVisibility(View.VISIBLE);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        binding.segmen.tabTas.setSelected(true);
        binding.segmen.tabMenu.setSelected(false);
        binding.segmen.tabMenu.setOnClickListener(v -> keMenu());

        binding.harga.addTextChangedListener(
                new PenontonTeks(
                        s -> {
                            vm.ubahHargaCampur(angka(s));
                            binding.isianHarga.setError(null);
                        }));
        hapusGalatSaatMengetik(binding.namaTas, binding.isianJudul);
        hapusGalatSaatMengetik(binding.hargaNormal, binding.isianHargaNormal);
        hapusGalatSaatMengetik(binding.kandungan, binding.isianKandungan);

        TampilanPasang.pasangJam(this, binding.jam, vm);
        TampilanPasang.pasangLabel(this, binding.labelToko, vm);
        binding.aksi.tombolTerbit.setOnClickListener(v -> terbitkan());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.template.observe(getViewLifecycleOwner(), t -> bangunTemplate());
        vm.terpilih.observe(
                getViewLifecycleOwner(),
                id -> {
                    tandaiTemplate();
                    binding.formCampur.setVisibility(
                            vm.cetakan() == null ? View.VISIBLE : View.GONE);
                    perbaruiHarga();
                });
        vm.jumlah.observe(
                getViewLifecycleOwner(),
                j -> {
                    int n = j == null ? 1 : j;
                    Stepper.isi(
                            binding.stepper,
                            n,
                            n > 1,
                            n < JualanMitra.JUMLAH_MAKS,
                            () -> vm.ubahJumlah(false),
                            () -> vm.ubahJumlah(true));
                    perbaruiHarga();
                });
        vm.hargaCampur.observe(getViewLifecycleOwner(), h -> perbaruiHarga());
        vm.alergen.observe(getViewLifecycleOwner(), this::bangunAlergen);
        vm.alergenTerpilih.observe(getViewLifecycleOwner(), s -> tandaiAlergen());
        vm.tanpaAlergen.observe(getViewLifecycleOwner(), s -> tandaiAlergen());
        vm.galatField.observe(getViewLifecycleOwner(), this::tampilkanGalat);
        vm.mengirim.observe(
                getViewLifecycleOwner(),
                jalan -> {
                    binding.aksi.tombolTerbit.setEnabled(!jalan);
                    binding.aksi.tombolTerbit.setText(
                            jalan ? R.string.m09_menerbitkan : R.string.m09_terbitkan);
                });
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.terbit.observe(
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

    private void tampilkanStatus() {
        PasangJualanViewModel.Status s = vm.status.getValue();
        boolean siap = s == PasangJualanViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.aksi.getRoot().setVisibility(siap ? View.VISIBLE : View.GONE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == PasangJualanViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == PasangJualanViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == PasangJualanViewModel.Status.GAGAL && vm.galatAwal.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galatAwal.getValue());
        }
    }

    private void bangunTemplate() {
        binding.daftarTemplate.removeAllViews();
        List<TemplateTasDto> d = vm.template.getValue();
        if (d != null) {
            for (TemplateTasDto t : d) {
                String isi =
                        t.contentHint == null || t.contentHint.trim().isEmpty()
                                ? getString(R.string.m09_template_tanpa_isi)
                                : getString(R.string.m09_template_isi, t.contentHint.trim());
                tambahPilihan(t.id, t.name, isi, FormatTampilan.rupiah(t.priceRupiah));
            }
        }
        tambahPilihan(
                PasangTasViewModel.CAMPUR,
                getString(R.string.m09_campur),
                getString(R.string.m09_campur_isi),
                null);
        tandaiTemplate();
    }

    private void tambahPilihan(long id, String nama, String isi, @Nullable String harga) {
        ItemPilihanTemplateBinding b =
                ItemPilihanTemplateBinding.inflate(
                        getLayoutInflater(), binding.daftarTemplate, false);
        b.nama.setText(nama);
        b.isi.setText(isi);
        b.harga.setText(harga);
        b.harga.setVisibility(harga == null ? View.GONE : View.VISIBLE);
        b.getRoot().setTag(id);
        b.getRoot().setContentDescription(harga == null ? nama : nama + ", " + harga);
        b.getRoot().setOnClickListener(v -> vm.pilih(id));
        binding.daftarTemplate.addView(b.getRoot());
    }

    private void tandaiTemplate() {
        Long terpilih = vm.terpilih.getValue();
        for (int i = 0; i < binding.daftarTemplate.getChildCount(); i++) {
            View v = binding.daftarTemplate.getChildAt(i);
            boolean pilih = terpilih != null && terpilih.equals(v.getTag());
            v.setSelected(pilih);
            ItemPilihanTemplateBinding b = ItemPilihanTemplateBinding.bind(v);
            b.radio.setChecked(pilih);
            // Tanpa animasi supaya radio langsung menunjukkan keadaan akhirnya.
            b.radio.jumpDrawablesToCurrentState();
        }
    }

    private void perbaruiHarga() {
        long h = vm.harga();
        binding.hargaTampil.setText(h > 0 ? FormatTampilan.rupiah(h) : "–");
        binding.aksi.potensi.setText(FormatTampilan.rupiah(vm.potensi()));
    }

    private void bangunAlergen(@Nullable List<AlergenDto> daftar) {
        binding.grupAlergen.removeAllViews();
        if (daftar != null) {
            for (AlergenDto a : daftar) {
                Chip c =
                        (Chip)
                                getLayoutInflater()
                                        .inflate(
                                                R.layout.item_chip_pilihan,
                                                binding.grupAlergen,
                                                false);
                c.setText(a.name);
                c.setTag(a.code);
                c.setOnClickListener(v -> vm.pilihAlergen(a.code, c.isChecked()));
                binding.grupAlergen.addView(c);
            }
        }
        chipTanpa =
                (Chip)
                        getLayoutInflater()
                                .inflate(R.layout.item_chip_pilihan, binding.grupAlergen, false);
        chipTanpa.setText(R.string.m09_tanpa_alergen);
        chipTanpa.setOnClickListener(v -> vm.pilihTanpaAlergen(chipTanpa.isChecked()));
        binding.grupAlergen.addView(chipTanpa);
        tandaiAlergen();
    }

    private void tandaiAlergen() {
        Set<String> s = vm.alergenTerpilih.getValue();
        for (int i = 0; i < binding.grupAlergen.getChildCount(); i++) {
            Chip c = (Chip) binding.grupAlergen.getChildAt(i);
            if (c == chipTanpa) {
                c.setChecked(Boolean.TRUE.equals(vm.tanpaAlergen.getValue()));
            } else {
                c.setChecked(s != null && s.contains((String) c.getTag()));
            }
        }
    }

    private void terbitkan() {
        String hn = teks(binding.hargaNormal);
        Map<String, Integer> lokal =
                vm.terbitkan(
                        teks(binding.namaTas),
                        hn.isEmpty() ? null : angka(hn),
                        teks(binding.perkiraan),
                        teks(binding.kandungan));
        if (lokal.isEmpty()) {
            return;
        }
        Map<String, String> pesan = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : lokal.entrySet()) {
            pesan.put(e.getKey(), getString(TampilanPasang.teks(e.getValue(), false)));
        }
        vm.galatField.setValue(pesan);
        Snackbar.make(binding.getRoot(), R.string.m09_g_periksa, Snackbar.LENGTH_SHORT).show();
    }

    private void tampilkanGalat(@Nullable Map<String, String> g) {
        galatIsian(binding.isianJudul, g, "title");
        galatIsian(binding.isianHarga, g, "price_rupiah");
        galatIsian(binding.isianHargaNormal, g, "original_value_rupiah");
        galatIsian(binding.isianKandungan, g, "ingredients_text");
        TampilanPasang.tampilGalat(binding.galatJumlah, g, "qty_total");
        TampilanPasang.tampilGalat(binding.galatAlergen, g, "allergens", "template_id");
        TampilanPasang.tampilGalat(binding.jam.galatJam, g, "pickup_end", "pickup_start");
    }

    private static void galatIsian(TextInputLayout l, @Nullable Map<String, String> g, String f) {
        l.setError(g == null ? null : g.get(f));
    }

    private void hapusGalatSaatMengetik(android.widget.EditText e, TextInputLayout l) {
        e.addTextChangedListener(new PenontonTeks(s -> l.setError(null)));
    }

    private void keMenu() {
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.m09_pasang_tas, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.m16_pasang_menu, null, opsi);
    }

    private void selesai() {
        Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        R.string.m09_terbit,
                        Snackbar.LENGTH_LONG)
                .show();
        // Kembali ke M10 (tas kejutan); M10 dimuat ulang di onStart.
        Bundle a = new Bundle();
        a.putString(KelolaJualanFragment.ARG_TIPE, JualanMitraDto.TIPE_TAS);
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.m10_kelola, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.m10_kelola, a, opsi);
    }

    private static String teks(android.widget.EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    static long angka(CharSequence s) {
        String d = s.toString().replaceAll("[^0-9]", "");
        if (d.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(d);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        chipTanpa = null;
    }

    /** TextWatcher satu metode. */
    static final class PenontonTeks implements TextWatcher {
        interface Ubah {
            void jadi(CharSequence s);
        }

        private final Ubah ubah;

        PenontonTeks(Ubah ubah) {
            this.ubah = ubah;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {}

        @Override
        public void afterTextChanged(Editable s) {
            ubah.jadi(s);
        }
    }
}
