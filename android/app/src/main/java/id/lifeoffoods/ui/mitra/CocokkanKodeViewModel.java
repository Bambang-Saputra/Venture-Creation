package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.TukarKodeBody;
import id.lifeoffoods.ui.umum.Peristiwa;

/**
 * M12 Cocokkan kode (PRD-11). Satu tindakan: POST /pickup-codes/redeem menyelesaikan pesanan di
 * server, lalu layar menampilkan isi pesanan dan alergi pembeli sekali lagi sebelum diserahkan.
 * Galat 404/409 ditampilkan di bawah kotak kode dengan pesan server, lalu kode siap diketik ulang.
 */
public class CocokkanKodeViewModel extends AndroidViewModel {

    public final MutableLiveData<Boolean> menukar = new MutableLiveData<>(false);
    public final MutableLiveData<PesananMitraDto> hasil = new MutableLiveData<>();
    public final MutableLiveData<String> galatKode = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    /** Kode terakhir yang dikirim, supaya ketikan yang sama tidak terkirim dua kali. */
    private String terakhirDikirim;

    public CocokkanKodeViewModel(@NonNull Application app) {
        super(app);
    }

    /** Ketikan berubah: hapus galat lama, dan hasil lama kalau kasir mulai mengetik kode baru. */
    public void kodeBerubah(String kode) {
        if (galatKode.getValue() != null) {
            galatKode.setValue(null);
        }
        if (!kode.equals(terakhirDikirim)) {
            terakhirDikirim = null;
        }
    }

    public void tukar(String ketikan) {
        String kode = PesananMitra.rapikanKode(ketikan);
        if (kode.length() != PesananMitra.PANJANG_KODE) {
            galatKode.setValue(null);
            return;
        }
        if (Boolean.TRUE.equals(menukar.getValue()) || kode.equals(terakhirDikirim)) {
            return;
        }
        terakhirDikirim = kode;
        menukar.setValue(true);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        kirim(app, idToko, kode);
                    }

                    @Override
                    public void gagal(ApiError e) {
                        selesaiGagal(e);
                    }
                });
    }

    private void kirim(LofApp app, long idToko, String kode) {
        app.api()
                .tukarKode(new TukarKodeBody(idToko, kode))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananMitraDto> data) {
                                menukar.setValue(false);
                                if (data == null || data.data == null) {
                                    selesaiGagal(ApiError.jaringan());
                                    return;
                                }
                                hasil.setValue(data.data);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                selesaiGagal(e);
                            }
                        });
    }

    private void selesaiGagal(ApiError e) {
        menukar.setValue(false);
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (e.kode() == 404 || e.kode() == 409 || e.kode() == 422) {
            // Pesan server sudah menjelaskan alasannya (PRD-11): tidak ditemukan, sudah dipakai
            // jam sekian, dibatalkan, atau lewat jam ambil. Kode yang sama boleh dicoba lagi.
            galatKode.setValue(e.pesan());
        } else {
            galat.setValue(new Peristiwa<>(e.pesan()));
        }
        terakhirDikirim = null;
    }

    /** "Cocokkan kode lain": kembali ke keadaan awal. */
    public void ulang() {
        hasil.setValue(null);
        galatKode.setValue(null);
        terakhirDikirim = null;
    }
}
