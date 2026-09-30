package id.lifeoffoods.ui.mitra;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.databinding.FragmentCocokkanKodeBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;

/**
 * M12 Cocokkan kode pickup (PRD-11). Enam kotak hanyalah tampilan; ketikan masuk ke satu EditText,
 * sama dengan K03. Kode ditukar otomatis saat karakter keenam diketik, atau lewat tombol.
 */
public class CocokkanKodeFragment extends Fragment {

    private FragmentCocokkanKodeBinding binding;
    private CocokkanKodeViewModel vm;
    private TextView[] kotak;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCocokkanKodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(CocokkanKodeViewModel.class);

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m12_judul);
        binding.kepala.tombolKembali.setOnClickListener(v -> kembali());
        binding.tabPindai.setOnClickListener(
                v -> Snackbar.make(view, R.string.m12_pindai_belum, Snackbar.LENGTH_LONG).show());

        binding.barisPelanggan.label.setText(R.string.m12_pelanggan);
        binding.barisPesanan.label.setText(R.string.m12_pesanan);
        binding.barisBayar.label.setText(R.string.m12_terima_bayar);
        binding.barisJam.label.setText(R.string.m12_jam_ambil);
        binding.barisBayar.nilai.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.teks_merek));

        kotak =
                new TextView[] {
                    binding.kotak1, binding.kotak2, binding.kotak3,
                    binding.kotak4, binding.kotak5, binding.kotak6
                };
        binding.barisKotak.setOnClickListener(v -> fokusKeKode());
        binding.kode.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

                    @Override
                    public void onTextChanged(CharSequence s, int a, int b, int c) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String kode = PesananMitra.rapikanKode(s);
                        tampilkanKode(kode);
                        vm.kodeBerubah(kode);
                        if (kode.length() == PesananMitra.PANJANG_KODE
                                && vm.hasil.getValue() == null) {
                            vm.tukar(kode);
                        }
                    }
                });
        tampilkanKode(kodeSekarang());

        binding.tombolUtama.setOnClickListener(
                v -> {
                    if (vm.hasil.getValue() != null) {
                        kembali();
                    } else {
                        vm.tukar(kodeSekarang());
                    }
                });
        binding.tombolLain.setOnClickListener(
                v -> {
                    vm.ulang();
                    binding.kode.setText("");
                    fokusKeKode();
                });

        vm.menukar.observe(
                getViewLifecycleOwner(),
                jalan -> {
                    binding.progres.setVisibility(jalan ? View.VISIBLE : View.GONE);
                    perbaruiTombol();
                });
        vm.galatKode.observe(
                getViewLifecycleOwner(),
                g -> {
                    binding.galatKode.setText(g);
                    binding.galatKode.setVisibility(g == null ? View.GONE : View.VISIBLE);
                    for (TextView t : kotak) {
                        t.setActivated(g != null);
                    }
                });
        vm.hasil.observe(getViewLifecycleOwner(), this::isiHasil);
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });

        if (savedInstanceState == null) {
            fokusKeKode();
        }
    }

    private void isiHasil(@Nullable PesananMitraDto p) {
        binding.hasil.setVisibility(p == null ? View.GONE : View.VISIBLE);
        perbaruiTombol();
        if (p == null) {
            return;
        }
        sembunyikanKeyboard();
        binding.barisPelanggan.nilai.setText(p.buyerName);
        binding.barisPesanan.nilai.setText(PesananMitra.ringkasItem(p.items));
        binding.barisBayar.nilai.setText(
                getString(
                        R.string.m12_bayar_nilai,
                        FormatTampilan.rupiah(p.totalRupiah),
                        TampilanPesananMitra.caraBayar(requireContext(), p.paymentMethod)));
        binding.barisJam.nilai.setText(FormatTampilan.rentangJam(p.pickupStart, p.pickupEnd));
        TampilanPesananMitra.isiCatatan(requireContext(), binding.catatan, p);
    }

    private void perbaruiTombol() {
        boolean selesai = vm.hasil.getValue() != null;
        boolean jalan = Boolean.TRUE.equals(vm.menukar.getValue());
        // Kode dikunci selama ditukar dan setelah cocok; "Kode lain" membukanya lagi.
        binding.kode.setEnabled(!jalan && !selesai);
        binding.barisKotak.setEnabled(!jalan && !selesai);
        binding.tombolLain.setVisibility(selesai ? View.VISIBLE : View.GONE);
        binding.tombolUtama.setEnabled(!jalan);
        binding.tombolUtama.setText(
                selesai
                        ? R.string.m12_selesai
                        : jalan ? R.string.m12_menukar : R.string.m12_tandai);
        if (selesai) {
            binding.tombolUtama.setIcon(null);
        } else {
            binding.tombolUtama.setIconResource(R.drawable.ic_centang);
        }
    }

    private void tampilkanKode(String kode) {
        for (int i = 0; i < kotak.length; i++) {
            kotak[i].setText(i < kode.length() ? String.valueOf(kode.charAt(i)) : "");
            int aktif = Math.min(kode.length(), kotak.length - 1);
            kotak[i].setSelected(i == aktif);
            kotak[i].setEnabled(i <= aktif);
        }
    }

    private String kodeSekarang() {
        return PesananMitra.rapikanKode(binding.kode.getText());
    }

    private void fokusKeKode() {
        binding.kode.requestFocus();
        InputMethodManager imm =
                (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.kode, InputMethodManager.SHOW_IMPLICIT);
    }

    private void sembunyikanKeyboard() {
        InputMethodManager imm =
                (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.kode.getWindowToken(), 0);
    }

    private void kembali() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        kotak = null;
    }
}
