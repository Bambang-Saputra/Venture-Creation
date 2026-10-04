package id.lifeoffoods.ui.pesanan;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.databinding.FragmentKodePickupBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.KodeQr;
import id.lifeoffoods.ui.umum.SisiAman;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** K14 Kode pickup (PRD-07). Argumen {@code id_pesanan} (long) dari K12/K13 atau nanti K15. */
public class KodePickupFragment extends Fragment {

    public static final String ARG_ID_PESANAN = "id_pesanan";

    private FragmentKodePickupBinding binding;
    private KodePickupViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentKodePickupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(KodePickupViewModel.class);
        vm.mulai(requireArguments().getLong(ARG_ID_PESANAN, 0));

        SisiAman.atas(binding.isi);
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), false);

        binding.barisMitra.label.setText(R.string.k14_mitra);
        binding.barisJam.label.setText(R.string.k14_jam_ambil);
        binding.barisJumlah.label.setText(R.string.k14_jumlah);
        binding.barisBayar.label.setText(R.string.k14_total_bayar);
        binding.barisBayar.nilai.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.teks_merek));

        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolMaps.setOnClickListener(v -> bukaMaps());
        binding.tombolBatal.setOnClickListener(v -> tanyaBatal());
        binding.tombolPesananSaya.setOnClickListener(v -> kePesananSaya());
        binding.tombolCariLagi.setOnClickListener(v -> keBeranda());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.pesanan.observe(getViewLifecycleOwner(), p -> tampilkan());
        vm.luringSejak.observe(getViewLifecycleOwner(), this::isiLuring);
        vm.membatalkan.observe(
                getViewLifecycleOwner(), jalan -> binding.tombolBatal.setEnabled(!jalan));
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
    }

    @Override
    public void onStart() {
        super.onStart();
        vm.terlihat();
    }

    @Override
    public void onStop() {
        super.onStop();
        vm.tersembunyi();
    }

    private void tampilkan() {
        KodePickupViewModel.Status s = vm.status.getValue();
        PesananDto p = vm.pesanan.getValue();
        boolean siap = s == KodePickupViewModel.Status.SIAP && p != null;
        binding.kartu.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.pilStatus.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == KodePickupViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == KodePickupViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (!siap) {
            binding.dampak.setVisibility(View.GONE);
            binding.tautan.setVisibility(View.GONE);
            return;
        }
        isi(p);
    }

    private void isi(PesananDto p) {
        boolean aktif = p.kodeAktif();
        binding.blokKode.setVisibility(aktif ? View.VISIBLE : View.GONE);
        binding.blokAkhir.setVisibility(aktif ? View.GONE : View.VISIBLE);
        binding.kode.setText(aktif ? p.pickupCode : "");
        binding.qr.setImageBitmap(
                aktif && p.pickupCode != null
                        ? KodeQr.gambar(
                                PesananMitra.isiQr(p.pickupCode),
                                Math.round(132 * getResources().getDisplayMetrics().density),
                                ContextCompat.getColor(requireContext(), R.color.teks_utama))
                        : null);
        if (aktif) {
            binding.kode.setContentDescription(
                    getString(R.string.k14_kode_desk, ejaan(p.pickupCode)));
        }
        isiStatus(p);

        binding.barisMitra.nilai.setText(p.store == null ? "" : p.store.name);
        binding.barisJam.nilai.setText(FormatTampilan.rentangJam(p.pickupStart, p.pickupEnd));
        binding.barisJumlah.nilai.setText(getString(R.string.k14_jumlah_item, jumlahPorsi(p)));
        binding.barisBayar.nilai.setText(
                getString(
                        R.string.k14_total_nilai,
                        FormatTampilan.rupiah(p.totalRupiah),
                        getString(
                                PesananBody.BAYAR_QRIS.equals(p.paymentMethod)
                                        ? R.string.k14_qris
                                        : R.string.k14_tunai)));

        boolean menunggu = PesananDto.MENUNGGU_DIAMBIL.equals(p.status);
        binding.dampak.setVisibility(
                menunggu || PesananDto.SELESAI.equals(p.status) ? View.VISIBLE : View.GONE);
        binding.teksDampak.setText(getString(R.string.k14_dampak, jumlahPorsi(p)));
        binding.tautan.setVisibility(menunggu ? View.VISIBLE : View.GONE);
        boolean adaLokasi =
                p.store != null && p.store.latitude != null && p.store.longitude != null;
        binding.tombolMaps.setVisibility(adaLokasi ? View.VISIBLE : View.INVISIBLE);
    }

    private void isiStatus(PesananDto p) {
        @StringRes int teks;
        @StringRes int judulAkhir = 0;
        @StringRes int isiAkhir = 0;
        int warnaTitik = R.color.merek_terang;
        if (PesananDto.SELESAI.equals(p.status)) {
            teks = R.string.k14_selesai;
            judulAkhir = R.string.k14_selesai_judul;
            isiAkhir = R.string.k14_selesai_isi;
            warnaTitik = R.color.merek_lime;
        } else if (PesananDto.DIBATALKAN.equals(p.status)) {
            teks = R.string.k14_dibatalkan;
            judulAkhir = R.string.k14_batal_judul;
            isiAkhir = R.string.k14_batal_isi;
            warnaTitik = R.color.ikon_redup;
        } else if (PesananDto.TIDAK_DIAMBIL.equals(p.status)) {
            teks = R.string.k14_tidak_diambil;
            judulAkhir = R.string.k14_lewat_judul;
            isiAkhir = R.string.k14_lewat_isi;
            warnaTitik = R.color.tanda_bintang;
        } else {
            teks = R.string.k14_siap_diambil;
        }
        binding.teksStatus.setText(teks);
        binding.titikStatus.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), warnaTitik));
        if (judulAkhir != 0) {
            binding.judulAkhir.setText(judulAkhir);
            binding.isiAkhir.setText(isiAkhir);
        }
    }

    private void isiLuring(@Nullable Long sejak) {
        binding.luring.setVisibility(sejak == null ? View.GONE : View.VISIBLE);
        if (sejak != null) {
            String jam = new SimpleDateFormat("HH.mm", Locale.US).format(new Date(sejak));
            binding.luring.setText(getString(R.string.k14_luring, jam));
        }
    }

    /** Figma "Jumlah 2 item": jumlah porsi (qty), bukan jumlah baris. */
    private static int jumlahPorsi(PesananDto p) {
        if (p.items == null || p.items.isEmpty()) {
            return p.itemCount;
        }
        int n = 0;
        for (var b : p.items) {
            n += b.qty;
        }
        return n;
    }

    /** "LF7Q2K" dibacakan huruf per huruf oleh TalkBack: "L F 7 Q 2 K". */
    private static String ejaan(@Nullable String kode) {
        if (kode == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : kode.toCharArray()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Intent geo: tanpa API key (sama dengan K08 saat maps_enabled mati). */
    private void bukaMaps() {
        PesananDto p = vm.pesanan.getValue();
        if (p == null || p.store == null || p.store.latitude == null || p.store.longitude == null) {
            return;
        }
        String titik = String.format(Locale.US, "%f,%f", p.store.latitude, p.store.longitude);
        Uri uri = Uri.parse("geo:" + titik + "?q=" + titik + "(" + Uri.encode(p.store.name) + ")");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.k14_maps_tidak_ada, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void tanyaBatal() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.k14_batal_tanya_judul)
                .setMessage(R.string.k14_batal_tanya_isi)
                .setNegativeButton(R.string.k14_batal_tidak, null)
                .setPositiveButton(R.string.k14_batal_ya, (d, w) -> vm.batalkan())
                .show();
    }

    /** Dibuka dari K15: kembali ke sana, supaya K15 tidak menumpuk di back stack. */
    private void kePesananSaya() {
        NavController nav = NavHostFragment.findNavController(this);
        NavBackStackEntry sebelum = nav.getPreviousBackStackEntry();
        if (sebelum != null && sebelum.getDestination().getId() == R.id.k15_pesanan) {
            nav.popBackStack();
        } else {
            nav.navigate(R.id.k15_pesanan);
        }
    }

    private void keBeranda() {
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.k07_beranda, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.k07_beranda, null, opsi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SisiAman.ikonGelap(requireActivity(), true);
        binding = null;
    }
}
