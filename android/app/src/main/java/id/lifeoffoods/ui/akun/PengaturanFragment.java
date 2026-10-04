package id.lifeoffoods.ui.akun;

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
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.ProfilBody;
import id.lifeoffoods.databinding.FragmentPengaturanBinding;
import id.lifeoffoods.databinding.ItemSakelarBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.onboarding.AlergiFragment;
import id.lifeoffoods.ui.umum.KeluarAkun;
import id.lifeoffoods.ui.umum.SisiAman;

/** K20 Pengaturan (PRD-19, F-19 hapus akun). Dari K18. */
public class PengaturanFragment extends Fragment {

    private FragmentPengaturanBinding binding;
    private AkunViewModel vm;

    /** Sakelar diubah dari kode saat mengisi data: jangan kirim PATCH. */
    private boolean mengisi;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPengaturanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(AkunViewModel.class);

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k20_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.segarkan(false));

        sakelar(binding.sakelarFavorit, R.string.k20_favorit, R.string.k20_favorit_ket, 0);
        sakelar(binding.sakelarPengingat, R.string.k20_pengingat, R.string.k20_pengingat_ket, 1);
        sakelar(binding.sakelarPromo, R.string.k20_promo, R.string.k20_promo_ket, 2);

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
        binding.tombolKeluar.setOnClickListener(v -> KeluarAkun.tanya(this));
        binding.tombolHapus.setOnClickListener(v -> tanyaHapus());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.saya.observe(getViewLifecycleOwner(), this::isi);
        vm.menyimpan.observe(
                getViewLifecycleOwner(),
                m -> binding.tombolHapus.setEnabled(!Boolean.TRUE.equals(m)));
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.akunTerhapus.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).kembaliKeAwal();
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
        vm.segarkan(false);
    }

    private void sakelar(
            ItemSakelarBinding s, @StringRes int judul, @StringRes int ket, int jenis) {
        s.judul.setText(judul);
        s.ket.setText(ket);
        s.sakelar.setContentDescription(getString(judul));
        s.sakelar.setOnCheckedChangeListener(
                (b, nyala) -> {
                    if (mengisi) {
                        return;
                    }
                    ProfilBody body = new ProfilBody();
                    if (jenis == 0) {
                        body.notifyFavoriteStore = nyala;
                    } else if (jenis == 1) {
                        body.notifyPickupReminder = nyala;
                    } else {
                        body.notifyPromo = nyala;
                    }
                    vm.ubahSakelar(body);
                });
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
    }

    private void isi(@Nullable MeResponse m) {
        if (m == null) {
            return;
        }
        MeResponse.ConsumerProfile p = m.consumerProfile;
        mengisi = true;
        binding.sakelarFavorit.sakelar.setChecked(p != null && p.notifyFavoriteStore);
        binding.sakelarPengingat.sakelar.setChecked(p != null && p.notifyPickupReminder);
        binding.sakelarPromo.sakelar.setChecked(p != null && p.notifyPromo);
        mengisi = false;
        binding.barisAlergi.nilai.setVisibility(View.VISIBLE);
        binding.barisAlergi.nilai.setText(
                FotoProfil.ringkasAlergi(m.allergens, getString(R.string.k18_alergi_kosong)));
    }

    private void tanyaHapus() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.k20_hapus_judul)
                .setMessage(R.string.k20_hapus_isi)
                .setNegativeButton(R.string.batal, null)
                .setPositiveButton(R.string.k20_hapus_ya, (d, w) -> vm.hapusAkun())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
