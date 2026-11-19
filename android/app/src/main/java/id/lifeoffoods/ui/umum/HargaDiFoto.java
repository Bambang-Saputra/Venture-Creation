package id.lifeoffoods.ui.umum;

import android.graphics.Paint;
import android.view.View;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.IncludeHargaDiFotoBinding;

/**
 * Mengisi include_harga_di_foto: pil diskon oranye di pojok foto, dan (kalau diminta) harga jual
 * besar di atas selubung gelap. Uji coba dari masukan pengguna supaya potongan harga langsung
 * terlihat di foto.
 */
public final class HargaDiFoto {

    private HargaDiFoto() {}

    /**
     * Persen hemat dari harga normal, dibulatkan; 0 kalau tidak ada harga normal yang lebih mahal.
     */
    public static int diskon(ListingDto l) {
        Long asli = l.originalValueRupiah;
        if (asli == null || asli <= l.priceRupiah || asli <= 0) {
            return 0;
        }
        return (int) Math.round((asli - l.priceRupiah) * 100.0 / asli);
    }

    public static void isi(IncludeHargaDiFotoBinding o, ListingDto l, boolean denganHarga) {
        int d = diskon(l);
        o.pilDiskon.setVisibility(d > 0 ? View.VISIBLE : View.GONE);
        o.pilDiskon.setText(o.getRoot().getContext().getString(R.string.war_diskon, d));
        o.selubung.setVisibility(denganHarga ? View.VISIBLE : View.GONE);
        o.barisHarga.setVisibility(denganHarga ? View.VISIBLE : View.GONE);
        if (!denganHarga) {
            return;
        }
        o.hargaFoto.setText(FormatTampilan.rupiah(l.priceRupiah));
        if (d > 0) {
            o.hargaCoretFoto.setText(FormatTampilan.rupiah(l.originalValueRupiah));
            o.hargaCoretFoto.setPaintFlags(
                    o.hargaCoretFoto.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            o.hargaCoretFoto.setVisibility(View.VISIBLE);
        } else {
            o.hargaCoretFoto.setVisibility(View.GONE);
        }
    }
}
