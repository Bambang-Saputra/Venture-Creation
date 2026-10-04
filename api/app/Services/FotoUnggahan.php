<?php

namespace App\Services;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * Foto unggahan (F-20): profil, toko, template tas, dan jualan.
 *
 * Setiap foto digambar ulang sebagai JPEG lewat GD. Akibatnya metadata
 * kamera ikut terbuang, termasuk koordinat GPS yang biasanya tertanam di
 * foto HP, dan berkas yang hanya menyamar sebagai gambar tidak pernah
 * disimpan apa adanya. Nama berkas acak, bukan nama dari HP.
 */
final class FotoUnggahan
{
    /** Sisi terpanjang foto yang disimpan, px. Cukup untuk layar HP 3x. */
    public const MAKS_SISI = 1600;

    /** Aturan validasi untuk field `photo`. Batas piksel menjaga memori GD. */
    public const ATURAN = ['required', 'file', 'mimes:jpg,jpeg,png,webp', 'max:5120',
        'dimensions:min_width=200,min_height=200,max_width=4096,max_height=4096'];

    /** @return string path relatif di disk public, contoh "listings/12/<uuid>.jpg" */
    public static function simpan(UploadedFile $berkas, string $folder): string
    {
        $gambar = @imagecreatefromstring((string) file_get_contents($berkas->getRealPath()));
        if ($gambar === false) {
            throw ValidationException::withMessages(['photo' => 'Berkas ini bukan gambar yang bisa dibaca.']);
        }

        $gambar = self::luruskan($gambar, $berkas);

        $lebar = imagesx($gambar);
        $tinggi = imagesy($gambar);
        $skala = min(1, self::MAKS_SISI / max($lebar, $tinggi));
        $lebarBaru = max(1, (int) round($lebar * $skala));
        $tinggiBaru = max(1, (int) round($tinggi * $skala));

        // Latar putih: PNG/WebP transparan tidak jadi hitam saat menjadi JPEG.
        $hasil = imagecreatetruecolor($lebarBaru, $tinggiBaru);
        imagefill($hasil, 0, 0, (int) imagecolorallocate($hasil, 255, 255, 255));
        imagecopyresampled($hasil, $gambar, 0, 0, 0, 0, $lebarBaru, $tinggiBaru, $lebar, $tinggi);

        ob_start();
        imagejpeg($hasil, null, 82);
        $jpeg = (string) ob_get_clean();

        $path = trim($folder, '/').'/'.Str::uuid().'.jpg';
        Storage::disk('public')->put($path, $jpeg);

        return $path;
    }

    /**
     * Hapus foto lama, tapi hanya yang memang milik folder entitas itu.
     * Jualan yang memakai foto template atau foto demo tidak boleh ikut
     * menghapus berkas tersebut.
     */
    public static function hapus(?string $path, string $folderMilik): void
    {
        if ($path !== null && str_starts_with($path, trim($folderMilik, '/').'/')) {
            Storage::disk('public')->delete($path);
        }
    }

    /**
     * URL publik. Memakai host permintaan (asset), bukan APP_URL, supaya
     * HP yang masuk lewat ngrok mendapat alamat https ngrok.
     */
    public static function url(?string $path): ?string
    {
        return $path === null ? null : asset('storage/'.$path);
    }

    /** Foto HP potret sering disimpan miring dengan tanda Orientation di EXIF. */
    private static function luruskan(\GdImage $gambar, UploadedFile $berkas): \GdImage
    {
        if (! function_exists('exif_read_data') || $berkas->getMimeType() !== 'image/jpeg') {
            return $gambar;
        }
        $exif = @exif_read_data($berkas->getRealPath());
        $sudut = match ((int) (($exif ?: [])['Orientation'] ?? 1)) {
            3 => 180,
            6 => -90,
            8 => 90,
            default => 0,
        };

        return $sudut === 0 ? $gambar : (imagerotate($gambar, $sudut, 0) ?: $gambar);
    }
}
