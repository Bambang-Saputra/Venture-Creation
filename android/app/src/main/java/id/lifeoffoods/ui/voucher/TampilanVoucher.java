package id.lifeoffoods.ui.voucher;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.VoucherDto;
import id.lifeoffoods.databinding.ItemVoucherBinding;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Teks voucher yang dipakai K22, pop up promo, dan hasil Gosok & Menang. */
public final class TampilanVoucher {

    private static final DateTimeFormatter TANGGAL =
            DateTimeFormatter.ofPattern("d MMM", new Locale("id", "ID"));

    private TampilanVoucher() {}

    /** "20%" atau "Rp5rb", untuk sisi kiri kartu. */
    public static String nilai(VoucherDto v) {
        if (v.persen()) {
            return v.value + "%";
        }
        return v.value % 1000 == 0
                ? "Rp" + (v.value / 1000) + "rb"
                : FormatTampilan.rupiah(v.value);
    }

    /** "Min. belanja Rp15.000 · maks. potongan Rp10.000", atau "Tanpa minimal belanja". */
    public static String syarat(Context c, VoucherDto v) {
        String min =
                v.minSpendRupiah > 0
                        ? c.getString(
                                R.string.voucher_min_belanja,
                                FormatTampilan.rupiah(v.minSpendRupiah))
                        : c.getString(R.string.voucher_tanpa_min);
        if (v.maxDiscountRupiah != null && v.persen()) {
            return min
                    + " · "
                    + c.getString(
                            R.string.voucher_maks_potong,
                            FormatTampilan.rupiah(v.maxDiscountRupiah));
        }
        return min;
    }

    @Nullable
    public static String berlaku(Context c, VoucherDto v) {
        if (v.expiresAt != null) {
            try {
                return c.getString(
                        R.string.voucher_berlaku_sampai,
                        OffsetDateTime.parse(v.expiresAt).format(TANGGAL));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        if (v.validDays != null) {
            return c.getString(R.string.voucher_berlaku_hari, v.validDays);
        }
        return null;
    }

    public static void isi(ItemVoucherBinding b, VoucherDto v) {
        Context c = b.getRoot().getContext();
        b.nilai.setText(nilai(v));
        b.judul.setText(v.title);
        b.syarat.setText(syarat(c, v));
        String masa = berlaku(c, v);
        b.berlaku.setText(masa);
        b.berlaku.setVisibility(masa == null ? View.GONE : View.VISIBLE);
        b.getRoot()
                .setContentDescription(
                        v.title + ". " + b.syarat.getText() + (masa == null ? "" : ". " + masa));
    }
}
