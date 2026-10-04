package id.lifeoffoods.ui.akun;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.NomorHp;
import id.lifeoffoods.data.ValidasiProfil;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.databinding.FragmentEditProfilBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.onboarding.AlergiFragment;
import id.lifeoffoods.ui.umum.BantuanIsian;
import id.lifeoffoods.ui.umum.SisiAman;

/** K19 Edit profil (PRD-19). Dari K18. */
public class EditProfilFragment extends Fragment {

    private FragmentEditProfilBinding binding;
    private AkunViewModel vm;
    private BantuanIsian bantuanNama;
    private BantuanIsian bantuanEmail;
    private BantuanIsian bantuanArea;

    /**
     * Isian diisi dari server sekali saja, supaya ketikan pengguna tidak tertimpa saat dimuat
     * ulang.
     */
    private boolean sudahDiisi;

    private final ActivityResultLauncher<PickVisualMediaRequest> pilihFoto =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(), this::fotoDipilih);

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(AkunViewModel.class);
        sudahDiisi = savedInstanceState != null;

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k19_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.segarkan(false));

        bantuanNama = new BantuanIsian(binding.isianNama, binding.bantuanNama);
        bantuanEmail = new BantuanIsian(binding.isianEmail, binding.bantuanEmail);
        bantuanArea = new BantuanIsian(binding.isianArea, binding.bantuanArea);

        View.OnClickListener ganti =
                v ->
                        pilihFoto.launch(
                                new PickVisualMediaRequest.Builder()
                                        .setMediaType(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        .INSTANCE)
                                        .build());
        binding.tombolGantiFoto.setOnClickListener(ganti);
        binding.kotakFoto.setOnClickListener(ganti);

        binding.barisAlergi.ikon.setImageResource(R.drawable.ic_peringatan);
        binding.barisAlergi.label.setText(R.string.k18_alergi);
        binding.barisAlergi
                .getRoot()
                .setOnClickListener(
                        v -> {
                            Bundle a = new Bundle();
                            a.putBoolean(AlergiFragment.ARG_DARI_PROFIL, true);
                            NavHostFragment.findNavController(this).navigate(R.id.k05_alergi, a);
                        });
        binding.tombolSimpan.setOnClickListener(v -> simpan());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.saya.observe(getViewLifecycleOwner(), this::isi);
        vm.menyimpan.observe(
                getViewLifecycleOwner(),
                m -> {
                    boolean sibuk = Boolean.TRUE.equals(m);
                    binding.tombolSimpan.setEnabled(!sibuk);
                    binding.tombolGantiFoto.setEnabled(!sibuk);
                });
        vm.galatIsian.observe(getViewLifecycleOwner(), this::galatServer);
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.bilahAksi)
                                .show();
                    }
                });
        vm.tersimpan.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        NavHostFragment.findNavController(this).navigateUp();
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

    @Override
    public void onResume() {
        super.onResume();
        // Kembali dari K05: ringkasan alergi ikut diperbarui.
        vm.segarkan(false);
    }

    private void tampilkanStatus() {
        AkunViewModel.Status s = vm.status.getValue();
        boolean siap = s == AkunViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.bilahAksi.setVisibility(siap ? View.VISIBLE : View.GONE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == AkunViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == AkunViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == AkunViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
    }

    private void isi(@Nullable MeResponse m) {
        if (m == null || m.user == null) {
            return;
        }
        binding.inisial.setText(FormatTampilan.inisial(m.user.name));
        ProfilFragment.muatFoto(binding.foto, m.user.photoUrl);
        binding.nomorHp.setText(m.user.phone == null ? "" : NomorHp.tampilan(m.user.phone));
        binding.barisAlergi.nilai.setVisibility(View.VISIBLE);
        binding.barisAlergi.nilai.setText(
                FotoProfil.ringkasAlergi(m.allergens, getString(R.string.k18_alergi_kosong)));
        if (sudahDiisi) {
            return;
        }
        sudahDiisi = true;
        binding.nama.setText(m.user.name);
        binding.email.setText(m.user.email);
        binding.area.setText(m.consumerProfile == null ? null : m.consumerProfile.areaLabel);
    }

    private void simpan() {
        String nama = ValidasiProfil.nama(teks(binding.nama.getText()));
        String email = teks(binding.email.getText()).trim();
        boolean emailSah = ValidasiProfil.emailSah(email);
        bantuanNama.galat(nama == null ? getString(R.string.k04_galat_nama) : null);
        bantuanEmail.galat(emailSah ? null : getString(R.string.k04_galat_email));
        bantuanArea.galat(null);
        if (nama == null || !emailSah) {
            return;
        }
        vm.simpanProfil(nama, email, teks(binding.area.getText()).trim());
    }

    private void galatServer(@Nullable ApiError e) {
        if (e == null) {
            return;
        }
        bantuanNama.galat(e.pesanField("name"));
        bantuanEmail.galat(e.pesanField("email"));
        bantuanArea.galat(e.pesanField("area_label"));
    }

    private void fotoDipilih(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        // Membaca dan memperkecil foto kamera bisa ratusan milidetik: jangan di main thread.
        android.content.ContentResolver cr = requireContext().getContentResolver();
        new Thread(
                        () -> {
                            byte[] jpeg = FotoProfil.siapkan(cr, uri);
                            if (!isAdded()) {
                                return;
                            }
                            requireActivity()
                                    .runOnUiThread(
                                            () -> {
                                                if (binding == null) {
                                                    return;
                                                }
                                                if (jpeg == null) {
                                                    Snackbar.make(
                                                                    binding.getRoot(),
                                                                    R.string.k19_foto_gagal,
                                                                    Snackbar.LENGTH_LONG)
                                                            .show();
                                                    return;
                                                }
                                                vm.unggahFoto(jpeg);
                                            });
                        },
                        "foto-profil")
                .start();
    }

    private static String teks(@Nullable CharSequence c) {
        return c == null ? "" : c.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
