package id.lifeoffoods.ui.akun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import id.lifeoffoods.R;
import id.lifeoffoods.data.AkunPembeli;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.AkunDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.databinding.FragmentProfilBinding;
import id.lifeoffoods.databinding.ItemBarisTokoBinding;
import id.lifeoffoods.databinding.ItemUbinProfilBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.onboarding.AlergiFragment;
import id.lifeoffoods.ui.umum.KeluarAkun;
import id.lifeoffoods.ui.umum.SisiAman;

/** K18 Profil (PRD-19). Tab Profil di nav bawah konsumen. */
public class ProfilFragment extends Fragment {

    private FragmentProfilBinding binding;
    private AkunViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(AkunViewModel.class);

        // Header hijau sampai ke balik status bar.
        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.segarkan(true));

        binding.nav.tabProfil.setSelected(true);
        binding.nav.tabBeranda.setOnClickListener(v -> keBeranda());
        binding.nav.tabFavorit.setOnClickListener(v -> buka(R.id.k16_favorit, null));
        binding.nav.tabPesanan.setOnClickListener(v -> buka(R.id.k15_pesanan, null));

        ubin(binding.ubinPorsi, R.string.k18_porsi);
        ubin(binding.ubinHemat, R.string.k18_hemat);
        ubin(binding.ubinPesanan, R.string.k18_pesanan);

        baris(
                binding.barisEdit,
                R.drawable.ic_pengguna,
                R.string.k18_edit_profil,
                R.id.k19_edit_profil,
                null);
        Bundle dariProfil = new Bundle();
        dariProfil.putBoolean(AlergiFragment.ARG_DARI_PROFIL, true);
        baris(
                binding.barisAlergi,
                R.drawable.ic_peringatan,
                R.string.k18_alergi,
                R.id.k05_alergi,
                dariProfil);
        baris(
                binding.barisNotifikasi,
                R.drawable.ic_lonceng,
                R.string.k18_notifikasi,
                R.id.k17_notifikasi,
                null);
        baris(
                binding.barisPengaturan,
                R.drawable.ic_pengaturan,
                R.string.k20_judul,
                R.id.k20_pengaturan,
                null);
        binding.tombolPengaturan.setOnClickListener(v -> buka(R.id.k20_pengaturan, null));
        binding.tombolKeluar.setOnClickListener(v -> KeluarAkun.tanya(this));

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.saya.observe(getViewLifecycleOwner(), this::isi);
        vm.dampak.observe(getViewLifecycleOwner(), this::isiDampak);
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
        // Header hijau: ikon status bar putih. Kembali dari K19/K20: data ikut diperbarui.
        SisiAman.ikonGelap(requireActivity(), false);
        vm.segarkan(true);
    }

    private void tampilkanStatus() {
        AkunViewModel.Status s = vm.status.getValue();
        boolean siap = s == AkunViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == AkunViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == AkunViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == AkunViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
        SisiAman.ikonGelap(requireActivity(), !siap);
    }

    private void isi(@Nullable MeResponse m) {
        if (m == null || m.user == null) {
            return;
        }
        binding.nama.setText(m.user.name);
        binding.inisial.setText(FormatTampilan.inisial(m.user.name));
        muatFoto(binding.foto, m.user.photoUrl);
        isiSub();
        binding.barisAlergi.nilai.setVisibility(View.VISIBLE);
        binding.barisAlergi.nilai.setText(
                FotoProfil.ringkasAlergi(m.allergens, getString(R.string.k18_alergi_kosong)));
    }

    private void isiDampak(@Nullable AkunDto.Dampak d) {
        if (d == null) {
            return;
        }
        angka(binding.ubinPorsi, R.string.k18_porsi, String.valueOf(d.portionsRescued));
        angka(binding.ubinHemat, R.string.k18_hemat, AkunPembeli.rupiahRingkas(d.savedRupiah));
        angka(binding.ubinPesanan, R.string.k18_pesanan, String.valueOf(d.ordersCompleted));
        isiSub();
    }

    /** "Anggota sejak Maret 2026, SCBD, Jakarta Selatan". */
    private void isiSub() {
        AkunDto.Dampak d = vm.dampak.getValue();
        MeResponse.ConsumerProfile p = vm.profil();
        String sejak = d == null ? null : AkunPembeli.bulanTahun(d.memberSince);
        String area = p == null ? null : p.areaLabel;
        String teks;
        if (sejak == null) {
            teks = area == null ? "" : area;
        } else if (area == null || area.isEmpty()) {
            teks = getString(R.string.k18_anggota, sejak);
        } else {
            teks = getString(R.string.k18_anggota_area, sejak, area);
        }
        binding.sub.setText(teks);
        binding.sub.setVisibility(teks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void ubin(ItemUbinProfilBinding u, @StringRes int label) {
        u.label.setText(label);
        u.angka.setText("0");
    }

    private void angka(ItemUbinProfilBinding u, @StringRes int label, String nilai) {
        u.angka.setText(nilai);
        u.getRoot().setContentDescription(getString(R.string.k18_ubin, getString(label), nilai));
    }

    private void baris(
            ItemBarisTokoBinding b,
            @DrawableRes int ikon,
            @StringRes int label,
            int tujuan,
            @Nullable Bundle args) {
        b.ikon.setImageResource(ikon);
        b.label.setText(label);
        b.getRoot().setOnClickListener(v -> buka(tujuan, args));
    }

    /** Foto profil bulat; tanpa foto, inisial di bawahnya yang terlihat. */
    static void muatFoto(android.widget.ImageView v, @Nullable String url) {
        if (url == null || url.isEmpty()) {
            Glide.with(v).clear(v);
            v.setVisibility(View.GONE);
            return;
        }
        v.setVisibility(View.VISIBLE);
        Glide.with(v).load(url).transform(new CircleCrop()).into(v);
    }

    private void keBeranda() {
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.k07_beranda, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.k07_beranda, null, opsi);
    }

    private void buka(int tujuan, @Nullable Bundle args) {
        NavHostFragment.findNavController(this).navigate(tujuan, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
