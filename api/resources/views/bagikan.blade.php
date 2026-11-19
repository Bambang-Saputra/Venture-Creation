<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{ $l->title }} di {{ $l->store_name }} · Life of Foods</title>
{{-- Tag Open Graph: WhatsApp memakainya untuk pratinjau tautan. --}}
<meta property="og:type" content="website">
<meta property="og:site_name" content="Life of Foods">
<meta property="og:title" content="{{ $l->title }}, {{ $harga }}{{ $hemat ? ' (hemat '.$hemat.'%)' : '' }}">
<meta property="og:description" content="{{ $l->store_name }}. Ambil hari ini jam {{ $jam }}. Buruan sebelum habis.">
@if ($foto)
<meta property="og:image" content="{{ $foto }}">
@endif
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: system-ui, -apple-system, "Segoe UI", sans-serif; background: #F5F9F5; color: #0F2A1E; }
  main { max-width: 440px; margin: 0 auto; padding: 20px 16px 32px; }
  .merek { font-weight: 800; color: #1B8A5A; font-size: 18px; }
  .kartu { margin-top: 16px; background: #fff; border: 1px solid #E8EFE9; border-radius: 20px; overflow: hidden; }
  .foto { position: relative; height: 220px; background: #E6EFE8 center / cover no-repeat; }
  .hemat { position: absolute; left: 12px; top: 12px; background: #FF5A1F; color: #fff; font-weight: 800; padding: 6px 12px; border-radius: 999px; }
  .isi { padding: 16px; }
  h1 { font-size: 20px; line-height: 1.25; }
  .toko { margin-top: 4px; color: #5E6F64; font-size: 14px; }
  .harga { margin-top: 12px; display: flex; align-items: baseline; gap: 8px; }
  .harga b { font-size: 26px; color: #1B8A5A; }
  .harga s { color: #6B7C71; }
  .jam { margin-top: 10px; display: inline-block; background: #F1F6F2; padding: 6px 12px; border-radius: 999px; font-size: 13px; color: #4E5F55; }
  .habis { margin-top: 12px; color: #C0392B; font-weight: 600; }
  .tombol { display: block; margin-top: 20px; text-align: center; background: #1B8A5A; color: #fff; text-decoration: none; font-weight: 700; padding: 15px; border-radius: 16px; }
  .catatan { margin-top: 12px; font-size: 12.5px; color: #5E6F64; text-align: center; line-height: 1.5; }
</style>
</head>
<body>
<main>
  <div class="merek">Life of Foods</div>
  <div class="kartu">
    <div class="foto" @if ($foto) style="background-image: url('{{ $foto }}')" @endif>
      @if ($hemat)<span class="hemat">-{{ $hemat }}%</span>@endif
    </div>
    <div class="isi">
      <h1>{{ $l->title }}</h1>
      <div class="toko">{{ $l->store_name }}@if ($l->store_address) · {{ $l->store_address }}@endif</div>
      <div class="harga"><b>{{ $harga }}</b>@if ($hargaAsli)<s>{{ $hargaAsli }}</s>@endif</div>
      <span class="jam">Ambil hari ini jam {{ $jam }}</span>
      @unless ($masihAda)
        <p class="habis">Jualan ini sudah habis atau lewat jam ambil. Cek jualan lain di aplikasi.</p>
      @endunless
    </div>
  </div>
  <a class="tombol" href="{{ $tautanApp }}">Buka di aplikasi</a>
  <p class="catatan">Pesan lewat aplikasi Life of Foods, lalu ambil dan bayar langsung di toko.</p>
</main>
</body>
</html>
