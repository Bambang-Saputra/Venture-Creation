package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PendaftaranMitraDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.Locale;

/**
 * M03 Daftar sebagai mitra (langkah 1, data usaha) dan M04 Verifikasi usaha (langkah 2, pemilik dan
 * izin), lalu layar menunggu sampai tim menyetujui lewat {@code php artisan mitra:setujui}.
 *
 * <p>Satu ViewModel untuk tiga keadaan karena isiannya satu badan POST /partner/application. KTP
 * dan rekening dari Figma M04 sengaja tidak diminta; NIB opsional.
 */
public class PendaftaranMitraViewModel extends AndroidViewModel {

    public enum Tahap {
        MEMUAT,
        GAGAL_MUAT,
        DATA_USAHA,
        PEMILIK,
        MENUNGGU
    }

    /** Nilai {@code category} di API, urut sama dengan chip M03. */
    public static final String[] KATEGORI = {"cafe", "bakery", "resto", "catering", "grocery"};

    private static final int BUKA_BAWAAN = 7 * 60;
    private static final int TUTUP_BAWAAN = 21 * 60;

    public final MutableLiveData<Tahap> tahap = new MutableLiveData<>(Tahap.MEMUAT);
    public final MutableLiveData<String> galatMuat = new MutableLiveData<>();

    /** Pendaftaran terakhir: ringkasan di layar menunggu, atau alasan penolakan di M03. */
    public final MutableLiveData<PendaftaranMitraDto> pendaftaran = new MutableLiveData<>();

    /** Isi ulang formulir dari pendaftaran yang ditolak. Dikirim sekali. */
    public final MutableLiveData<Peristiwa<PendaftaranMitraDto>> isiUlang = new MutableLiveData<>();

    public final MutableLiveData<String> kategori = new MutableLiveData<>();

    /** Menit sejak tengah malam. Bawaan 07.00 sampai 21.00, sama dengan contoh di Figma M03. */
    public final MutableLiveData<Integer> jamBuka = new MutableLiveData<>(BUKA_BAWAAN);

    public final MutableLiveData<Integer> jamTutup = new MutableLiveData<>(TUTUP_BAWAAN);

    public final MutableLiveData<String> galatNamaUsaha = new MutableLiveData<>();
    public final MutableLiveData<String> galatKategori = new MutableLiveData<>();
    public final MutableLiveData<String> galatAlamat = new MutableLiveData<>();
    public final MutableLiveData<String> galatJam = new MutableLiveData<>();
    public final MutableLiveData<String> galatPemilik = new MutableLiveData<>();
    public final MutableLiveData<String> galatNib = new MutableLiveData<>();
    public final MutableLiveData<String> galatHalal = new MutableLiveData<>();

    public final MutableLiveData<Boolean> mengirim = new MutableLiveData<>(false);
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();

    /** Tim sudah menyetujui: fragment membuka M05. */
    public final MutableLiveData<Peristiwa<Boolean>> disetujui = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private boolean sudahMuat;
    private boolean memeriksa;

    public PendaftaranMitraViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dipanggil tiap onViewCreated; hanya pemanggilan pertama yang memuat. */
    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        periksa(false);
    }

    public void muatUlang() {
        tahap.setValue(Tahap.MEMUAT);
        periksa(false);
    }

    /** Tombol "Periksa status" di layar menunggu. */
    public void periksaStatus() {
        periksa(true);
    }

    private void periksa(boolean dariTombol) {
        if (memeriksa) {
            return;
        }
        memeriksa = true;
        if (dariTombol) {
            mengirim.setValue(true);
        }
        app().api()
                .pendaftaranMitra()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PendaftaranMitraDto> data) {
                                memeriksa = false;
                                mengirim.setValue(false);
                                terapkan(data == null ? null : data.data, dariTombol);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memeriksa = false;
                                mengirim.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                } else if (dariTombol) {
                                    pesan.setValue(new Peristiwa<>(e.pesan()));
                                } else {
                                    galatMuat.setValue(e.pesan());
                                    tahap.setValue(Tahap.GAGAL_MUAT);
                                }
                            }
                        });
    }

    private void terapkan(@Nullable PendaftaranMitraDto p, boolean dariTombol) {
        pendaftaran.setValue(p);
        if (p == null) {
            tahap.setValue(Tahap.DATA_USAHA);
            return;
        }
        if (PendaftaranMitraDto.DISETUJUI.equals(p.status)) {
            disetujui.setValue(new Peristiwa<>(true));
        } else if (PendaftaranMitraDto.MENUNGGU.equals(p.status)) {
            tahap.setValue(Tahap.MENUNGGU);
            if (dariTombol) {
                pesan.setValue(new Peristiwa<>(app().getString(R.string.m04_masih_ditinjau)));
            }
        } else {
            // Ditolak: formulir diisi ulang supaya cukup memperbaiki yang disebut tim.
            kategori.setValue(p.category);
            jamBuka.setValue(menit(p.openTime, BUKA_BAWAAN));
            jamTutup.setValue(menit(p.closeTime, TUTUP_BAWAAN));
            isiUlang.setValue(new Peristiwa<>(p));
            tahap.setValue(Tahap.DATA_USAHA);
        }
    }

    public void pilihKategori(String k) {
        kategori.setValue(k);
        galatKategori.setValue(null);
    }

    public void ubahJam(boolean buka, int menit) {
        (buka ? jamBuka : jamTutup).setValue(menit);
        galatJam.setValue(null);
    }

    /** "Lanjut" di M03. Pemeriksaan ringan di sini; aturan lengkapnya tetap di server. */
    public void lanjut(String namaUsaha, String alamat) {
        String gNama = namaUsaha.trim().length() < 2 ? teks(R.string.m03_galat_nama) : null;
        String gKategori = kategori.getValue() == null ? teks(R.string.m03_galat_kategori) : null;
        String gAlamat = alamat.trim().length() < 10 ? teks(R.string.m03_galat_alamat) : null;
        String gJam = nilai(jamTutup) <= nilai(jamBuka) ? teks(R.string.m03_galat_jam) : null;
        galatNamaUsaha.setValue(gNama);
        galatKategori.setValue(gKategori);
        galatAlamat.setValue(gAlamat);
        galatJam.setValue(gJam);
        if (gNama == null && gKategori == null && gAlamat == null && gJam == null) {
            tahap.setValue(Tahap.PEMILIK);
        }
    }

    /** Tombol kembali di M04 kembali ke M03, bukan menutup layar. */
    public boolean mundur() {
        if (tahap.getValue() == Tahap.PEMILIK) {
            tahap.setValue(Tahap.DATA_USAHA);
            return true;
        }
        return false;
    }

    /** "Kirim untuk ditinjau" di M04. */
    public void kirim(String namaUsaha, String alamat, String pemilik, String nib, String halal) {
        if (Boolean.TRUE.equals(mengirim.getValue())) {
            return;
        }
        // NIB sering disalin dengan spasi pemisah; yang dikirim hanya angkanya.
        String nibBersih = nib.replaceAll("\\s", "");
        String gPemilik = pemilik.trim().length() < 2 ? teks(R.string.m04_galat_pemilik) : null;
        String gNib =
                nibBersih.isEmpty() || nibBersih.matches("[0-9]{13}")
                        ? null
                        : teks(R.string.m04_galat_nib);
        galatPemilik.setValue(gPemilik);
        galatNib.setValue(gNib);
        galatHalal.setValue(null);
        if (gPemilik != null || gNib != null) {
            return;
        }

        PendaftaranMitraDto.Kirim body = new PendaftaranMitraDto.Kirim();
        body.storeName = namaUsaha.trim();
        body.category = kategori.getValue();
        body.address = alamat.trim();
        body.openTime = jam(nilai(jamBuka));
        body.closeTime = jam(nilai(jamTutup));
        body.ownerName = pemilik.trim();
        body.nib = nibBersih.isEmpty() ? null : nibBersih;
        body.halalCertificateNo = halal.trim().isEmpty() ? null : halal.trim();

        mengirim.setValue(true);
        app().api()
                .kirimPendaftaranMitra(body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PendaftaranMitraDto> data) {
                                mengirim.setValue(false);
                                pendaftaran.setValue(data == null ? null : data.data);
                                tahap.setValue(Tahap.MENUNGGU);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                mengirim.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                } else if (e.kode() == 409) {
                                    // Sudah ada yang menunggu, atau tokonya sudah jadi.
                                    pesan.setValue(new Peristiwa<>(e.pesan()));
                                    periksa(false);
                                } else {
                                    tampilkanGalatServer(e);
                                }
                            }
                        });
    }

    /** Galat 422 milik langkah 1 membawa pengguna kembali ke M03, ke kolom yang salah. */
    private void tampilkanGalatServer(ApiError e) {
        String gNama = e.pesanField("store_name");
        String gKategori = e.pesanField("category");
        String gAlamat = e.pesanField("address");
        String gJam = e.pesanField("close_time");
        if (gJam == null) {
            gJam = e.pesanField("open_time");
        }
        String gPemilik = e.pesanField("owner_name");
        String gNib = e.pesanField("nib");
        String gHalal = e.pesanField("halal_certificate_no");
        galatNamaUsaha.setValue(gNama);
        galatKategori.setValue(gKategori);
        galatAlamat.setValue(gAlamat);
        galatJam.setValue(gJam);
        galatPemilik.setValue(gPemilik);
        galatNib.setValue(gNib);
        galatHalal.setValue(gHalal);
        if (gNama != null || gKategori != null || gAlamat != null || gJam != null) {
            tahap.setValue(Tahap.DATA_USAHA);
        } else if (gPemilik == null && gNib == null && gHalal == null) {
            pesan.setValue(new Peristiwa<>(e.pesan()));
        }
    }

    /** "07.00", format jam yang dipakai di seluruh aplikasi. */
    public static String tampilJam(int menit) {
        return String.format(Locale.ROOT, "%02d.%02d", menit / 60, menit % 60);
    }

    /** "07:00", format yang diminta API. */
    static String jam(int menit) {
        return String.format(Locale.ROOT, "%02d:%02d", menit / 60, menit % 60);
    }

    /** "07:00" atau "07:00:00" menjadi menit; nilai yang tidak dikenal memakai bawaan. */
    static int menit(@Nullable String hhmm, int bawaan) {
        if (hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            return bawaan;
        }
        String[] b = hhmm.split(":");
        return Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
    }

    private static int nilai(MutableLiveData<Integer> d) {
        Integer v = d.getValue();
        return v == null ? 0 : v;
    }

    private String teks(int id) {
        return app().getString(id);
    }

    private LofApp app() {
        return getApplication();
    }
}
