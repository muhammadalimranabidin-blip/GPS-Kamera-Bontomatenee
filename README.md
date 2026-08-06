# GPS Map Camera Bontomatene — Versi 4.0

Aplikasi Android kamera ber-GPS untuk lima desa di Kecamatan Bontomatene, Kabupaten Kepulauan Selayar:

- Tanete
- Pamatata
- Bungaiya
- Menara Indah
- Kayu Bauk

## Pembaruan tampilan versi 4.0

Hasil foto sekarang dibuat menyerupai tampilan contoh GPS Map Camera:

- Foto memenuhi layar dalam posisi mendatar/landscape.
- Mini map persegi di kiri bawah dengan pin lokasi berwarna magenta.
- Panel informasi hitam transparan dengan sudut membulat.
- Badge **GPS Map Camera** di atas panel.
- Judul lokasi: Kecamatan Bontomatene, Sulawesi Selatan, Indonesia.
- Nama desa, kecamatan, kabupaten, provinsi, dan negara.
- Kode GPS unik.
- Latitude, longitude, dan akurasi GPS.
- Hari, tanggal, jam WITA, dan GMT +08:00.
- Bendera Indonesia pada judul.
- Cap terlihat langsung pada pratinjau kamera dan ditulis permanen ke hasil foto.

## Cara penggunaan

1. Buka aplikasi dengan HP dalam posisi mendatar.
2. Pilih desa.
3. Atur tanggal dan jam, atau tekan **Sekarang**.
4. Tekan **Perbarui GPS** dan tunggu akurasi membaik.
5. Tekan **Ambil Foto**.
6. Foto tersimpan di `Galeri > Pictures > GPS Bontomatene`.

## Peta mini dan penggunaan offline

Mini map hanya menampilkan satu tulisan **Google**. Tulisan tambahan yang menyebabkan label ganda telah dihapus.
Contoh hasil terbaru tersedia pada `CONTOH_TAMPILAN_V4.png`.

Untuk menampilkan citra satelit atau peta jalan yang benar-benar sesuai titik GPS, dibutuhkan tile peta yang diunduh sebelumnya atau layanan peta daring. Hal tersebut belum dimasukkan pada versi ini agar aplikasi tetap sederhana dan berfungsi tanpa internet.

## Data yang ditulis ke foto

Contoh:

```text
Kecamatan Bontomatene, Sulawesi Selatan, Indonesia
Desa Tanete, Kec. Bontomatene, Kab. Kepulauan Selayar,
Sulawesi Selatan, Indonesia
Kode GPS: BTM-TNT-20260729-094100
Lat -5.859911° Long 120.507415° | Akurasi ±6 m
Senin, 08/06/2026 09:41 WITA (GMT +08:00)
```

Koordinat dan waktu aktual juga disimpan ke metadata EXIF JPEG.

## Persyaratan HP

- Android 10 atau lebih baru.
- Kamera belakang.
- GPS aktif.
- Izin kamera dan lokasi presisi.
- Disarankan akurasi GPS di bawah 20 meter.

## Membuka di Android Studio

1. Ekstrak ZIP proyek.
2. Buka Android Studio.
3. Pilih **Open** lalu pilih folder `GPSKameraBontomatene`.
4. Tunggu Gradle Sync selesai.
5. Hubungkan HP dengan USB Debugging aktif.
6. Tekan **Run**.

## Membuat APK di Windows

Android Studio, Android SDK, dan Java 17 harus tersedia. Klik dua kali:

`BUILD_APK_WINDOWS.bat`

Skrip akan mengunduh Gradle Wrapper resmi bila belum tersedia, lalu menghasilkan:

`GPS-Kamera-Bontomatene-debug.apk`

## Catatan pengujian

Kode sumber, resource XML, penyimpanan MediaStore, EXIF, dan struktur proyek sudah diperbarui ke versi 4.0. APK belum dikompilasi di lingkungan pembuatan ini karena Android SDK tidak tersedia. Lakukan Gradle Sync dan uji pada HP untuk memastikan ukuran cap sesuai rasio kamera masing-masing perangkat.
