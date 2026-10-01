package id.lifeoffoods.ui.masuk;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.data.api.model.OtpRequestBody;
import id.lifeoffoods.data.api.model.OtpRequestResponse;
import id.lifeoffoods.data.api.model.OtpVerifyBody;
import id.lifeoffoods.ui.umum.Peristiwa;

/** K03 dan M02 Verifikasi OTP. */
public class VerifikasiOtpViewModel extends AndroidViewModel {

    public static final int PANJANG_KODE = 6;

    public final MutableLiveData<Boolean> memuat = new MutableLiveData<>(false);

    /** Detik sampai "Kirim ulang" boleh diketuk; 0 berarti boleh. */
    public final MutableLiveData<Integer> sisaDetik = new MutableLiveData<>(0);

    /** Kode yang ditampilkan di spanduk mode uji coba; null di luar PILOT_MODE. */
    public final MutableLiveData<String> kodeUjiCoba = new MutableLiveData<>();

    public final MutableLiveData<String> galatKode = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<AuthResponse>> masuk = new MutableLiveData<>();

    /** Kode salah (422): isian kode dikosongkan sekali, bukan setiap rotasi. */
    public final MutableLiveData<Peristiwa<Boolean>> kosongkanKode = new MutableLiveData<>();

    /** Nomor terdaftar dengan peran lain (403): pesan server, lalu tawarkan halaman yang benar. */
    public final MutableLiveData<Peristiwa<String>> salahHalaman = new MutableLiveData<>();

    /** Peran halaman ini ("consumer" atau "partner"); tujuan tombol pindah adalah peran lainnya. */
    public String peran() {
        return peran;
    }

    private String nomor;
    private String peran;
    private boolean siap;
    @Nullable private String kodeTerakhir;
    @Nullable private CountDownTimer penghitung;

    public VerifikasiOtpViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dipanggil sekali dari argumen navigasi; pemanggilan berikutnya (rotasi) diabaikan. */
    public void mulai(String nomor, String peran, int jedaKirimUlang, @Nullable String kodeUji) {
        if (siap) {
            return;
        }
        siap = true;
        this.nomor = nomor;
        this.peran = peran;
        kodeUjiCoba.setValue(kodeUji);
        hitungMundur(jedaKirimUlang);
    }

    /**
     * Dipanggil saat digit keenam diketik. Kode yang sama tidak dikirim dua kali, misalnya saat
     * EditText memulihkan teksnya setelah rotasi, supaya jatah 5 percobaan tidak terbuang.
     */
    public void verifikasiOtomatis(String kode) {
        if (kode.equals(kodeTerakhir)) {
            return;
        }
        verifikasi(kode);
    }

    public void verifikasi(String kode) {
        if (kode.length() != PANJANG_KODE) {
            galatKode.setValue("Masukkan 6 digit kode.");
            return;
        }
        Boolean sedang = memuat.getValue();
        if (sedang != null && sedang) {
            return;
        }
        kodeTerakhir = kode;
        galatKode.setValue(null);
        memuat.setValue(true);
        app().api()
                .verifikasiOtp(new OtpVerifyBody(nomor, kode, peran))
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
                                // 422 kode salah/kedaluwarsa dan 429 percobaan habis tampil di
                                // bawah kotak kode. Kode salah juga mengosongkan isian (PRD-01).
                                // Peran salah (403) menawarkan halaman yang benar (kriteria 8).
                                if (e.kode() == 422 || e.kode() == 429) {
                                    String field = e.pesanField("code");
                                    galatKode.setValue(field != null ? field : e.pesan());
                                    if (e.kode() == 422) {
                                        kodeTerakhir = null;
                                        kosongkanKode.setValue(new Peristiwa<>(true));
                                    }
                                } else if (e.salahHalaman()) {
                                    salahHalaman.setValue(new Peristiwa<>(e.pesan()));
                                } else {
                                    galat.setValue(new Peristiwa<>(e.pesan()));
                                }
                            }
                        });
    }

    public void kirimUlang() {
        Integer sisa = sisaDetik.getValue();
        if (sisa != null && sisa > 0) {
            return;
        }
        memuat.setValue(true);
        app().api()
                .mintaOtp(new OtpRequestBody(nomor, peran))
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(OtpRequestResponse data) {
                                memuat.setValue(false);
                                galatKode.setValue(null);
                                kodeTerakhir = null;
                                kodeUjiCoba.setValue(data.pilotCode);
                                hitungMundur(data.resendIn);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memuat.setValue(false);
                                galat.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    private void hitungMundur(int detik) {
        if (penghitung != null) {
            penghitung.cancel();
        }
        sisaDetik.setValue(Math.max(0, detik));
        if (detik <= 0) {
            return;
        }
        penghitung =
                new CountDownTimer(detik * 1000L, 1000L) {
                    @Override
                    public void onTick(long sisaMs) {
                        sisaDetik.setValue((int) Math.ceil(sisaMs / 1000.0));
                    }

                    @Override
                    public void onFinish() {
                        sisaDetik.setValue(0);
                    }
                }.start();
    }

    @Override
    protected void onCleared() {
        if (penghitung != null) {
            penghitung.cancel();
        }
    }

    private LofApp app() {
        return getApplication();
    }
}
