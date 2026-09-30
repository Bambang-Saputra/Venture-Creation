package id.lifeoffoods.ui.mitra;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import id.lifeoffoods.R;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.databinding.IncludeCatatanAlergiBinding;
import java.util.ArrayList;
import java.util.List;

/** Tampilan bersama M11 dan M12: kotak alergi, kotak catatan, dan label cara bayar. */
final class TampilanPesananMitra {

    private TampilanPesananMitra() {}

    /**
     * Alergi berat di baris pertama dengan kotak merah; alergi biasa memakai kotak yang sama tapi
     * tidak ditulis "berat". Catatan pembeli di kotak kuning. Kotak kosong disembunyikan.
     */
    static void isiCatatan(Context c, IncludeCatatanAlergiBinding b, PesananMitraDto p) {
        List<String> berat = new ArrayList<>();
        List<String> biasa = new ArrayList<>();
        for (PesananMitraDto.Alergi a : PesananMitra.urutAlergi(p.allergenSnapshot)) {
            (PesananMitra.ALERGI_BERAT.equals(a.severity) ? berat : biasa).add(a.name);
        }
        StringBuilder teks = new StringBuilder();
        if (!berat.isEmpty()) {
            teks.append(c.getString(R.string.m11_alergi_berat, String.join(", ", berat)));
        }
        if (!biasa.isEmpty()) {
            if (teks.length() > 0) {
                teks.append('\n');
            }
            teks.append(c.getString(R.string.m11_alergi, String.join(", ", biasa)));
        }
        b.kotakAlergi.setVisibility(teks.length() == 0 ? View.GONE : View.VISIBLE);
        b.teksAlergi.setText(teks);
        // Merah bergaris hanya kalau ada alergi berat; alergi biasa memakai kotak kuning.
        boolean adaBerat = !berat.isEmpty();
        int warna =
                ContextCompat.getColor(
                        c, adaBerat ? R.color.tanda_bahaya_teks : R.color.tanda_proses_teks);
        b.kotakAlergi.setBackgroundResource(
                adaBerat ? R.drawable.bg_alergi_berat : R.drawable.bg_catatan_pelanggan);
        b.teksAlergi.setTextColor(warna);
        b.ikonAlergi.setImageTintList(ColorStateList.valueOf(warna));

        @Nullable String catatan = p.note == null ? null : p.note.trim();
        boolean adaCatatan = catatan != null && !catatan.isEmpty();
        b.kotakCatatan.setVisibility(adaCatatan ? View.VISIBLE : View.GONE);
        if (adaCatatan) {
            b.teksCatatan.setText(c.getString(R.string.m11_catatan, catatan));
        }
    }

    static String caraBayar(Context c, @Nullable String metode) {
        return PesananBody.BAYAR_QRIS.equals(metode)
                ? c.getString(R.string.m11_bayar_qris)
                : c.getString(R.string.m11_bayar_tunai);
    }
}
