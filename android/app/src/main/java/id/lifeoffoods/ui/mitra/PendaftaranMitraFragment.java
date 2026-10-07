package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.model.PendaftaranMitraDto;
import id.lifeoffoods.databinding.FragmentPendaftaranMitraBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.mitra.PendaftaranMitraViewModel.Tahap;
import id.lifeoffoods.ui.umum.BantuanIsian;
import id.lifeoffoods.ui.umum.KeluarAkun;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.ArrayList;
import java.util.List;

/**
 * M03 Daftar sebagai mitra dan M04 Verifikasi usaha, untuk akun mitra yang belum punya toko. M05
 * membuka layar ini (tanpa riwayat) saat GET /partner/stores kosong; layar ini kembali ke M05
 * begitu tim menyetujui pendaftarannya.
 */
public class PendaftaranMitraFragment extends Fragment {

    /** Label chip M03, urut sama dengan {@link PendaftaranMitraViewModel#KATEGORI}. */
    private static final int[] LABEL_KATEGORI = {
        R.string.kategori_kafe,
        R.string.kategori_bakery,
        R.string.kategori_restoran,
        R.string.kategori_katering,
        R.string.kategori_swalayan
    };

    private FragmentPendaftaranMitraBinding binding;
    private PendaftaranMitraViewModel vm;
    private final List<Chip> chipKategori = new ArrayList<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPendaftaranMitraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PendaftaranMitraViewModel.class);
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);

        BantuanIsian bNamaUsaha =
                new BantuanIsian(binding.isianNamaUsaha, binding.bantuanNamaUsaha);
        BantuanIsian bAlamat = new BantuanIsian(binding.isianAlamat, binding.bantuanAlamat);
        BantuanIsian bPemilik = new BantuanIsian(binding.isianPemilik, binding.bantuanPemilik);
        BantuanIsian bNib = new BantuanIsian(binding.isianNib, binding.bantuanNib);
        BantuanIsian bHalal = new BantuanIsian(binding.isianHalal, binding.bantuanHalal);

        // Di langkah 2 tombol kembali (bilah atas dan sistem) kembali ke langkah 1.
        OnBackPressedCallback keLangkahSatu =
                new OnBackPressedCallback(false) {
                    @Override
                    public void handleOnBackPressed() {
                        vm.mundur();
                    }
                };
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), keLangkahSatu);
        binding.kepala.tombolKembali.setOnClickListener(v -> vm.mundur());

        pasangChipKategori();
        binding.tombolJamBuka.setOnClickListener(v -> pilihJam(true));
        binding.tombolJamTutup.setOnClickListener(v -> pilihJam(false));
        binding.tombolUtama.setOnClickListener(v -> aksiUtama());
        binding.tombolKeluar.setOnClickListener(v -> KeluarAkun.tanya(this));
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        vm.muat();
        vm.tahap.observe(
                getViewLifecycleOwner(),
                t -> {
                    keLangkahSatu.setEnabled(t == Tahap.PEMILIK);
                    tampilkanTahap(t);
                });
        vm.kategori.observe(
                getViewLifecycleOwner(),
                k -> {
                    for (Chip c : chipKategori) {
                        boolean pilih = c.getTag().equals(k);
                        if (c.isChecked() != pilih) {
                            c.setChecked(pilih);
                        }
                    }
                });
        vm.jamBuka.observe(
                getViewLifecycleOwner(),
                m -> binding.tombolJamBuka.setText(PendaftaranMitraViewModel.tampilJam(m)));
        vm.jamTutup.observe(
                getViewLifecycleOwner(),
                m -> binding.tombolJamTutup.setText(PendaftaranMitraViewModel.tampilJam(m)));
        vm.galatNamaUsaha.observe(getViewLifecycleOwner(), bNamaUsaha::galat);
        vm.galatAlamat.observe(getViewLifecycleOwner(), bAlamat::galat);
        vm.galatPemilik.observe(getViewLifecycleOwner(), bPemilik::galat);
        vm.galatNib.observe(getViewLifecycleOwner(), bNib::galat);
        vm.galatHalal.observe(getViewLifecycleOwner(), bHalal::galat);
        vm.galatKategori.observe(
                getViewLifecycleOwner(), g -> tampilkanGalat(binding.galatKategori, g));
        vm.galatJam.observe(getViewLifecycleOwner(), g -> tampilkanGalat(binding.galatJam, g));
        vm.pendaftaran.observe(getViewLifecycleOwner(), this::tampilkanPendaftaran);
        vm.isiUlang.observe(
                getViewLifecycleOwner(),
                p -> {
                    PendaftaranMitraDto d = p.ambil();
                    if (d != null) {
                        binding.namaUsaha.setText(d.storeName);
                        binding.alamat.setText(d.address);
                        binding.pemilik.setText(d.ownerName);
                        binding.nib.setText(d.nib);
                        binding.halal.setText(d.halalCertificateNo);
                    }
                });
        vm.mengirim.observe(
                getViewLifecycleOwner(),
                kirim -> {
                    binding.tombolUtama.setEnabled(!kirim);
                    binding.progresKirim.setVisibility(kirim ? View.VISIBLE : View.GONE);
                });
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String s = p.ambil();
                    if (s != null) {
                        Snackbar.make(view, s, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.disetujui.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        NavHostFragment.findNavController(this)
                                .navigate(
                                        R.id.m05_dashboard,
                                        null,
                                        new NavOptions.Builder()
                                                .setPopUpTo(R.id.m03_daftar_mitra, true)
                                                .build());
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

    private void pasangChipKategori() {
        chipKategori.clear();
        binding.grupKategori.removeAllViews();
        for (int i = 0; i < PendaftaranMitraViewModel.KATEGORI.length; i++) {
            Chip chip =
                    (Chip)
                            getLayoutInflater()
                                    .inflate(
                                            R.layout.item_chip_pilihan,
                                            binding.grupKategori,
                                            false);
            chip.setText(LABEL_KATEGORI[i]);
            chip.setTag(PendaftaranMitraViewModel.KATEGORI[i]);
            chip.setId(View.generateViewId());
            chip.setOnCheckedChangeListener(
                    (c, dipilih) -> {
                        if (dipilih) {
                            vm.pilihKategori((String) c.getTag());
                        }
                    });
            binding.grupKategori.addView(chip);
            chipKategori.add(chip);
        }
    }

    private void pilihJam(boolean buka) {
        Integer sekarang = (buka ? vm.jamBuka : vm.jamTutup).getValue();
        int m = sekarang == null ? (buka ? 7 * 60 : 21 * 60) : sekarang;
        MaterialTimePicker p =
                new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(m / 60)
                        .setMinute(m % 60)
                        .setTitleText(buka ? R.string.m03_jam_buka : R.string.m03_jam_tutup)
                        .build();
        p.addOnPositiveButtonClickListener(v -> vm.ubahJam(buka, p.getHour() * 60 + p.getMinute()));
        p.show(getChildFragmentManager(), "jam");
    }

    private void aksiUtama() {
        Tahap t = vm.tahap.getValue();
        if (t == Tahap.DATA_USAHA) {
            vm.lanjut(teks(binding.namaUsaha), teks(binding.alamat));
        } else if (t == Tahap.PEMILIK) {
            vm.kirim(
                    teks(binding.namaUsaha),
                    teks(binding.alamat),
                    teks(binding.pemilik),
                    teks(binding.nib),
                    teks(binding.halal));
        } else if (t == Tahap.MENUNGGU) {
            vm.periksaStatus();
        }
    }

    private void tampilkanTahap(Tahap t) {
        boolean muat = t == Tahap.MEMUAT || t == Tahap.GAGAL_MUAT;
        binding.keadaan.getRoot().setVisibility(muat ? View.VISIBLE : View.GONE);
        binding.keadaan.progres.setVisibility(t == Tahap.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(t == Tahap.GAGAL_MUAT ? View.VISIBLE : View.GONE);
        if (t == Tahap.GAGAL_MUAT) {
            binding.keadaan.gagalIsi.setText(vm.galatMuat.getValue());
        }
        binding.gulir.setVisibility(muat ? View.INVISIBLE : View.VISIBLE);
        binding.bilahAksi.setVisibility(muat ? View.GONE : View.VISIBLE);

        binding.bagianUsaha.setVisibility(t == Tahap.DATA_USAHA ? View.VISIBLE : View.GONE);
        binding.bagianPemilik.setVisibility(t == Tahap.PEMILIK ? View.VISIBLE : View.GONE);
        binding.bagianMenunggu.setVisibility(t == Tahap.MENUNGGU ? View.VISIBLE : View.GONE);
        binding.tombolKeluar.setVisibility(t == Tahap.PEMILIK ? View.GONE : View.VISIBLE);
        // Layar ini tujuan awal graf mitra; kembali hanya bermakna di langkah 2.
        binding.kepala.tombolKembali.setVisibility(t == Tahap.PEMILIK ? View.VISIBLE : View.GONE);

        if (t == Tahap.DATA_USAHA) {
            judul(R.string.m03_judul, R.string.m03_langkah);
            binding.tombolUtama.setText(R.string.lanjut);
        } else if (t == Tahap.PEMILIK) {
            judul(R.string.m04_judul, R.string.m04_langkah);
            binding.tombolUtama.setText(R.string.m04_kirim);
        } else {
            judul(R.string.m04_menunggu_bar, 0);
            binding.tombolUtama.setText(R.string.m04_periksa_status);
        }
        binding.gulir.scrollTo(0, 0);
    }

    private void tampilkanPendaftaran(@Nullable PendaftaranMitraDto p) {
        boolean ditolak = p != null && PendaftaranMitraDto.DITOLAK.equals(p.status);
        binding.spandukDitolak.setVisibility(ditolak ? View.VISIBLE : View.GONE);
        if (ditolak) {
            binding.teksDitolak.setText(
                    TextUtils.isEmpty(p.rejectionReason)
                            ? getString(R.string.m03_ditolak_tanpa_alasan)
                            : getString(R.string.m03_ditolak, p.rejectionReason));
        }
        if (p == null || !PendaftaranMitraDto.MENUNGGU.equals(p.status)) {
            return;
        }
        binding.teksMenunggu.setText(getString(R.string.m04_menunggu_isi, p.storeName));
        binding.ringkasanToko.setText(p.storeName);
        int buka = PendaftaranMitraViewModel.menit(p.openTime, 0);
        int tutup = PendaftaranMitraViewModel.menit(p.closeTime, 0);
        String baris1 =
                getString(
                        R.string.m04_ringkasan_jam,
                        labelKategori(p.category),
                        PendaftaranMitraViewModel.tampilJam(buka),
                        PendaftaranMitraViewModel.tampilJam(tutup));
        String baris3 =
                TextUtils.isEmpty(p.nib)
                        ? getString(R.string.m04_ringkasan_tanpa_nib)
                        : getString(R.string.m04_ringkasan_nib, p.nib);
        binding.ringkasanIsi.setText(baris1 + "\n" + p.address + "\n" + baris3);
    }

    private String labelKategori(@Nullable String kode) {
        for (int i = 0; i < PendaftaranMitraViewModel.KATEGORI.length; i++) {
            if (PendaftaranMitraViewModel.KATEGORI[i].equals(kode)) {
                return getString(LABEL_KATEGORI[i]);
            }
        }
        return kode == null ? "" : kode;
    }

    private void judul(@StringRes int judul, @StringRes int sub) {
        binding.kepala.judul.setText(judul);
        binding.kepala.subjudul.setVisibility(sub == 0 ? View.GONE : View.VISIBLE);
        if (sub != 0) {
            binding.kepala.subjudul.setText(sub);
        }
    }

    private static void tampilkanGalat(TextView t, @Nullable String g) {
        t.setText(g);
        t.setVisibility(g == null ? View.GONE : View.VISIBLE);
    }

    private static String teks(EditText kolom) {
        CharSequence t = kolom.getText();
        return t == null ? "" : t.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        chipKategori.clear();
        binding = null;
    }
}
