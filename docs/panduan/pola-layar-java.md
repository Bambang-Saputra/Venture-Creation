# Pola layar Android (Java)

Panduan yang dijanjikan ADR-0002: satu cara membuat layar, dipakai di semua fitur. Salin pola ini, jangan membuat variasi baru per layar.

## Struktur paket

```
id.lifeoffoods
├── LofApp                 satu SesiPengguna dan satu LofApi untuk seluruh aplikasi
├── data
│   ├── SesiPengguna       token dan peran (SharedPreferences privat)
│   └── api
│       ├── ApiClient      Retrofit + header wajib (Accept, Bearer, ngrok)
│       ├── LofApi         daftar endpoint, mengikuti docs/api/kontrak-api.md
│       ├── ApiCallback    sukses(data) / gagal(ApiError)
│       ├── ApiError       pesan server siap tampil, galat per field
│       └── model          DTO Gson, satu kelas per bentuk JSON
└── ui
    ├── MainActivity       memilih graf: nav_awal, nav_konsumen, nav_mitra
    ├── umum               BaseListAdapter dan komponen bersama
    ├── awal               K01
    ├── konsumen.<fitur>   contoh konsumen.beranda untuk K07
    └── mitra.<fitur>      contoh mitra.catatsisa untuk M06
```

## Satu layar = Fragment + ViewModel + layout

1. **Layout** `fragment_<nama>.xml`, warna selalu dari `colors.xml` (token Figma), teks dari `strings.xml`.
2. **ViewModel** memanggil API dan menyimpan keadaan layar di `MutableLiveData`. ViewModel tidak memegang View atau Context activity.
3. **Fragment** hanya mengamati LiveData dan mengisi View lewat ViewBinding.
4. **Graf navigasi**: ganti `SegeraFragment` di `nav_konsumen.xml` atau `nav_mitra.xml` dengan fragment baru, id tujuan memakai kode layar (`k07_beranda`).

```java
public class BerandaViewModel extends AndroidViewModel {
    public final MutableLiveData<Boolean> memuat = new MutableLiveData<>(false);
    public final MutableLiveData<List<ListingDto>> daftar = new MutableLiveData<>();
    public final MutableLiveData<String> galat = new MutableLiveData<>();

    public BerandaViewModel(@NonNull Application app) { super(app); }

    public void muat() {
        memuat.setValue(true);
        ((LofApp) getApplication()).api().listings().enqueue(new ApiCallback<>() {
            @Override public void sukses(Halaman<ListingDto> data) {
                memuat.setValue(false);
                daftar.setValue(data.data);
            }
            @Override public void gagal(ApiError e) {
                memuat.setValue(false);
                galat.setValue(e.pesan());
            }
        });
    }
}
```

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
    vm = new ViewModelProvider(this).get(BerandaViewModel.class);
    BaseListAdapter<ListingDto, ItemListingBinding> adapter = new BaseListAdapter<>(
            ItemListingBinding::inflate,
            (b, l) -> b.judul.setText(l.title),
            l -> l.id);
    binding.daftar.setAdapter(adapter);

    vm.daftar.observe(getViewLifecycleOwner(), adapter::submitList);
    vm.galat.observe(getViewLifecycleOwner(),
            pesan -> Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show());
    if (state == null) vm.muat();
}
```

## Aturan

- **Binding** dibuat di `onCreateView` dan di-null-kan di `onDestroyView`.
- **Observe** selalu dengan `getViewLifecycleOwner()`, bukan `this`.
- **Galat**: tampilkan `ApiError.pesan()` apa adanya (sudah berbahasa Indonesia). Untuk 422, pasang `pesanField("phone")` di `TextInputLayout.setError`. Kalau `perluMasukUlang()` (401), panggil `sesi().hapus()` lalu `MainActivity.kembaliKeAwal()`.
- **Endpoint baru**: tambahkan method di `LofApi` dan DTO di `data.api.model` dengan `@SerializedName` persis seperti kontrak API. Respons yang dibungkus `data` memakai kelas pembungkus, auth dan `/me` tidak.
- **Uang** diformat di layar dengan `NumberFormat.getCurrencyInstance(new Locale("id", "ID"))`, tidak dihitung ulang di aplikasi.
- **Uji**: logika tanpa View (pemformat, pengurai, validasi) diuji di `app/src/test` dengan JUnit, jalan di `testDebugUnitTest`.
- **Format kode**: `./gradlew spotlessApply` sebelum commit; CI menolak kode yang belum diformat.
