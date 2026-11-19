package id.lifeoffoods.ui.voucher;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.GosokDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.VoucherDto;
import id.lifeoffoods.databinding.DialogGosokBinding;

/**
 * Gosok & Menang (ADR-0008). Hasil diundi server (POST /games/scratch, sekali sehari) saat dialog
 * dibuka, lalu ditaruh di bawah lapisan gosok. Pembeli menggosok untuk melihatnya.
 */
public final class GosokDialog {

    private GosokDialog() {}

    /**
     * @param setelah dipanggil saat dialog ditutup, misalnya untuk memuat ulang daftar voucher.
     */
    public static void tampil(Fragment f, @Nullable Runnable setelah) {
        DialogGosokBinding b = DialogGosokBinding.inflate(LayoutInflater.from(f.requireContext()));
        AlertDialog d =
                new MaterialAlertDialogBuilder(f.requireContext())
                        .setView(b.getRoot())
                        .setOnDismissListener(
                                x -> {
                                    if (setelah != null) {
                                        setelah.run();
                                    }
                                })
                        .create();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        b.kartu.setAktif(false);
        b.tombolTutup.setOnClickListener(v -> d.dismiss());
        b.tombolBuka.setOnClickListener(v -> b.kartu.buka());
        b.tombolBuka.setEnabled(false);
        d.show();

        LofApp app = (LofApp) f.requireActivity().getApplication();
        app.api()
                .gosok()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<GosokDto> data) {
                                if (!f.isAdded()) {
                                    return;
                                }
                                GosokDto h = data == null ? null : data.data;
                                isiHasil(f, b, h);
                                b.kartu.setAktif(true);
                                b.tombolBuka.setEnabled(true);
                                b.kartu.saatTerbuka(
                                        () -> {
                                            b.pesan.setVisibility(View.VISIBLE);
                                            b.pesan.setText(
                                                    h != null && h.won
                                                            ? R.string.gosok_menang
                                                            : R.string.gosok_zonk_ajak);
                                            b.tombolBuka.setVisibility(View.GONE);
                                        });
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (!f.isAdded()) {
                                    return;
                                }
                                // Sudah main hari ini (409) atau gagal jaringan: tampilkan
                                // pesannya tanpa lapisan gosok.
                                b.progres.setVisibility(View.GONE);
                                b.judulHasil.setVisibility(View.VISIBLE);
                                b.judulHasil.setText(e.pesan());
                                b.kartu.buka();
                                b.tombolBuka.setVisibility(View.GONE);
                            }
                        });
    }

    private static void isiHasil(Fragment f, DialogGosokBinding b, @Nullable GosokDto h) {
        b.progres.setVisibility(View.GONE);
        b.nilai.setVisibility(View.VISIBLE);
        b.judulHasil.setVisibility(View.VISIBLE);
        VoucherDto v = h == null ? null : h.voucher;
        if (h != null && h.won && v != null) {
            b.nilai.setText(TampilanVoucher.nilai(v));
            b.judulHasil.setText(v.title);
            b.syaratHasil.setVisibility(View.VISIBLE);
            b.syaratHasil.setText(TampilanVoucher.syarat(f.requireContext(), v));
        } else {
            b.nilai.setText(R.string.gosok_zonk_nilai);
            b.judulHasil.setText(R.string.gosok_zonk);
        }
    }
}
