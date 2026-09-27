<?php

namespace App\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

/**
 * Login Google ditolak. Pesannya aman ditampilkan ke pengguna:
 * tidak pernah memuat isi token atau alasan kriptografis yang rinci.
 */
class LoginGoogleDitolak extends RuntimeException
{
    public function __construct(string $pesan, private readonly int $status = 401)
    {
        parent::__construct($pesan);
    }

    public function render(): JsonResponse
    {
        return response()->json(['message' => $this->getMessage()], $this->status);
    }
}
