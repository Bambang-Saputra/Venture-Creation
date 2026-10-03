package id.lifeoffoods.ui.umum;

import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.ui.MainActivity;

/** Keluar dari akun: konfirmasi dulu, cabut token di server, hapus sesi lokal, kembali ke K01. */
public final class KeluarAkun {

    private KeluarAkun() {}

    public static void tanya(Fragment f) {
        new MaterialAlertDialogBuilder(f.requireContext())
                .setTitle(R.string.keluar_judul)
                .setMessage(R.string.keluar_isi)
                .setNegativeButton(R.string.batal, null)
                .setPositiveButton(R.string.segera_keluar, (d, w) -> jalankan(f))
                .show();
    }

    public static void jalankan(Fragment f) {
        LofApp app = (LofApp) f.requireActivity().getApplication();
        if (app.sesi().sudahMasuk()) {
            // Token di server dicabut kalau jaringan ada; sesi lokal dihapus apa pun hasilnya.
            app.api()
                    .keluar("Bearer " + app.sesi().token())
                    .enqueue(
                            new ApiCallback<>() {
                                @Override
                                public void sukses(Void data) {}

                                @Override
                                public void gagal(ApiError galat) {}
                            });
            app.sesi().hapus();
        }
        ((MainActivity) f.requireActivity()).kembaliKeAwal();
    }
}
