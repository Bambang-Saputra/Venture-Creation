package id.lifeoffoods.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.databinding.FragmentLengkapiProfilBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BantuanIsian;
import id.lifeoffoods.ui.umum.SisiAman;

/**
 * K04 Lengkapi profil, langkah 1 dari 2 sesudah akun konsumen baru dibuat. Ini tujuan awal graf,
 * jadi tombol kembali menutup aplikasi seperti tombol kembali sistem; saat dibuka lagi aplikasi
 * kembali ke sini sampai profil tersimpan (SesiPengguna.perluProfil).
 */
public class LengkapiProfilFragment extends Fragment {

    private FragmentLengkapiProfilBinding binding;
    private LengkapiProfilViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLengkapiProfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(LengkapiProfilViewModel.class);
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);
        BantuanIsian bantuanNama = new BantuanIsian(binding.isianNama, binding.bantuanNama);
        BantuanIsian bantuanEmail = new BantuanIsian(binding.isianEmail, binding.bantuanEmail);
        BantuanIsian bantuanArea = new BantuanIsian(binding.isianArea, binding.bantuanArea);

        binding.kepala.judul.setText(R.string.k04_judul_bar);
        binding.kepala.subjudul.setText(R.string.k04_langkah);
        binding.kepala.subjudul.setVisibility(View.VISIBLE);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.tombolLanjut.setOnClickListener(v -> simpan());
        binding.area.setOnEditorActionListener(
                (v, aksi, e) -> {
                    if (aksi == EditorInfo.IME_ACTION_DONE) {
                        simpan();
                        return true;
                    }
                    return false;
                });

        vm.muat();
        vm.isiAwal.observe(
                getViewLifecycleOwner(),
                p -> {
                    MeResponse me = p.ambil();
                    if (me != null) {
                        isiKalauKosong(binding.nama, me.user.name);
                        isiKalauKosong(binding.email, me.user.email);
                        if (me.consumerProfile != null) {
                            isiKalauKosong(binding.area, me.consumerProfile.areaLabel);
                        }
                    }
                });
        vm.memuat.observe(
                getViewLifecycleOwner(),
                memuat -> {
                    binding.tombolLanjut.setEnabled(!memuat);
                    binding.progres.setVisibility(memuat ? View.VISIBLE : View.GONE);
                });
        vm.galatNama.observe(getViewLifecycleOwner(), bantuanNama::galat);
        vm.galatEmail.observe(getViewLifecycleOwner(), bantuanEmail::galat);
        vm.galatArea.observe(getViewLifecycleOwner(), bantuanArea::galat);
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
                        NavHostFragment.findNavController(this).navigate(R.id.ke_alergi);
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

    private void simpan() {
        vm.simpan(teks(binding.nama), teks(binding.email), teks(binding.area));
    }

    /** Jangan menimpa yang sudah diketik pengguna kalau respons GET /me datang belakangan. */
    private static void isiKalauKosong(EditText kolom, @Nullable String nilai) {
        if (nilai != null && teks(kolom).isEmpty()) {
            kolom.setText(nilai);
        }
    }

    private static String teks(EditText kolom) {
        CharSequence t = kolom.getText();
        return t == null ? "" : t.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
