package id.lifeoffoods.ui.umum;

import androidx.annotation.Nullable;

/**
 * Nilai LiveData yang hanya boleh ditangani sekali: navigasi, Snackbar. Tanpa pembungkus ini,
 * rotasi layar mengirim ulang nilai terakhir dan navigasi atau Snackbar terjadi dua kali.
 */
public final class Peristiwa<T> {

    private final T isi;
    private boolean sudahDiambil;

    public Peristiwa(T isi) {
        this.isi = isi;
    }

    /** Isi pada pengambilan pertama, null sesudahnya. */
    @Nullable
    public T ambil() {
        if (sudahDiambil) {
            return null;
        }
        sudahDiambil = true;
        return isi;
    }
}
