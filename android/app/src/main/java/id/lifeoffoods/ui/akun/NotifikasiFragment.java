package id.lifeoffoods.ui.akun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.AkunPembeli;
import id.lifeoffoods.data.RiwayatMitra;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.databinding.FragmentNotifikasiBinding;
import id.lifeoffoods.databinding.ItemNotifikasiBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.pesanan.KodePickupFragment;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** K17 Notifikasi (PRD-18). Dari lonceng K07 dan dari K18. */
public class NotifikasiFragment extends Fragment {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    private FragmentNotifikasiBinding binding;
    private NotifikasiViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentNotifikasiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(NotifikasiViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k17_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolMuatLagi.setOnClickListener(v -> vm.muatLagi());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.daftar.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.adaBerikutnya.observe(getViewLifecycleOwner(), a -> tampilkan());
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG).show();
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

    private void tampilkan() {
        if (binding == null) {
            return;
        }
        NotifikasiViewModel.Status s = vm.status.getValue();
        boolean siap = s == NotifikasiViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == NotifikasiViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == NotifikasiViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == NotifikasiViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
        if (!siap) {
            return;
        }
        List<NotifikasiResponse.Notifikasi> d = vm.daftar.getValue();
        binding.daftar.removeAllViews();
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(kosong ? View.VISIBLE : View.GONE);
        binding.tombolMuatLagi.setVisibility(
                Boolean.TRUE.equals(vm.adaBerikutnya.getValue()) ? View.VISIBLE : View.GONE);
        if (kosong) {
            return;
        }
        OffsetDateTime sekarang = OffsetDateTime.now(WIB);
        LocalDate hariIni = sekarang.toLocalDate();
        String sebelumnya = null;
        for (NotifikasiResponse.Notifikasi n : d) {
            ItemNotifikasiBinding b =
                    ItemNotifikasiBinding.inflate(getLayoutInflater(), binding.daftar, false);
            boolean awal = sebelumnya == null || RiwayatMitra.awalKelompok(sebelumnya, n.createdAt);
            LocalDate tgl = RiwayatMitra.tanggal(n.createdAt);
            b.judulHari.setVisibility(awal && tgl != null ? View.VISIBLE : View.GONE);
            if (tgl != null) {
                b.judulHari.setText(RiwayatMitra.judulHari(tgl, hariIni));
            }
            sebelumnya = n.createdAt;

            b.ikon.setImageResource(ikon(n.type));
            b.judul.setText(n.title);
            b.isi.setVisibility(n.body == null || n.body.isEmpty() ? View.GONE : View.VISIBLE);
            b.isi.setText(n.body);
            String waktu = AkunPembeli.waktuNotifikasi(n.createdAt, sekarang);
            b.waktu.setText(waktu);
            b.baris.setContentDescription(
                    getString(R.string.k17_baris, n.title, n.body == null ? "" : n.body, waktu));

            long idPesanan = n.idPesanan();
            if (idPesanan > 0) {
                b.baris.setOnClickListener(v -> bukaPesanan(idPesanan));
            } else {
                b.baris.setClickable(false);
                b.baris.setForeground(null);
            }
            binding.daftar.addView(b.getRoot());
        }
    }

    private void bukaPesanan(long id) {
        Bundle a = new Bundle();
        a.putLong(KodePickupFragment.ARG_ID_PESANAN, id);
        NavHostFragment.findNavController(this).navigate(R.id.k14_kode_pickup, a);
    }

    @DrawableRes
    private static int ikon(@Nullable String tipe) {
        if (NotifikasiResponse.Notifikasi.MITRA_FAVORIT.equals(tipe)) {
            return R.drawable.ic_hati;
        }
        if (NotifikasiResponse.Notifikasi.PORSI.equals(tipe)) {
            return R.drawable.ic_daun;
        }
        if (NotifikasiResponse.Notifikasi.SIAP_DIAMBIL.equals(tipe)) {
            return R.drawable.ic_tas;
        }
        return R.drawable.ic_lonceng;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
