package id.lifeoffoods.ui.umum;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputLayout;
import id.lifeoffoods.R;

/**
 * Teks bantuan dan galat di bawah kolom isian, rata kiri dengan label seperti di Figma.
 * TextInputLayout tetap dipakai untuk kotaknya; galat hanya mengubah garis kotak jadi merah dan
 * mengganti teks bantuan, tanpa ikon galat bawaan Material.
 */
public final class BantuanIsian {

    private final TextInputLayout kotak;
    private final TextView bantuan;
    @Nullable private final CharSequence teksBawaan;
    private final ColorStateList garisBawaan;

    /**
     * @param bantuan TextView di bawah kotak; teksnya saat ini dipakai sebagai teks bawaan.
     */
    public BantuanIsian(TextInputLayout kotak, TextView bantuan) {
        this.kotak = kotak;
        this.bantuan = bantuan;
        CharSequence t = bantuan.getText();
        this.teksBawaan = t == null || t.length() == 0 ? null : t;
        this.garisBawaan = ContextCompat.getColorStateList(kotak.getContext(), R.color.isian_garis);
        galat(null);
    }

    /** null menghapus galat dan mengembalikan teks bantuan (atau menyembunyikannya). */
    public void galat(@Nullable String pesan) {
        boolean ada = pesan != null;
        int merah = ContextCompat.getColor(kotak.getContext(), R.color.tanda_bahaya_teks);
        // Harus stateful: untuk ColorStateList satu warna, TextInputLayout hanya mengganti warna
        // saat fokus, jadi garis kotak yang tidak fokus tetap abu-abu.
        ColorStateList garisMerah =
                new ColorStateList(
                        new int[][] {new int[] {android.R.attr.state_focused}, new int[] {}},
                        new int[] {merah, merah});
        kotak.setBoxStrokeColorStateList(ada ? garisMerah : garisBawaan);
        CharSequence isi = ada ? pesan : teksBawaan;
        bantuan.setText(isi);
        bantuan.setVisibility(isi == null ? View.GONE : View.VISIBLE);
        bantuan.setTextColor(
                ada ? merah : ContextCompat.getColor(kotak.getContext(), R.color.teks_sekunder));
    }
}
