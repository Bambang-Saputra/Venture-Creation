package id.lifeoffoods.ui.jualan;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.ProfilToko;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.pesanan.RingkasanPesananFragment;
import id.lifeoffoods.ui.umum.Pil;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bagian yang sama di K10 dan K11: argumen, chip info, ikon kategori, tombol favorit, lanjut. */
final class BersamaDetail {

    /** Id jualan yang dibuka (long). Sama dengan BerandaFragment.ARG_LISTING_ID. */
    static final String ARG_LISTING_ID = "listing_id";

    /** Untuk K12/K13: id jualan dan jumlahnya, urutan sama. */
    static final String ARG_ID_JUALAN = "id_jualan";

    static final String ARG_JUMLAH = "jumlah";

    private BersamaDetail() {}

    static long idJualan(Fragment f) {
        return f.requireArguments().getLong(ARG_LISTING_ID, 0);
    }

    static void chip(
            ChipGroup grup,
            CharSequence teks,
            @DrawableRes int latar,
            @ColorRes int warna,
            @DrawableRes int ikon) {
        grup.addView(Pil.buat(grup.getContext(), teks, latar, warna, ikon));
    }

    /** Chip "4,8 (180)" kuning dengan bintang di depan. Tidak dipasang kalau belum ada ulasan. */
    static void chipRating(ChipGroup grup, ListingDto l) {
        if (l.store == null) {
            return;
        }
        String rating = ProfilToko.rating(l.store.ratingAverage, l.store.ratingCount);
        if (rating == null) {
            return;
        }
        TextView pil =
                Pil.buat(
                        grup.getContext(),
                        rating,
                        R.drawable.bg_pil_proses,
                        R.color.tanda_proses_teks,
                        0);
        Pil.ikon(pil, R.drawable.ic_bintang, R.color.tanda_bintang);
        pil.setContentDescription(grup.getContext().getString(R.string.m14_rating, rating));
        grup.addView(pil);
    }

    static void chipAmbil(ChipGroup grup, ListingDto l) {
        chip(
                grup,
                grup.getContext()
                        .getString(
                                R.string.k10_ambil,
                                FormatTampilan.rentangJam(l.pickupStart, l.pickupEnd)),
                R.drawable.bg_pil_isian,
                R.color.teks_kuat,
                R.drawable.ic_jam);
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
    static int keteranganFavorit(boolean favorit) {
        return favorit ? R.string.favorit_hapus : R.string.favorit_tambah;
    }

    /** Figma: hati hijau bergaris; tersimpan jadi merah. */
    @ColorRes
    static int warnaFavorit(boolean favorit) {
        return favorit ? R.color.tanda_bahaya_teks : R.color.merek_utama;
    }

    /**
     * Ke K12 (tas) atau K13 (menu satuan) dengan isi keranjang. Stok dan harga normal dari {@code
     * sumber} ikut dikirim supaya stepper dan harga coret di K13 sama dengan K11.
     */
    static void lanjutKeRingkasan(
            Fragment f,
            int tujuan,
            Map<Long, Integer> terpilih,
            @Nullable List<? extends ListingDto> sumber) {
        long[] id = new long[terpilih.size()];
        int[] qty = new int[terpilih.size()];
        int[] stok = new int[terpilih.size()];
        long[] normal = new long[terpilih.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : terpilih.entrySet()) {
            id[i] = e.getKey();
            qty[i] = e.getValue();
            ListingDto l = cari(sumber, e.getKey());
            stok[i] = l == null ? 0 : l.qtyRemaining;
            normal[i] = l == null || l.originalValueRupiah == null ? 0 : l.originalValueRupiah;
            i++;
        }
        Bundle args = new Bundle();
        args.putLongArray(ARG_ID_JUALAN, id);
        args.putIntArray(ARG_JUMLAH, qty);
        args.putIntArray(RingkasanPesananFragment.ARG_STOK, stok);
        args.putLongArray(RingkasanPesananFragment.ARG_HARGA_NORMAL, normal);
        NavHostFragment.findNavController(f).navigate(tujuan, args);
    }

    @Nullable
    private static ListingDto cari(@Nullable List<? extends ListingDto> sumber, long id) {
        if (sumber == null) {
            return null;
        }
        for (ListingDto l : sumber) {
            if (l != null && l.id == id) {
                return l;
            }
        }
        return null;
    }

    /**
     * Ringkasan menolak karena stok berubah (PRD-06 "Kosong dan galat"): muat ulang detail begitu
     * kembali ke K10/K11.
     */
    static void muatUlangSetelahRingkasan(Fragment f, Runnable muatUlang) {
        NavBackStackEntry ini = NavHostFragment.findNavController(f).getCurrentBackStackEntry();
        if (ini == null) {
            return;
        }
        SavedStateHandle h = ini.getSavedStateHandle();
        h.<Boolean>getLiveData(RingkasanPesananFragment.HASIL_MUAT_ULANG)
                .observe(
                        f.getViewLifecycleOwner(),
                        perlu -> {
                            if (Boolean.TRUE.equals(perlu)) {
                                h.set(RingkasanPesananFragment.HASIL_MUAT_ULANG, false);
                                muatUlang.run();
                            }
                        });
    }

    static void sesiBerakhir(Fragment f) {
        ((MainActivity) f.requireActivity()).sesiBerakhir();
    }

    /** Tombol "Buka di Google Maps" hanya tampil kalau toko punya koordinat. */
    static boolean adaLokasi(@Nullable ListingDto.Toko t) {
        return t != null && t.latitude != null && t.longitude != null;
    }

    /** PRD-05 kriteria 8 dan ADR-0005: intent geo:, tanpa API key. Sama dengan K14. */
    static void bukaMaps(Fragment f, @Nullable ListingDto.Toko t) {
        if (!adaLokasi(t)) {
            return;
        }
        String titik = String.format(Locale.US, "%.6f,%.6f", t.latitude, t.longitude);
        Uri uri =
                Uri.parse(
                        "geo:"
                                + titik
                                + "?q="
                                + titik
                                + "("
                                + Uri.encode(t.name == null ? "" : t.name)
                                + ")");
        try {
            f.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(f.requireView(), R.string.k14_maps_tidak_ada, Snackbar.LENGTH_LONG)
                    .show();
        }
    }
}
