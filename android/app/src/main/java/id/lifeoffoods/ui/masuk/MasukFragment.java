package id.lifeoffoods.ui.masuk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.BuildConfig;
import id.lifeoffoods.R;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.databinding.FragmentMasukBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;

/**
 * K02 Masuk (konsumen) dan M01 Masuk mitra. Satu fragment, bedanya hanya argumen {@code peran} dan
 * teks. Keduanya mengarah ke aksi {@code ke_verifikasi} di graf masing-masing.
 */
public class MasukFragment extends Fragment {

    public static final String ARG_PERAN = "peran";

    private FragmentMasukBinding binding;
    private MasukViewModel vm;
    private String peran;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMasukBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        peran = requireArguments().getString(ARG_PERAN, SesiPengguna.PERAN_KONSUMEN);
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        vm = new ViewModelProvider(this).get(MasukViewModel.class);
        boolean mitra = SesiPengguna.PERAN_MITRA.equals(peran);

        binding.kepala.judul.setText(R.string.masuk_judul_bar);
        binding.label.setText(mitra ? R.string.m01_label : R.string.k02_label);
        binding.label.setBackgroundResource(
                mitra ? R.drawable.bg_chip_gelap : R.drawable.bg_ikon_mint);
        binding.label.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        mitra ? R.color.teks_di_gelap : R.color.tanda_hemat_teks));
        binding.judulUtama.setText(mitra ? R.string.m01_judul : R.string.k02_judul);
        binding.keterangan.setText(mitra ? R.string.m01_keterangan : R.string.k02_keterangan);
        String bantuan = getString(mitra ? R.string.m01_bantuan_nomor : R.string.k02_bantuan_nomor);
        binding.bantuanNomor.setText(bantuan);

        // Tombol kembali di layar pertama alur membawa ke K01, bukan menutup aplikasi.
        Runnable keAwal = () -> ((MainActivity) requireActivity()).kembaliKeAwal();
        binding.kepala.tombolKembali.setOnClickListener(v -> keAwal.run());
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                keAwal.run();
                            }
                        });

        binding.tombolKirim.setOnClickListener(v -> kirim());
        binding.nomor.setOnEditorActionListener(
                (v, aksi, e) -> {
                    if (aksi == EditorInfo.IME_ACTION_SEND) {
                        kirim();
                        return true;
                    }
                    return false;
                });

        // Tanpa Client ID Web, Credential Manager pasti gagal; tombol disembunyikan saja.
        boolean adaGoogle = !BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty();
        binding.pemisah.setVisibility(adaGoogle ? View.VISIBLE : View.GONE);
        binding.tombolGoogle.setVisibility(adaGoogle ? View.VISIBLE : View.GONE);
        binding.tombolGoogle.setOnClickListener(v -> masukGoogle());

        vm.memuat.observe(
                getViewLifecycleOwner(),
                memuat -> {
                    binding.tombolKirim.setEnabled(!memuat);
                    binding.tombolGoogle.setEnabled(!memuat);
                    binding.progres.setVisibility(memuat ? View.VISIBLE : View.GONE);
                });
        // Galat menggantikan teks bantuan di bawah kotak, dan garis kotak menjadi merah.
        vm.galatNomor.observe(
                getViewLifecycleOwner(),
                g -> {
                    binding.kotakNomor.setActivated(g != null);
                    binding.bantuanNomor.setText(g != null ? g : bantuan);
                    binding.bantuanNomor.setTextColor(
                            ContextCompat.getColor(
                                    requireContext(),
                                    g != null ? R.color.tanda_bahaya_teks : R.color.teks_sekunder));
                });
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.kodeTerkirim.observe(
                getViewLifecycleOwner(),
                p -> {
                    MasukViewModel.KodeTerkirim k = p.ambil();
                    if (k != null) {
                        Bundle args = new Bundle();
                        args.putString(VerifikasiOtpFragment.ARG_NOMOR, k.nomor);
                        args.putString(VerifikasiOtpFragment.ARG_PERAN, peran);
                        args.putInt(VerifikasiOtpFragment.ARG_JEDA, k.respons.resendIn);
                        args.putString(VerifikasiOtpFragment.ARG_KODE_UJI, k.respons.pilotCode);
                        NavHostFragment.findNavController(this).navigate(R.id.ke_verifikasi, args);
                    }
                });
        vm.masuk.observe(
                getViewLifecycleOwner(),
                p -> {
                    AuthResponse hasil = p.ambil();
                    if (hasil != null) {
                        ((MainActivity) requireActivity()).selesaiMasuk(hasil);
                    }
                });
    }

    private void kirim() {
        CharSequence teks = binding.nomor.getText();
        vm.mintaKode(teks == null ? "" : teks.toString(), peran);
    }

    /** ADR-0006: Credential Manager memberi ID token, server yang memverifikasinya. */
    private void masukGoogle() {
        GetCredentialRequest permintaan =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(
                                new GetSignInWithGoogleOption.Builder(
                                                BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                        .build())
                        .build();

        CredentialManager.create(requireContext())
                .getCredentialAsync(
                        requireActivity(),
                        permintaan,
                        null,
                        ContextCompat.getMainExecutor(requireContext()),
                        new CredentialManagerCallback<>() {
                            @Override
                            public void onResult(GetCredentialResponse hasil) {
                                if (!isAdded()) {
                                    return;
                                }
                                Credential c = hasil.getCredential();
                                String token = null;
                                if (c instanceof CustomCredential
                                        && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                                .equals(c.getType())) {
                                    try {
                                        token =
                                                GoogleIdTokenCredential.createFrom(c.getData())
                                                        .getIdToken();
                                    } catch (Exception e) {
                                        // GoogleIdTokenParsingException: data kredensial rusak.
                                        token = null;
                                    }
                                }
                                if (token != null) {
                                    vm.masukGoogle(token, peran);
                                } else {
                                    vm.tampilkanGalat(getString(R.string.google_gagal));
                                }
                            }

                            @Override
                            public void onError(@NonNull GetCredentialException e) {
                                if (!isAdded()) {
                                    return;
                                }
                                if (e instanceof NoCredentialException) {
                                    // Belum ada akun Google di perangkat, atau client ID tidak
                                    // cocok.
                                    vm.tampilkanGalat(getString(R.string.google_tanpa_akun));
                                } else if (!(e instanceof GetCredentialCancellationException)) {
                                    // Pengguna menutup pemilih akun bukan galat; sisanya ya.
                                    vm.tampilkanGalat(getString(R.string.google_gagal));
                                }
                            }
                        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
