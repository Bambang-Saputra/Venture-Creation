package id.lifeoffoods.ui.masuk;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.NomorHp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.data.api.model.GoogleLoginBody;
import id.lifeoffoods.data.api.model.OtpRequestBody;
import id.lifeoffoods.data.api.model.OtpRequestResponse;
import id.lifeoffoods.ui.umum.Peristiwa;

/** K02 Masuk dan M01 Masuk mitra: minta kode OTP, atau masuk lewat Google. */
public class MasukViewModel extends AndroidViewModel {

    /** Kode sudah diminta; lanjut ke K03/M02. */
    public static final class KodeTerkirim {
        public final String nomor;
        public final OtpRequestResponse respons;

        KodeTerkirim(String nomor, OtpRequestResponse respons) {
            this.nomor = nomor;
            this.respons = respons;
        }
    }

    public final MutableLiveData<Boolean> memuat = new MutableLiveData<>(false);
    public final MutableLiveData<String> galatNomor = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<KodeTerkirim>> kodeTerkirim = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<AuthResponse>> masuk = new MutableLiveData<>();

    public MasukViewModel(@NonNull Application app) {
        super(app);
    }

    public void mintaKode(String masukan, String peran) {
        String nomor = NomorHp.normalisasi(masukan);
        if (nomor == null) {
            galatNomor.setValue("Nomor HP tidak valid. Contoh: 812 3456 7890.");
            return;
        }
        galatNomor.setValue(null);
        memuat.setValue(true);
        app().api()
                .mintaOtp(new OtpRequestBody(nomor, peran))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(OtpRequestResponse data) {
                                memuat.setValue(false);
                                kodeTerkirim.setValue(
                                        new Peristiwa<>(new KodeTerkirim(nomor, data)));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memuat.setValue(false);
                                String field = e.pesanField("phone");
                                if (field != null) {
                                    galatNomor.setValue(field);
                                } else {
                                    galat.setValue(new Peristiwa<>(e.pesan()));
                                }
                            }
                        });
    }

    /** Dipanggil setelah Credential Manager mengembalikan ID token Google. */
    public void masukGoogle(String idToken, String peran) {
        memuat.setValue(true);
        app().api()
                .masukGoogle(new GoogleLoginBody(idToken, peran))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(AuthResponse data) {
                                memuat.setValue(false);
                                masuk.setValue(new Peristiwa<>(data));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memuat.setValue(false);
                                galat.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    public void tampilkanGalat(String pesan) {
        galat.setValue(new Peristiwa<>(pesan));
    }

    private LofApp app() {
        return getApplication();
    }
}
