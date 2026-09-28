package id.lifeoffoods.ui.umum;

import androidx.annotation.NonNull;
import id.lifeoffoods.databinding.IncludeStepperBinding;

/**
 * Mengisi komponen "Stepper jumlah" (K10, K11, K13). Tombol yang tidak bisa dipakai diredupkan dan
 * dimatikan.
 */
public final class Stepper {

    private Stepper() {}

    public static void isi(
            @NonNull IncludeStepperBinding s,
            int qty,
            boolean bisaKurang,
            boolean bisaTambah,
            Runnable kurang,
            Runnable tambah) {
        s.jumlah.setText(String.valueOf(qty));
        s.kurang.setEnabled(bisaKurang);
        s.kurang.setAlpha(bisaKurang ? 1f : 0.4f);
        s.tambah.setEnabled(bisaTambah);
        s.tambah.setAlpha(bisaTambah ? 1f : 0.4f);
        s.kurang.setOnClickListener(v -> kurang.run());
        s.tambah.setOnClickListener(v -> tambah.run());
    }
}
