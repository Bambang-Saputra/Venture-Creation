package id.lifeoffoods.ui.mitra;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.NomorHp;
import id.lifeoffoods.data.ProfilToko;
import id.lifeoffoods.data.api.model.AnggotaTokoDto;
import id.lifeoffoods.databinding.DialogUndangKasirBinding;
import id.lifeoffoods.databinding.FragmentPengaturanTokoBinding;
import id.lifeoffoods.databinding.ItemAnggotaTokoBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;

import java.util.List;

/** M15 Pengaturan toko dan kasir (PRD-15). Dari baris "Pengaturan toko dan kasir" di M14. */
public class PengaturanTokoFragment extends Fragment {

    private static final String PAKET_WHATSAPP = "com.whatsapp";

    private FragmentPengaturanTokoBinding binding;
    private PengaturanTokoViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPengaturanTokoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PengaturanTokoViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m15_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolUndang.setOnClickListener(v -> tanyaUndang());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.anggota.observe(getViewLifecycleOwner(), this::isi);
        vm.mengirim.observe(
                getViewLifecycleOwner(),
                m -> binding.tombolUndang.setEnabled(!Boolean.TRUE.equals(m)));
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.undangan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        bagikan(teks);
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
        PengaturanTokoViewModel.Status s = vm.status.getValue();
        boolean siap = s == PengaturanTokoViewModel.Status.SIAP;
        boolean bukanPemilik = s == PengaturanTokoViewModel.Status.BUKAN_PEMILIK;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == PengaturanTokoViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == PengaturanTokoViewModel.Status.GAGAL || bukanPemilik
                        ? View.VISIBLE
                        : View.GONE);
        binding.keadaan.tombolCobaLagi.setVisibility(bukanPemilik ? View.GONE : View.VISIBLE);
        if (bukanPemilik) {
            binding.keadaan.gagalIsi.setText(R.string.m15_bukan_pemilik);
        } else if (s == PengaturanTokoViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
    }

    private void isi(@Nullable List<AnggotaTokoDto> daftar) {
        binding.daftarAnggota.removeAllViews();
        if (daftar == null) {
            return;
        }
        for (AnggotaTokoDto a : daftar) {
            ItemAnggotaTokoBinding b =
                    ItemAnggotaTokoBinding.inflate(
                            getLayoutInflater(), binding.daftarAnggota, false);
            boolean pemilik = a.isStoreOwner || "owner".equals(a.role);
            String nomor = ProfilToko.nomorHp(a.phone);
            String peran = getString(pemilik ? R.string.m15_pemilik : R.string.m15_kasir);
            b.inisial.setText(FormatTampilan.inisial(a.name));
            b.nama.setText(a.name);
            b.nomor.setText(nomor);
            b.peran.setText(peran);
            b.peran.setBackgroundResource(
                    pemilik ? R.drawable.bg_chip_gelap : R.drawable.bg_pil_isian);
            b.peran.setTextColor(
                    ContextCompat.getColor(
                            requireContext(), pemilik ? R.color.teks_di_gelap : R.color.teks_kuat));
            b.getRoot()
                    .setContentDescription(
                            getString(R.string.m15_baris_anggota, a.name, nomor, peran));
            if (a.bisaDicabut()) {
                b.getRoot().setOnClickListener(v -> tanyaCabut(a));
                androidx.core.view.ViewCompat.replaceAccessibilityAction(
                        b.getRoot(),
                        androidx.core.view.accessibility.AccessibilityNodeInfoCompat
                                .AccessibilityActionCompat.ACTION_CLICK,
                        getString(R.string.m15_ketuk_cabut),
                        null);
            } else {
                b.getRoot().setClickable(false);
                b.getRoot().setBackground(null);
            }
            binding.daftarAnggota.addView(b.getRoot());
        }
    }

    private void tanyaUndang() {
        DialogUndangKasirBinding d = DialogUndangKasirBinding.inflate(getLayoutInflater());
        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.m15_undang_judul)
                        .setView(d.getRoot())
                        .setNegativeButton(R.string.batal, null)
                        .setPositiveButton(R.string.m15_undang_kirim, null)
                        .show();
        // Tombol Undang tidak menutup dialog selama isian belum benar.
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            String nama =
                                    d.nama.getText() == null
                                            ? ""
                                            : d.nama.getText().toString().trim();
                            String hp =
                                    NomorHp.normalisasi(
                                            d.hp.getText() == null
                                                    ? null
                                                    : d.hp.getText().toString());
                            d.isianNama.setError(
                                    nama.length() < 2 ? getString(R.string.k04_galat_nama) : null);
                            d.isianHp.setError(
                                    hp == null ? getString(R.string.m15_galat_hp) : null);
                            if (nama.length() < 2 || hp == null) {
                                return;
                            }
                            vm.undang(nama, hp);
                            dialog.dismiss();
                        });
    }

    private void tanyaCabut(AnggotaTokoDto a) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.m15_cabut_judul, a.name))
                .setMessage(getString(R.string.m15_cabut_isi, a.name))
                .setNegativeButton(R.string.batal, null)
                .setPositiveButton(R.string.m15_cabut, (dlg, w) -> vm.cabut(a))
                .show();
    }

    /** Undangan dibagikan lewat WhatsApp kalau terpasang, selain itu lewat pemilih aplikasi. */
    private void bagikan(String teks) {
        Intent kirim =
                new Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, teks);
        try {
            startActivity(new Intent(kirim).setPackage(PAKET_WHATSAPP));
        } catch (ActivityNotFoundException e) {
            startActivity(Intent.createChooser(kirim, getString(R.string.m15_undang_bagikan)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
