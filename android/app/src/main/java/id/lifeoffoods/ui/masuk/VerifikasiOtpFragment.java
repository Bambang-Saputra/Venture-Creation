package id.lifeoffoods.ui.masuk;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.NomorHp;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.databinding.FragmentVerifikasiOtpBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.Locale;

/**
 * K03 dan M02 Verifikasi OTP. Enam kotak hanyalah tampilan; ketikan masuk ke satu EditText
 * tersembunyi sehingga tempel, isi otomatis kode SMS, dan pembaca layar tetap bekerja.
 */
public class VerifikasiOtpFragment extends Fragment {

    public static final String ARG_NOMOR = "nomor";
    public static final String ARG_PERAN = "peran";
    public static final String ARG_JEDA = "jeda_kirim_ulang";
    public static final String ARG_KODE_UJI = "kode_uji";

    private FragmentVerifikasiOtpBinding binding;
    private VerifikasiOtpViewModel vm;
    private TextView[] kotak;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentVerifikasiOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);
        String nomor = args.getString(ARG_NOMOR, "");
        vm = new ViewModelProvider(this).get(VerifikasiOtpViewModel.class);
        vm.mulai(
                nomor,
                args.getString(ARG_PERAN, ""),
                args.getInt(ARG_JEDA, 60),
                args.getString(ARG_KODE_UJI));

        binding.kepala.judul.setText(R.string.k03_judul_bar);
        binding.kepala.tombolKembali.setOnClickListener(v -> kembali());
        binding.keterangan.setText(getString(R.string.k03_keterangan, NomorHp.tampilan(nomor)));
        binding.tombolUbahNomor.setOnClickListener(v -> kembali());

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
                        tampilkanKode(s.toString());
                        if (s.length() == VerifikasiOtpViewModel.PANJANG_KODE) {
                            vm.verifikasiOtomatis(s.toString());
                        }
                    }
                });
        tampilkanKode(binding.kode.getText() == null ? "" : binding.kode.getText().toString());

        binding.tombolVerifikasi.setOnClickListener(v -> vm.verifikasi(kodeSekarang()));
        binding.tombolKirimUlang.setOnClickListener(
                v -> {
                    binding.kode.setText("");
                    vm.kirimUlang();
                });
        // Mode uji coba: mengetuk spanduk mengisi kodenya, supaya demo di booth cepat.
        binding.spandukUji.setOnClickListener(
                v -> {
                    String k = vm.kodeUjiCoba.getValue();
                    if (k != null) {
                        binding.kode.setText(k);
                    }
                });

        vm.kodeUjiCoba.observe(
                getViewLifecycleOwner(),
                k -> {
                    binding.spandukUji.setVisibility(k == null ? View.GONE : View.VISIBLE);
                    if (k != null) {
                        binding.teksUji.setText(getString(R.string.k03_mode_uji, k));
                    }
                });
        vm.sisaDetik.observe(
                getViewLifecycleOwner(),
                d -> {
                    boolean boleh = d == 0;
                    binding.tombolKirimUlang.setEnabled(boleh);
                    binding.tombolKirimUlang.setText(
                            boleh
                                    ? getString(R.string.k03_kirim_ulang)
                                    : getString(
                                            R.string.k03_kirim_ulang_dalam,
                                            String.format(
                                                    Locale.ROOT, "%02d:%02d", d / 60, d % 60)));
                });
        vm.memuat.observe(
                getViewLifecycleOwner(),
                memuat -> {
                    binding.tombolVerifikasi.setEnabled(!memuat);
                    binding.progres.setVisibility(memuat ? View.VISIBLE : View.GONE);
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
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.masuk.observe(
                getViewLifecycleOwner(),
                p -> {
                    AuthResponse hasil = p.ambil();
                    if (hasil != null) {
                        sembunyikanKeyboard();
                        ((MainActivity) requireActivity()).selesaiMasuk(hasil);
                    }
                });

        if (savedInstanceState == null) {
            fokusKeKode();
        }
    }

    private void tampilkanKode(String kode) {
        for (int i = 0; i < kotak.length; i++) {
            kotak[i].setText(i < kode.length() ? String.valueOf(kode.charAt(i)) : "");
            // Kotak yang sedang diisi diberi garis hijau; kotak sesudahnya abu-abu tanpa garis,
            // seperti di Figma.
            int aktif = Math.min(kode.length(), kotak.length - 1);
            kotak[i].setSelected(i == aktif);
            kotak[i].setEnabled(i <= aktif);
        }
    }

    private String kodeSekarang() {
        return binding.kode.getText() == null ? "" : binding.kode.getText().toString();
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
