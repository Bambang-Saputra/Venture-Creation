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
    public function __construct(string $pesan, private readonly int $status = 422)
    {
        parent::__construct($pesan);
    }

    public function render(): JsonResponse
    {
        return response()->json(['message' => $this->getMessage()], $this->status);
    }
}
