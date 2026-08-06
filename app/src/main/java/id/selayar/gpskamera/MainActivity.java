package id.selayar.gpskamera;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import id.selayar.gpskamera.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final TimeZone WITA = TimeZone.getTimeZone("Asia/Makassar");
    private static final Locale LOCALE_ID = new Locale("id", "ID");

    private ActivityMainBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private LocationManager locationManager;
    private Location currentLocation;
    private Calendar activityDateTime;
    private Uri lastSavedUri;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private boolean settingsVisible = true;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding == null) return;
            SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", LOCALE_ID);
            clock.setTimeZone(WITA);
            binding.tvClock.setText(clock.format(System.currentTimeMillis()) + " WITA");
            updateOverlayPreview();
            clockHandler.postDelayed(this, 1000L);
        }
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLocation = location;
            updateLocationText(location);
            updateOverlayPreview();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            binding.tvLocation.setText("GPS tidak aktif");
            updateOverlayPreview();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        activityDateTime = Calendar.getInstance(WITA, LOCALE_ID);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                    boolean locationGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    if (cameraGranted) startCamera();
                    if (locationGranted) startLocationUpdates();
                    if (!cameraGranted || !locationGranted) {
                        Toast.makeText(this,
                                "Izin kamera dan lokasi presisi diperlukan agar foto memiliki koordinat GPS.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        setupVillageSpinner();
        setupControls();
        updateScheduleText();
        updateOverlayPreview();
        clockHandler.post(clockRunnable);
        requestRequiredPermissions();
    }

    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void setupVillageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.desa_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDesa.setAdapter(adapter);
        binding.spinnerDesa.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateOverlayPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupControls() {
        binding.btnDate.setOnClickListener(v -> showDatePicker());
        binding.btnTime.setOnClickListener(v -> showTimePicker());
        binding.btnNow.setOnClickListener(v -> {
            activityDateTime = Calendar.getInstance(WITA, LOCALE_ID);
            updateScheduleText();
        });
        binding.btnRefreshGps.setOnClickListener(v -> refreshGps());
        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnLastPhoto.setOnClickListener(v -> openLastPhoto());
        binding.btnToggleSettings.setOnClickListener(v -> toggleSettings());
    }

    private void toggleSettings() {
        settingsVisible = !settingsVisible;
        binding.settingsPanel.setVisibility(settingsVisible ? View.VISIBLE : View.GONE);
        binding.btnToggleSettings.setText(settingsVisible ? "SEMBUNYIKAN PENGATURAN" : "BUKA PENGATURAN");
    }

    private void requestRequiredPermissions() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (cameraGranted) startCamera();
        if (locationGranted) startLocationUpdates();
        if (!cameraGranted || !locationGranted) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = providerFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageCapture.Builder captureBuilder = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
                if (binding.previewView.getDisplay() != null) {
                    captureBuilder.setTargetRotation(binding.previewView.getDisplay().getRotation());
                }
                imageCapture = captureBuilder.build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                Toast.makeText(this, "Kamera gagal dibuka: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            binding.tvLocation.setText("GPS belum aktif");
            return;
        }

        Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (last != null) {
            currentLocation = last;
            updateLocationText(last);
        }

        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.5f,
                locationListener,
                Looper.getMainLooper()
        );
    }

    private void refreshGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestRequiredPermissions();
            return;
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setTitle("Aktifkan GPS")
                    .setMessage("GPS harus aktif agar titik koordinat dapat dimasukkan ke foto.")
                    .setPositiveButton("Buka Pengaturan", (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Batal", null)
                    .show();
            return;
        }

        binding.tvLocation.setText("Mencari koordinat GPS presisi...");
        startLocationUpdates();
    }

    private void updateLocationText(Location location) {
        String text = "Lat " + formatCoordinate(location.getLatitude())
                + "°  Long " + formatCoordinate(location.getLongitude())
                + "°  •  Akurasi ±" + Math.round(location.getAccuracy()) + " m";
        binding.tvLocation.setText(text);
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    activityDateTime.set(Calendar.YEAR, year);
                    activityDateTime.set(Calendar.MONTH, month);
                    activityDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateScheduleText();
                },
                activityDateTime.get(Calendar.YEAR),
                activityDateTime.get(Calendar.MONTH),
                activityDateTime.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    activityDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    activityDateTime.set(Calendar.MINUTE, minute);
                    activityDateTime.set(Calendar.SECOND, 0);
                    updateScheduleText();
                },
                activityDateTime.get(Calendar.HOUR_OF_DAY),
                activityDateTime.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void updateScheduleText() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm 'WITA'", LOCALE_ID);
        formatter.setTimeZone(WITA);
        binding.tvSchedule.setText(formatter.format(activityDateTime.getTime()));

        SimpleDateFormat dateButton = new SimpleDateFormat("dd/MM/yyyy", LOCALE_ID);
        dateButton.setTimeZone(WITA);
        binding.btnDate.setText(dateButton.format(activityDateTime.getTime()));

        SimpleDateFormat timeButton = new SimpleDateFormat("HH:mm", LOCALE_ID);
        timeButton.setTimeZone(WITA);
        binding.btnTime.setText(timeButton.format(activityDateTime.getTime()));
        updateOverlayPreview();
    }

    private void updateOverlayPreview() {
        if (binding == null) return;
        OverlayRenderer.OverlayData data = createOverlayData(
                getSelectedVillage(),
                activityDateTime != null ? activityDateTime.getTimeInMillis() : System.currentTimeMillis(),
                System.currentTimeMillis(),
                currentLocation
        );
        binding.overlayView.setData(data);
    }

    private String getSelectedVillage() {
        Object value = binding.spinnerDesa.getSelectedItem();
        return value == null ? "Tanete" : String.valueOf(value);
    }

    private OverlayRenderer.OverlayData createOverlayData(
            String village,
            long selectedTimeMillis,
            long capturedAt,
            Location location
    ) {
        OverlayRenderer.OverlayData data = new OverlayRenderer.OverlayData();
        data.village = village;
        data.selectedTimeMillis = selectedTimeMillis;
        data.capturedAtMillis = capturedAt;
        data.locationCode = buildLocationCode(village, capturedAt);
        if (location != null) {
            data.hasLocation = true;
            data.latitude = location.getLatitude();
            data.longitude = location.getLongitude();
            data.accuracyMeters = location.getAccuracy();
        }
        return data;
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera belum siap.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLocation == null) {
            Toast.makeText(this, "Koordinat GPS belum diperoleh. Tekan Perbarui GPS.", Toast.LENGTH_LONG).show();
            refreshGps();
            return;
        }
        if (System.currentTimeMillis() - currentLocation.getTime() > 120_000L) {
            Toast.makeText(this, "Koordinat GPS sudah lama. Perbarui GPS sebelum mengambil foto.", Toast.LENGTH_LONG).show();
            refreshGps();
            return;
        }
        if (currentLocation.getAccuracy() > 50f) {
            new AlertDialog.Builder(this)
                    .setTitle("Akurasi GPS masih rendah")
                    .setMessage("Akurasi saat ini ±" + Math.round(currentLocation.getAccuracy())
                            + " meter. Sebaiknya tunggu hingga di bawah 20 meter. Tetap ambil foto?")
                    .setPositiveButton("Tetap Foto", (dialog, which) -> captureNow())
                    .setNegativeButton("Tunggu GPS", null)
                    .show();
            return;
        }
        captureNow();
    }

    private void captureNow() {
        String village = getSelectedVillage();
        Location locationSnapshot = new Location(currentLocation);
        Calendar scheduleSnapshot = (Calendar) activityDateTime.clone();
        long capturedAt = System.currentTimeMillis();

        File tempFile;
        try {
            tempFile = File.createTempFile("capture_", ".jpg", getCacheDir());
        } catch (IOException e) {
            Toast.makeText(this, "Tidak dapat membuat file foto.", Toast.LENGTH_LONG).show();
            return;
        }

        binding.btnCapture.setEnabled(false);
        binding.btnCapture.setText("MENYIMPAN...");

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(tempFile).build();
        imageCapture.takePicture(
                options,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        try {
                            Uri savedUri = stampAndSavePhoto(
                                    tempFile,
                                    village,
                                    scheduleSnapshot,
                                    capturedAt,
                                    locationSnapshot
                            );
                            runOnUiThread(() -> onPhotoSaved(savedUri, locationSnapshot));
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                restoreCaptureButton();
                                Toast.makeText(MainActivity.this,
                                        "Foto gagal disimpan: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        } finally {
                            //noinspection ResultOfMethodCallIgnored
                            tempFile.delete();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        //noinspection ResultOfMethodCallIgnored
                        tempFile.delete();
                        runOnUiThread(() -> {
                            restoreCaptureButton();
                            Toast.makeText(MainActivity.this,
                                    "Pengambilan foto gagal: " + exception.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private Uri stampAndSavePhoto(
            File sourceFile,
            String village,
            Calendar schedule,
            long capturedAt,
            Location location
    ) throws IOException {
        Bitmap oriented = loadOrientedBitmap(sourceFile);
        Bitmap stamped = addGpsMapCameraStamp(oriented, village, schedule, capturedAt, location);

        SimpleDateFormat fileDate = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        fileDate.setTimeZone(WITA);
        String locationCode = buildLocationCode(village, capturedAt);
        String fileName = "GPS_" + locationCode + "_" + fileDate.format(capturedAt) + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GPS Bontomatene");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("MediaStore tidak dapat membuat file.");

        try {
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null || !stamped.compress(Bitmap.CompressFormat.JPEG, 93, output)) {
                    throw new IOException("Gagal menulis gambar.");
                }
            }

            writeExif(uri, location, capturedAt, village, locationCode);

            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            getContentResolver().update(uri, ready, null, null);
            return uri;
        } catch (Exception e) {
            getContentResolver().delete(uri, null, null);
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e);
        } finally {
            if (oriented != stamped && !oriented.isRecycled()) oriented.recycle();
            if (!stamped.isRecycled()) stamped.recycle();
        }
    }

    private Bitmap loadOrientedBitmap(File file) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        int sample = 1;
        int maxDimension = Math.max(bounds.outWidth, bounds.outHeight);
        while (maxDimension / sample > 4096) sample *= 2;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) throw new IOException("Gambar kamera tidak dapat dibaca.");

        ExifInterface exif = new ExifInterface(file);
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
        );

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1f, -1f);
                break;
            default:
                break;
        }

        if (!matrix.isIdentity()) {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return rotated;
        }
        return bitmap;
    }

    private Bitmap addGpsMapCameraStamp(
            Bitmap source,
            String village,
            Calendar schedule,
            long capturedAt,
            Location location
    ) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        OverlayRenderer.OverlayData data = createOverlayData(
                village,
                schedule.getTimeInMillis(),
                capturedAt,
                location
        );
        OverlayRenderer.draw(canvas, result.getWidth(), result.getHeight(), data);
        return result;
    }

    private void writeExif(
            Uri uri,
            Location location,
            long capturedAt,
            String village,
            String locationCode
    ) throws IOException {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "rw")) {
            if (pfd == null) throw new IOException("File EXIF tidak dapat dibuka.");
            ExifInterface exif = new ExifInterface(pfd.getFileDescriptor());
            exif.setGpsInfo(location);

            SimpleDateFormat exifDate = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
            exifDate.setTimeZone(WITA);
            String dateValue = exifDate.format(capturedAt);
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateValue);
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateValue);
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateValue);
            exif.setAttribute(
                    ExifInterface.TAG_USER_COMMENT,
                    "Desa=" + village
                            + "; KodeGPS=" + locationCode
            );
            exif.saveAttributes();
        }
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private String buildLocationCode(String village, long capturedAt) {
        Map<String, String> codes = new HashMap<>();
        codes.put("Tanete", "TNT");
        codes.put("Pamatata", "PMT");
        codes.put("Bungaiya", "BGY");
        codes.put("Menara Indah", "MNI");
        codes.put("Kayu Bauk", "KYB");

        String villageCode = codes.getOrDefault(village, "BTM");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        formatter.setTimeZone(WITA);
        return "BTM-" + villageCode + "-" + formatter.format(capturedAt);
    }

    private void onPhotoSaved(Uri uri, Location location) {
        lastSavedUri = uri;
        binding.btnLastPhoto.setEnabled(true);
        restoreCaptureButton();
        Toast.makeText(
                this,
                "Foto tersimpan di Galeri > Pictures > GPS Bontomatene\nKoordinat: "
                        + formatCoordinate(location.getLatitude())
                        + ", "
                        + formatCoordinate(location.getLongitude()),
                Toast.LENGTH_LONG
        ).show();
    }

    private void restoreCaptureButton() {
        binding.btnCapture.setEnabled(true);
        binding.btnCapture.setText("AMBIL FOTO");
    }

    private void openLastPhoto() {
        if (lastSavedUri == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(lastSavedUri, "image/jpeg");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Aplikasi galeri tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockRunnable);
        cameraExecutor.shutdown();
        binding = null;
    }
}
