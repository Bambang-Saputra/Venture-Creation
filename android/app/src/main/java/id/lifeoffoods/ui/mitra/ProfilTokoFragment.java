package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.ProfilToko;
import id.lifeoffoods.data.api.model.TokoDetailDto;
import id.lifeoffoods.databinding.FragmentProfilTokoBinding;
import id.lifeoffoods.databinding.ItemBarisTokoBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.KeluarAkun;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/** M14 Profil toko (PRD-14): tab Toko di nav mitra. Pemilik dan kasir. */
public class ProfilTokoFragment extends Fragment {

    private FragmentProfilTokoBinding binding;
    private ProfilTokoViewModel vm;

    /** Sakelar diubah dari kode (isi data), bukan oleh pengguna: jangan kirim PATCH. */
    private boolean mengisi;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfilTokoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(ProfilTokoViewModel.class);

        // Foto toko sampai ke balik status bar, seperti header gelap M05.
        SisiAman.atas(binding.slotFoto);
        SisiAman.bawah(binding.nav.getRoot());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        NavMitra.pasang(this, binding.nav, R.id.m14_toko);

        siapkanBaris(
                binding.barisKelola,
                R.drawable.ic_tas,
                R.string.m14_kelola_jualan,
                R.id.m10_kelola);
        siapkanBaris(binding.barisSaldo, R.drawable.ic_dompet, R.string.m14_saldo, R.id.m13_saldo);
        siapkanBaris(
                binding.barisLaporan,
                R.drawable.ic_laporan,
                R.string.m14_laporan,
                R.id.m07_laporan);
        siapkanBaris(
                binding.barisPengaturan,
                R.drawable.ic_pengaturan,
                R.string.m14_pengaturan,
                R.id.m15_pengaturan);
        binding.tombolKeluar.setOnClickListener(v -> KeluarAkun.tanya(this));

        binding.sakelarTutup.setOnCheckedChangeListener(
                (b, tutup) -> {
                    if (!mengisi) {
                        vm.ubahTutupSementara(tutup);
                    }
                });

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.toko.observe(getViewLifecycleOwner(), this::isi);
        vm.menyimpan.observe(getViewLifecycleOwner(), m -> kunciSakelar());
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.nav.getRoot())
                                .show();
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
        SisiAman.ikonGelap(requireActivity(), true);
        vm.segarkan();
    }

    private void siapkanBaris(
            ItemBarisTokoBinding b, @DrawableRes int ikon, @StringRes int label, int tujuan) {
        b.ikon.setImageResource(ikon);
        b.label.setText(label);
        b.getRoot()
                .setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(tujuan));
    }

    private void tampilkanStatus() {
        ProfilTokoViewModel.Status s = vm.status.getValue();
        boolean siap = s == ProfilTokoViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == ProfilTokoViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == ProfilTokoViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == ProfilTokoViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
    }

    private void isi(@Nullable TokoDetailDto t) {
        if (t == null) {
            return;
        }
        FotoJualan.muat(binding.foto, t.photoUrl, 0);
        binding.ikonToko.setImageResource(ikonKategori(t.category));
        binding.namaToko.setText(t.name);
        binding.subToko.setText(
                getString(R.string.m14_sub_toko, getString(namaKategori(t.category)), t.address));

        String rating = ProfilToko.rating(t.ratingAverage, t.ratingCount);
        binding.rating.setVisibility(rating == null ? View.GONE : View.VISIBLE);
        if (rating != null) {
            binding.teksRating.setText(rating);
            binding.rating.setContentDescription(getString(R.string.m14_rating, rating));
        }

        isiJam(ProfilToko.ringkasJam(t.hours));
        mengisi = true;
        binding.sakelarTutup.setChecked(t.isTemporarilyClosed);
        mengisi = false;
        binding.ketTutup.setText(
                t.pemilik()
                        ? R.string.m14_tutup_sementara_ket
                        : R.string.m14_tutup_sementara_kasir);
        kunciSakelar();

        isiLabel(t);

        // Kasir: saldo, laporan, dan pengaturan kasir hanya untuk pemilik (M15).
        boolean pemilik = t.pemilik();
        binding.barisSaldo.getRoot().setVisibility(pemilik ? View.VISIBLE : View.GONE);
        binding.barisLaporan.getRoot().setVisibility(pemilik ? View.VISIBLE : View.GONE);
        binding.barisPengaturan.getRoot().setVisibility(pemilik ? View.VISIBLE : View.GONE);
        if (t.availableBalanceRupiah != null) {
            binding.barisSaldo.nilai.setVisibility(View.VISIBLE);
            binding.barisSaldo.nilai.setText(FormatTampilan.rupiah(t.availableBalanceRupiah));
        }
    }

    private void isiJam(List<ProfilToko.BarisJam> daftar) {
        binding.daftarJam.removeAllViews();
        binding.jamKosong.setVisibility(daftar.isEmpty() ? View.VISIBLE : View.GONE);
        for (ProfilToko.BarisJam b : daftar) {
            View baris =
                    getLayoutInflater().inflate(R.layout.item_baris_jam, binding.daftarJam, false);
            TextView hari = baris.findViewById(R.id.hari);
            TextView jam = baris.findViewById(R.id.jam);
            hari.setText(b.hari);
            jam.setText(b.jam == null ? getString(R.string.m14_jam_tutup) : b.jam);
            binding.daftarJam.addView(baris);
        }
    }

    private void isiLabel(TokoDetailDto t) {
        @StringRes int halal = 0;
        if ("certified".equals(t.halalLabel)) {
            halal = R.string.m14_halal_resmi;
        } else if ("self_claim".equals(t.halalLabel)) {
            halal = R.string.m14_halal_klaim;
        }
        binding.halal.setVisibility(halal == 0 ? View.GONE : View.VISIBLE);
        if (halal != 0) {
            binding.teksHalal.setText(halal);
        }
        String kandungan = t.defaultIngredientsText == null ? "" : t.defaultIngredientsText.trim();
        binding.kandungan.setText(
                kandungan.isEmpty()
                        ? getString(R.string.m14_kandungan_kosong)
                        : getString(R.string.m14_kandungan, kandungan));
    }

    private void kunciSakelar() {
        TokoDetailDto t = vm.toko.getValue();
        boolean bisa = t != null && t.pemilik() && !Boolean.TRUE.equals(vm.menyimpan.getValue());
        binding.sakelarTutup.setEnabled(bisa);
    }

    @DrawableRes
    static int ikonKategori(@Nullable String kategori) {
        if (kategori == null) {
            return R.drawable.ic_toko;
        }
        switch (kategori) {
            case "bakery":
                return R.drawable.ic_roti;
            case "cafe":
                return R.drawable.ic_kopi;
            case "resto":
                return R.drawable.ic_restoran;
            case "catering":
                return R.drawable.ic_paket;
            default:
                return R.drawable.ic_toko;
        }
    }

    @StringRes
    static int namaKategori(@Nullable String kategori) {
        if (kategori == null) {
            return R.string.kategori_swalayan;
        }
        switch (kategori) {
            case "bakery":
                return R.string.kategori_bakery;
            case "cafe":
                return R.string.kategori_kafe;
            case "resto":
                return R.string.kategori_restoran;
            case "catering":
                return R.string.kategori_katering;
            default:
                return R.string.kategori_swalayan;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
