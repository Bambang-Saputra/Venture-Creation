package id.lifeoffoods.ui.jualan;

import android.os.Bundle;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.ChipGroup;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.Pil;
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

    /** Ke K12 (tas) atau K13 (menu satuan) dengan isi keranjang. */
    static void lanjutKeRingkasan(Fragment f, int tujuan, Map<Long, Integer> terpilih) {
        long[] id = new long[terpilih.size()];
        int[] qty = new int[terpilih.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : terpilih.entrySet()) {
            id[i] = e.getKey();
            qty[i] = e.getValue();
            i++;
        }
        Bundle args = new Bundle();
        args.putLongArray(ARG_ID_JUALAN, id);
        args.putIntArray(ARG_JUMLAH, qty);
        NavHostFragment.findNavController(f).navigate(tujuan, args);
    }

    static void sesiBerakhir(Fragment f) {
        ((MainActivity) f.requireActivity()).sesiBerakhir();
    }
}
