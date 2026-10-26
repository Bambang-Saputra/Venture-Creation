package id.lifeoffoods.ui.voucher;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.DaftarVoucherDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.VoucherDto;
import id.lifeoffoods.databinding.DialogPromoBinding;
import id.lifeoffoods.ui.jualan.BeliLangsung;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.HargaDiFoto;

/**
 * Pop up promo saat Beranda pertama tampil dalam satu sesi aplikasi (masukan uji coba: pembeli
 * langsung tertarik tanpa menggulir). Isinya jualan dengan diskon terbesar yang sedang war dan
 * voucher pop up yang bisa diklaim. "Beli sekarang" mengklaim voucher lalu langsung membuka
 * ringkasan pesanan dengan voucher terpasang.
 */
public final class PromoPopup {

    /** Sekali per proses aplikasi, supaya tidak muncul setiap kembali ke Beranda. */
    private static boolean sudahTampil;

    /** Hanya untuk tes tangkapan layar: pop up tidak menutupi layar lain. */
    @androidx.annotation.VisibleForTesting
    public static void aturUntukUji(boolean boleh) {
        sudahTampil = !boleh;
    }

    private PromoPopup() {}

    public static void tampilSekali(Fragment f, ListingDto jualan) {
        if (sudahTampil) {
            return;
        }
        sudahTampil = true;
        LofApp app = (LofApp) f.requireActivity().getApplication();
        app.api()
                .voucher()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<DaftarVoucherDto> data) {
                                if (f.isAdded() && f.getView() != null) {
                                    tampil(f, jualan, data == null ? null : data.data);
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (f.isAdded() && f.getView() != null) {
                                    tampil(f, jualan, null);
                                }
                            }
                        });
    }

    private static void tampil(Fragment f, ListingDto l, @Nullable DaftarVoucherDto v) {
        DialogPromoBinding b = DialogPromoBinding.inflate(LayoutInflater.from(f.requireContext()));
        AlertDialog d =
                new MaterialAlertDialogBuilder(f.requireContext()).setView(b.getRoot()).create();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        FotoJualan.muat(b.foto, l.photoUrl, 0);
        int diskon = HargaDiFoto.diskon(l);
        b.diskon.setVisibility(diskon > 0 ? View.VISIBLE : View.GONE);
        b.diskon.setText(f.getString(R.string.war_diskon, diskon));
        String toko = l.store == null ? "" : l.store.name;
        b.judulJualan.setText(toko.isEmpty() ? l.title : l.title + " · " + toko);
        b.harga.setText(FormatTampilan.rupiah(l.priceRupiah));
        if (diskon > 0) {
            b.hargaCoret.setText(FormatTampilan.rupiah(l.originalValueRupiah));
            b.hargaCoret.setPaintFlags(b.hargaCoret.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            b.hargaCoret.setVisibility(View.GONE);
        }
        String jam = FormatTampilan.jam(l.pickupEnd);
        b.judul.setText(
                diskon > 0
                        ? f.getString(R.string.promo_judul, diskon, jam)
                        : f.getString(R.string.promo_judul_tanpa_diskon, jam));

        VoucherDto promo = v == null ? null : v.promo;
        b.kotakVoucher.setVisibility(promo == null ? View.GONE : View.VISIBLE);
        b.tombolKlaim.setVisibility(promo == null ? View.GONE : View.VISIBLE);
        if (promo != null) {
            b.teksVoucher.setText(
                    f.getString(
                            R.string.promo_voucher,
                            promo.code,
                            promo.title,
                            TampilanVoucher.syarat(f.requireContext(), promo)));
        }

        b.tombolTutup.setOnClickListener(x -> d.dismiss());
        b.tombolBeli.setOnClickListener(
                x -> {
                    d.dismiss();
                    if (promo == null) {
                        BeliLangsung.satu(f, l);
                    } else {
                        klaimLalu(f, promo, milik -> BeliLangsung.satu(f, l, milik.id));
                    }
                });
        b.tombolKlaim.setOnClickListener(
                x -> {
                    d.dismiss();
                    klaimLalu(
                            f,
                            promo,
                            milik ->
                                    Snackbar.make(
                                                    f.requireView(),
                                                    R.string.k22_diklaim,
                                                    Snackbar.LENGTH_LONG)
                                            .show());
                });
        d.show();
    }

    private interface SetelahKlaim {
        void jalan(VoucherDto milik);
    }

    private static void klaimLalu(Fragment f, VoucherDto promo, SetelahKlaim lanjut) {
        LofApp app = (LofApp) f.requireActivity().getApplication();
        app.api()
                .klaimVoucher(promo.id)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<VoucherDto> data) {
                                if (f.isAdded() && data != null && data.data != null) {
                                    lanjut.jalan(data.data);
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (f.isAdded() && f.getView() != null) {
                                    Snackbar.make(f.requireView(), e.pesan(), Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        });
    }
}
