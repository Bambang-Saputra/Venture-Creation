<?php

namespace App\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

/**
 * Permintaan atau verifikasi OTP ditolak. Pesannya aman ditampilkan ke
 * pengguna dan tidak pernah memuat kode OTP.
 */
class OtpDitolak extends RuntimeException
{
    /**
     * @param  string|null  $kode  kode galat tetap untuk Android (kontrak API, bagian galat)
     * @param  array<string, mixed>  $tambahan  field lain di respons, misalnya registered_role
     */
    public function __construct(
        string $pesan,
        private readonly int $status = 422,
        private readonly ?string $kode = null,
        private readonly array $tambahan = [],
    ) {
        parent::__construct($pesan);
    }

    public function render(): JsonResponse
    {
        return response()->json(array_filter(['message' => $this->getMessage(), 'code' => $this->kode]) + $this->tambahan, $this->status);
    }
}
