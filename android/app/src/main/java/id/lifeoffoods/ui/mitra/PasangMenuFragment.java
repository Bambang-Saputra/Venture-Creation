package id.lifeoffoods.ui.mitra;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.ProdukDto;
import id.lifeoffoods.databinding.DialogMenuBaruBinding;
import id.lifeoffoods.databinding.FragmentPasangMenuBinding;
import id.lifeoffoods.databinding.ItemProdukMenuBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.jualan.Stepper;
import id.lifeoffoods.ui.umum.Pil;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M16 Pasang menu satuan (PRD-09). Baris produk dibangun sekali (bukan RecyclerView) supaya kolom
 * harga tidak kehilangan ketikan saat daftar digambar ulang. Setelah terbit kembali ke M17.
 */
public class PasangMenuFragment extends Fragment {

    private FragmentPasangMenuBinding binding;
    private PasangMenuViewModel vm;
    private final Map<Long, ItemProdukMenuBinding> baris = new HashMap<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPasangMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PasangMenuViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.aksi.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m16_judul_bar);
        binding.kepala.subjudul.setText(R.string.m16_subjudul);
        binding.kepala.subjudul.setVisibility(View.VISIBLE);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        binding.segmen.tabTas.setSelected(false);
        binding.segmen.tabMenu.setSelected(true);
        binding.segmen.tabTas.setOnClickListener(v -> keTas());
        binding.labelToko.ketHalal.setText(R.string.m16_halal_ket);
        binding.tombolMenuBaru.setOnClickListener(v -> tanyaMenuBaru());

        TampilanPasang.pasangJam(this, binding.jam, vm);
        TampilanPasang.pasangLabel(this, binding.labelToko, vm);
        binding.aksi.tombolTerbit.setText(R.string.m16_terbitkan);
        binding.aksi.tombolTerbit.setOnClickListener(v -> terbitkan());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.produk.observe(getViewLifecycleOwner(), this::bangunProduk);
        vm.versi.observe(getViewLifecycleOwner(), v -> perbaruiBaris());
        vm.alergen.observe(getViewLifecycleOwner(), a -> perbaruiBaris());
        vm.dapurKacang.observe(getViewLifecycleOwner(), a -> perbaruiBaris());
        vm.galatField.observe(
                getViewLifecycleOwner(),
                g -> {
                    TampilanPasang.tampilGalat(binding.galatItem, g, "items", "allergens");
                    TampilanPasang.tampilGalat(
                            binding.jam.galatJam, g, "pickup_end", "pickup_start");
                });
        vm.mengirim.observe(
                getViewLifecycleOwner(),
                jalan -> {
                    binding.aksi.tombolTerbit.setEnabled(!jalan);
                    binding.aksi.tombolTerbit.setText(
                            jalan ? R.string.m09_menerbitkan : R.string.m16_terbitkan);
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

    private void bangunProduk(@Nullable List<ProdukDto> daftar) {
        binding.daftarProduk.removeAllViews();
        baris.clear();
        boolean kosong = daftar == null || daftar.isEmpty();
        binding.kosong.setVisibility(kosong ? View.VISIBLE : View.GONE);
        if (kosong) {
            return;
        }
        for (ProdukDto p : daftar) {
            ItemProdukMenuBinding b =
                    ItemProdukMenuBinding.inflate(getLayoutInflater(), binding.daftarProduk, false);
            b.nama.setText(p.name);
            boolean adaKandungan = p.ingredientsText != null && !p.ingredientsText.trim().isEmpty();
            b.kandungan.setText(adaKandungan ? p.ingredientsText.trim() : null);
            b.kandungan.setVisibility(adaKandungan ? View.VISIBLE : View.GONE);
            b.hargaNormal.setText(p.priceRupiah > 0 ? FormatTampilan.rupiah(p.priceRupiah) : null);
            b.hargaNormal.setPaintFlags(
                    b.hargaNormal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            long h = vm.harga(p.id);
            if (h > 0) {
                b.harga.setText(String.valueOf(h));
            }
            b.harga.addTextChangedListener(
                    new PasangTasFragment.PenontonTeks(
                            s -> vm.ubahHarga(p.id, PasangTasFragment.angka(s))));
            b.tombolAlergen.setOnClickListener(v -> pilihAlergen(p));
            binding.daftarProduk.addView(b.getRoot());
            baris.put(p.id, b);
        }
        perbaruiBaris();
    }

    private void perbaruiBaris() {
        List<ProdukDto> daftar = vm.produk.getValue();
        if (daftar == null || binding == null) {
            return;
        }
        Map<String, String> nama = namaAlergen();
        boolean kacang = Boolean.TRUE.equals(vm.dapurKacang.getValue());
        for (ProdukDto p : daftar) {
            ItemProdukMenuBinding b = baris.get(p.id);
            if (b == null) {
                continue;
            }
            int s = vm.stok(p.id);
            Stepper.isi(
                    b.stepper,
                    s,
                    s > 0,
                    s < JualanMitra.JUMLAH_MAKS,
                    () -> vm.ubahStok(p.id, false),
                    () -> vm.ubahStok(p.id, true));
            b.barisJual.setAlpha(s > 0 ? 1f : 0.55f);

            b.label.removeAllViews();
            for (JualanMitraDto.Alergen a : JualanMitra.alergen(vm.alergen(p.id), kacang)) {
                String n = nama.containsKey(a.code) ? nama.get(a.code) : a.code;
                boolean mungkin = JualanMitraDto.Alergen.MUNGKIN.equals(a.presence);
                b.label.addView(
                        Pil.buat(
                                requireContext(),
                                mungkin ? getString(R.string.m10_alergen_mungkin, n) : n,
                                mungkin ? R.drawable.bg_pil_proses : R.drawable.bg_pil_isian,
                                mungkin ? R.color.tanda_proses_teks : R.color.teks_kuat,
                                0));
            }
            boolean dinyatakan = vm.sudahDinyatakan(p.id);
            if (dinyatakan && vm.alergen(p.id).isEmpty()) {
                b.label.addView(
                        Pil.buat(
                                requireContext(),
                                getString(R.string.m09_tanpa_alergen),
                                R.drawable.bg_pil_merek,
                                R.color.tanda_hemat_teks,
                                0));
            }
            b.label.setVisibility(b.label.getChildCount() == 0 ? View.GONE : View.VISIBLE);
            b.tombolAlergen.setText(
                    dinyatakan ? R.string.m16_alergen_item : R.string.m16_alergen_belum);
        }
        binding.aksi.potensi.setText(FormatTampilan.rupiah(vm.potensi()));
    }

    private Map<String, String> namaAlergen() {
        Map<String, String> m = new HashMap<>();
        List<AlergenDto> d = vm.alergen.getValue();
        if (d != null) {
            for (AlergenDto a : d) {
                m.put(a.code, a.name);
            }
        }
        return m;
    }

    /**
     * Dialog pilihan ganda: alergen yang ada, atau pilihan terakhir "Tidak mengandung alergen
     * umum".
     */
    private void pilihAlergen(ProdukDto p) {
        List<AlergenDto> d =
                vm.alergen.getValue() == null ? Collections.emptyList() : vm.alergen.getValue();
        int n = d.size();
        CharSequence[] label = new CharSequence[n + 1];
        boolean[] pilih = new boolean[n + 1];
        Set<String> sekarang = vm.alergen(p.id);
        for (int i = 0; i < n; i++) {
            label[i] = d.get(i).name;
            pilih[i] = sekarang.contains(d.get(i).code);
        }
        label[n] = getString(R.string.m09_tanpa_alergen);
        pilih[n] = vm.sudahDinyatakan(p.id) && sekarang.isEmpty();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.m16_dialog_alergen, p.name))
                .setMultiChoiceItems(
                        label,
                        pilih,
                        (dialog, i, dipilih) -> {
                            pilih[i] = dipilih;
                            android.widget.ListView lv =
                                    ((androidx.appcompat.app.AlertDialog) dialog).getListView();
                            if (dipilih && i == n) {
                                // "Tidak mengandung" menghapus pilihan alergen lain.
                                for (int k = 0; k < n; k++) {
                                    pilih[k] = false;
                                    lv.setItemChecked(k, false);
                                }
                            } else if (dipilih) {
                                pilih[n] = false;
                                lv.setItemChecked(n, false);
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.m16_simpan,
                        (dialog, w) -> {
                            Set<String> kode = new LinkedHashSet<>();
                            for (int i = 0; i < n; i++) {
                                if (pilih[i]) {
                                    kode.add(d.get(i).code);
                                }
                            }
                            // Tanpa centang sama sekali belum dianggap pernyataan.
                            if (!kode.isEmpty() || pilih[n]) {
                                vm.simpanAlergen(p.id, kode);
                            }
                        })
                .show();
    }

    /** Dialog "Tambah menu baru"; tetap terbuka selama isian atau server masih menolak. */
    private void tanyaMenuBaru() {
        DialogMenuBaruBinding d = DialogMenuBaruBinding.inflate(getLayoutInflater());
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.m16_baru_judul)
                        .setView(d.getRoot())
                        .setNegativeButton(R.string.batal, null)
                        .setPositiveButton(R.string.m16_baru_simpan, null)
                        .show();
        Button simpan = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        simpan.setOnClickListener(
                v -> {
                    String nama = teks(d.nama);
                    long harga = PasangTasFragment.angka(d.harga.getText());
                    String kandungan = teks(d.kandungan);
                    d.isianNama.setError(
                            nama.length() < 2 ? getString(R.string.m16_baru_g_nama) : null);
                    d.isianHarga.setError(
                            harga < PasangMenuViewModel.HARGA_MIN_ITEM
                                    ? getString(R.string.m16_baru_g_harga)
                                    : null);
                    d.isianKandungan.setError(
                            kandungan.length() < 3
                                    ? getString(R.string.m16_baru_g_kandungan)
                                    : null);
                    if (nama.length() < 2
                            || harga < PasangMenuViewModel.HARGA_MIN_ITEM
                            || kandungan.length() < 3) {
                        return;
                    }
                    simpan.setEnabled(false);
                    simpan.setText(R.string.m16_baru_menyimpan);
                    vm.tambahMenu(
                            nama,
                            harga,
                            kandungan,
                            new PasangMenuViewModel.HasilMenuBaru() {
                                @Override
                                public void sukses(ProdukDto p) {
                                    dialog.dismiss();
                                    if (binding != null && isAdded()) {
                                        Snackbar.make(
                                                        binding.getRoot(),
                                                        getString(
                                                                R.string.m16_baru_tersimpan,
                                                                p.name),
                                                        Snackbar.LENGTH_LONG)
                                                .show();
                                    }
                                }

                                @Override
                                public void gagal(@Nullable String field, String pesan) {
                                    if (!isAdded() || !dialog.isShowing()) {
                                        return;
                                    }
                                    simpan.setEnabled(true);
                                    simpan.setText(R.string.m16_baru_simpan);
                                    String p =
                                            pesan == null || pesan.isEmpty()
                                                    ? getString(R.string.m16_baru_g_umum)
                                                    : pesan;
                                    if ("price_rupiah".equals(field)) {
                                        d.isianHarga.setError(p);
                                    } else if ("ingredients_text".equals(field)) {
                                        d.isianKandungan.setError(p);
                                    } else {
                                        d.isianNama.setError(p);
                                    }
                                }
                            });
                });
    }

    private static String teks(android.widget.EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void terbitkan() {
        Map<String, Integer> lokal = vm.terbitkan();
        if (lokal.isEmpty()) {
            return;
        }
        Map<String, String> pesan = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : lokal.entrySet()) {
            pesan.put(
                    e.getKey(),
                    e.getValue() == PasangMenuViewModel.G_HARGA_ITEM
                            ? getString(R.string.m16_g_harga)
                            : getString(TampilanPasang.teks(e.getValue(), true)));
        }
        vm.galatField.setValue(pesan);
        Snackbar.make(binding.getRoot(), R.string.m09_g_periksa, Snackbar.LENGTH_SHORT).show();
    }

    private void keTas() {
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.m16_pasang_menu, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.m09_pasang_tas, null, opsi);
    }

    private void selesai() {
        Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        R.string.m09_terbit,
                        Snackbar.LENGTH_LONG)
                .show();
        Bundle a = new Bundle();
        a.putString(KelolaJualanFragment.ARG_TIPE, JualanMitraDto.TIPE_MENU);
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.m10_kelola, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.m10_kelola, a, opsi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baris.clear();
        binding = null;
    }
}
