package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.CatatSisa;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.CatatanSisaDto;
import id.lifeoffoods.databinding.DialogItemLainBinding;
import id.lifeoffoods.databinding.FragmentCatatSisaBinding;
import id.lifeoffoods.databinding.ItemSisaProdukBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.jualan.Stepper;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * M06 Catat sisa dan M19 Catat sisa timbang (PRD-12). Baris produk dibangun sekali (bukan
 * RecyclerView) supaya kolom berat tidak kehilangan ketikan saat angka lain berubah.
 */
public class CatatSisaFragment extends Fragment {

    private static final Locale ID = new Locale("id", "ID");
    private static final DateTimeFormatter TANGGAL =
            DateTimeFormatter.ofPattern("EEEE, d MMMM", ID);
    private static final DateTimeFormatter HARI = DateTimeFormatter.ofPattern("EEEE", ID);

    private static final String[] TUJUAN = {
        CatatSisa.DIBUANG,
        CatatSisa.DISUMBANGKAN,
        CatatSisa.MAKAN_KARYAWAN,
        CatatSisa.TERJUAL_SURPLUS
    };

    private FragmentCatatSisaBinding binding;
    private CatatSisaViewModel vm;
    private final Map<Long, ItemSisaProdukBinding> baris = new HashMap<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCatatSisaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(CatatSisaViewModel.class);
        vm.muat();

        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        binding.segmen.tabTas.setText(R.string.m06_per_item);
        binding.segmen.tabMenu.setText(R.string.m06_timbang);
        binding.segmen.tabTas.setOnClickListener(v -> vm.pilihMode(CatatanSisaDto.PER_ITEM));
        binding.segmen.tabMenu.setOnClickListener(v -> vm.pilihMode(CatatanSisaDto.TIMBANG));
        binding.tombolTambah.setOnClickListener(v -> tambahItemLain());
        binding.tombolSimpan.setOnClickListener(v -> simpan(false));
        binding.tombolSimpanPasang.setOnClickListener(v -> simpan(true));

        binding.nav.tabCatat.setSelected(true);
        // Selama M05 belum dibuat, Beranda membuka M07 Laporan mingguan.
        binding.nav.tabBeranda.setOnClickListener(v -> buka(R.id.m07_laporan));
        binding.nav.tabPesanan.setOnClickListener(v -> buka(R.id.m11_pesanan));
        binding.nav.tabToko.setOnClickListener(v -> buka(R.id.m10_kelola));

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.catatan.observe(getViewLifecycleOwner(), this::bangunProduk);
        vm.itemLain.observe(getViewLifecycleOwner(), l -> bangunItemLain());
        vm.mode.observe(getViewLifecycleOwner(), m -> tampilkanMode());
        vm.versi.observe(getViewLifecycleOwner(), v -> perbarui());
        vm.menyimpan.observe(getViewLifecycleOwner(), j -> perbaruiTombol());
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.tersimpan.observe(
                getViewLifecycleOwner(),
                p -> {
                    Boolean lanjut = p.ambil();
                    if (lanjut == null) {
                        return;
                    }
                    Snackbar.make(
                                    requireActivity().findViewById(android.R.id.content),
                                    R.string.m06_tersimpan,
                                    Snackbar.LENGTH_SHORT)
                            .show();
                    if (lanjut) {
                        buka(R.id.m09_pasang_tas);
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
        CatatSisaViewModel.Status s = vm.status.getValue();
        boolean siap = s == CatatSisaViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.aksi.setVisibility(siap ? View.VISIBLE : View.GONE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == CatatSisaViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == CatatSisaViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == CatatSisaViewModel.Status.GAGAL && vm.galatAwal.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galatAwal.getValue());
        }
        binding.tanggal.setText(tanggal(vm.tanggal(), TANGGAL));
    }

    private void bangunProduk(@Nullable CatatanSisaDto c) {
        binding.daftarProduk.removeAllViews();
        baris.clear();
        if (c == null) {
            return;
        }
        binding.terkunci.setVisibility(c.isLocked ? View.VISIBLE : View.GONE);
        List<CatatanSisaDto.Produk> produk = c.products;
        if (produk != null) {
            for (CatatanSisaDto.Produk p : produk) {
                ItemSisaProdukBinding b =
                        ItemSisaProdukBinding.inflate(
                                getLayoutInflater(), binding.daftarProduk, false);
                b.nama.setText(p.name);
                String satuan =
                        p.unit == null || p.unit.trim().isEmpty()
                                ? getString(R.string.m06_satuan_bawaan)
                                : p.unit.trim();
                b.nilai.setText(
                        getString(
                                R.string.m06_nilai_satuan,
                                FormatTampilan.rupiah(p.unitValueRupiah),
                                satuan));
                b.berat.setText(CatatSisa.isianKg(vm.gram(p.productId)));
                b.berat.setContentDescription(getString(R.string.m19_berat_label, p.name));
                b.berat.addTextChangedListener(
                        new PasangTasFragment.PenontonTeks(
                                s -> {
                                    int g = CatatSisa.gram(s);
                                    b.berat.setError(
                                            g < 0 ? getString(R.string.m19_berat_galat) : null);
                                    vm.ubahGram(p.productId, g);
                                }));
                b.tujuan.setOnClickListener(v -> pilihTujuan(p));
                binding.daftarProduk.addView(b.getRoot());
                baris.put(p.productId, b);
            }
        }
        perbarui();
    }

    private void bangunItemLain() {
        binding.daftarLain.removeAllViews();
        List<CatatSisaViewModel.ItemLain> lain = vm.itemLain.getValue();
        if (lain == null) {
            return;
        }
        for (int i = 0; i < lain.size(); i++) {
            CatatSisaViewModel.ItemLain l = lain.get(i);
            ItemSisaProdukBinding b =
                    ItemSisaProdukBinding.inflate(getLayoutInflater(), binding.daftarLain, false);
            b.nama.setText(l.label);
            b.nilai.setText(getString(R.string.m06_lain_ket, FormatTampilan.rupiah(l.nilaiSatuan)));
            b.stepper.getRoot().setVisibility(View.GONE);
            b.kotakBerat.setVisibility(View.GONE);
            // Item lain ditampilkan apa adanya; pil berisi jumlah dan tanda hapus.
            String banyak =
                    vm.timbang()
                            ? CatatSisa.kg(l.gram)
                            : getResources()
                                    .getQuantityString(R.plurals.m06_item, l.jumlah, l.jumlah);
            b.tujuan.setText(getString(R.string.m06_lain_hapus, banyak));
            b.tujuan.setContentDescription(getString(R.string.m06_hapus_item, l.label));
            int posisi = i;
            b.tujuan.setOnClickListener(
                    v -> {
                        if (!vm.terkunci()) {
                            vm.hapusItemLain(posisi);
                        }
                    });
            binding.daftarLain.addView(b.getRoot());
        }
    }

    private void tampilkanMode() {
        boolean timbang = vm.timbang();
        binding.segmen.tabTas.setSelected(!timbang);
        binding.segmen.tabMenu.setSelected(timbang);
        binding.keterangan.setText(timbang ? R.string.m19_ket : R.string.m06_ket);
        binding.labelRingkasan.setText(
                timbang ? R.string.m19_berat_terbuang : R.string.m06_nilai_terbuang);
        binding.tipsTimbang.setVisibility(timbang ? View.VISIBLE : View.GONE);
        bangunItemLain();
        perbarui();
    }

    /** Baris dan ringkasan setiap kali angka berubah. Kolom berat tidak disentuh. */
    private void perbarui() {
        if (binding == null) {
            return;
        }
        boolean timbang = vm.timbang();
        boolean kunci = vm.terkunci();
        CatatanSisaDto c = vm.catatan.getValue();
        if (c != null && c.products != null) {
            for (CatatanSisaDto.Produk p : c.products) {
                ItemSisaProdukBinding b = baris.get(p.productId);
                if (b == null) {
                    continue;
                }
                long id = p.productId;
                int j = vm.jumlah(id);
                b.stepper.getRoot().setVisibility(timbang ? View.GONE : View.VISIBLE);
                b.kotakBerat.setVisibility(timbang ? View.VISIBLE : View.GONE);
                Stepper.isi(
                        b.stepper,
                        j,
                        !kunci && j > 0,
                        !kunci && j < CatatSisa.JUMLAH_MAKS,
                        () -> vm.ubahJumlah(id, false),
                        () -> vm.ubahJumlah(id, true));
                b.berat.setEnabled(!kunci);
                b.tujuan.setEnabled(!kunci);
                b.tujuan.setText(
                        getString(R.string.m06_tujuan_label, getString(teksTujuan(vm.tujuan(id)))));
            }
        }
        if (timbang) {
            binding.nilaiRingkasan.setText(CatatSisa.kg(vm.gramTerbuang()));
            binding.subRingkasan.setText(
                    c != null && c.isRecorded && CatatanSisaDto.TIMBANG.equals(c.method)
                            ? getString(
                                    R.string.m19_nilai_tersimpan,
                                    FormatTampilan.rupiah(c.totalValueRupiah))
                            : getString(R.string.m19_nilai_nanti));
        } else {
            binding.nilaiRingkasan.setText(FormatTampilan.rupiah(vm.nilaiTerbuang()));
            int n = vm.itemTerbuang();
            String sub = getResources().getQuantityString(R.plurals.m06_item, n, n);
            String banding = banding(c);
            binding.subRingkasan.setText(banding == null ? sub : sub + " · " + banding);
        }
        binding.tombolTambah.setVisibility(kunci ? View.GONE : View.VISIBLE);
        perbaruiTombol();
    }

    /** "naik 12% dari Kamis lalu" dari server; "belum disimpan" kalau hari ini belum dicatat. */
    @Nullable
    private String banding(@Nullable CatatanSisaDto c) {
        if (c == null || !c.isRecorded) {
            return getString(R.string.m06_belum_disimpan);
        }
        Integer persen = c.changeVsLastWeekPercent;
        if (persen == null) {
            return null;
        }
        String hari = tanggal(c.logDate, HARI);
        if (persen > 0) {
            return getString(R.string.m06_naik, persen, hari);
        }
        if (persen < 0) {
            return getString(R.string.m06_turun, -persen, hari);
        }
        return getString(R.string.m06_sama, hari);
    }

    /** Mode timbang: jangan simpan kalau ada berat yang tidak terbaca (tersimpan sebagai 0). */
    private void simpan(boolean lanjutPasang) {
        if (vm.timbang()) {
            for (ItemSisaProdukBinding b : baris.values()) {
                if (b.berat.getError() != null) {
                    b.berat.requestFocus();
                    Snackbar.make(binding.getRoot(), R.string.m19_berat_galat, Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }
            }
        }
        vm.simpan(lanjutPasang);
    }

    private void perbaruiTombol() {
        boolean jalan = Boolean.TRUE.equals(vm.menyimpan.getValue());
        boolean aktif = !jalan && !vm.terkunci();
        binding.tombolSimpan.setEnabled(aktif);
        binding.tombolSimpanPasang.setEnabled(aktif);
        binding.tombolSimpanPasang.setText(
                jalan ? R.string.m06_menyimpan : R.string.m06_simpan_pasang);
    }

    private void pilihTujuan(CatatanSisaDto.Produk p) {
        CharSequence[] label = new CharSequence[TUJUAN.length];
        int pilih = 0;
        for (int i = 0; i < TUJUAN.length; i++) {
            label[i] = getString(teksTujuan(TUJUAN[i]));
            if (TUJUAN[i].equals(vm.tujuan(p.productId))) {
                pilih = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.m06_pilih_tujuan, p.name))
                .setSingleChoiceItems(
                        label,
                        pilih,
                        (d, i) -> {
                            vm.ubahTujuan(p.productId, TUJUAN[i]);
                            d.dismiss();
                        })
                .show();
    }

    private void tambahItemLain() {
        DialogItemLainBinding d = DialogItemLainBinding.inflate(getLayoutInflater());
        boolean timbang = vm.timbang();
        d.isianBanyak.setHint(timbang ? R.string.m06_dialog_berat : R.string.m06_dialog_jumlah);
        d.banyakLain.setInputType(
                timbang
                        ? android.text.InputType.TYPE_CLASS_NUMBER
                                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                        : android.text.InputType.TYPE_CLASS_NUMBER);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.m06_dialog_judul)
                .setView(d.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.m06_dialog_tambah,
                        (dialog, w) -> {
                            String nama =
                                    d.namaLain.getText() == null
                                            ? ""
                                            : d.namaLain.getText().toString().trim();
                            CharSequence banyak = d.banyakLain.getText();
                            int jumlah =
                                    timbang
                                            ? 0
                                            : (int)
                                                    Math.min(
                                                            CatatSisa.JUMLAH_MAKS,
                                                            PasangTasFragment.angka(
                                                                    banyak == null ? "" : banyak));
                            int gram = timbang ? CatatSisa.gram(banyak) : 0;
                            long nilai =
                                    PasangTasFragment.angka(
                                            d.nilaiLain.getText() == null
                                                    ? ""
                                                    : d.nilaiLain.getText());
                            if (nama.isEmpty() || (timbang ? gram <= 0 : jumlah <= 0)) {
                                Snackbar.make(
                                                binding.getRoot(),
                                                R.string.m06_dialog_galat,
                                                Snackbar.LENGTH_LONG)
                                        .show();
                                return;
                            }
                            vm.tambahItemLain(
                                    new CatatSisaViewModel.ItemLain(nama, jumlah, gram, nilai));
                        })
                .show();
    }

    @StringRes
    private static int teksTujuan(String t) {
        switch (t) {
            case CatatSisa.DISUMBANGKAN:
                return R.string.m06_tujuan_disumbangkan;
            case CatatSisa.MAKAN_KARYAWAN:
                return R.string.m06_tujuan_karyawan;
            case CatatSisa.TERJUAL_SURPLUS:
                return R.string.m06_tujuan_surplus;
            default:
                return R.string.m06_tujuan_dibuang;
        }
    }

    private static String tanggal(@Nullable String iso, DateTimeFormatter f) {
        if (iso == null) {
            return "";
        }
        try {
            String s = LocalDate.parse(iso).format(f);
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    private void buka(int tujuan) {
        NavHostFragment.findNavController(this).navigate(tujuan);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baris.clear();
        binding = null;
    }
}
