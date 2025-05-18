package com.example.kotprog;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormActivity extends AppCompatActivity {

    private EditText systolicInput;
    private EditText diastolicInput;
    private EditText pulseInput;
    private Button saveButton;

    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;

    // Engedély kérése kezelő
    private ActivityResultLauncher<String> locationPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        systolicInput = findViewById(R.id.systolicInput);
        diastolicInput = findViewById(R.id.diastolicInput);
        pulseInput = findViewById(R.id.pulseInput);
        saveButton = findViewById(R.id.button6);

        // SharedPreferences visszatöltés
        SharedPreferences prefs = getSharedPreferences("MeasurementPrefs", MODE_PRIVATE);
        systolicInput.setText(prefs.getString("tmp_systolic", ""));
        diastolicInput.setText(prefs.getString("tmp_diastolic", ""));
        pulseInput.setText(prefs.getString("tmp_pulse", ""));

        // Location client inicializálása
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Engedély kérő regisztráció
        locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        getLastLocationAndSave();
                    } else {
                        Toast.makeText(this, "Helymeghatározási engedély megtagadva", Toast.LENGTH_SHORT).show();
                        // Ha nincs engedély, akkor csak adatokat mentünk GPS nélkül
                        saveDataToCSV(null);
                    }
                });

        saveButton.setOnClickListener(v -> {
            // Ellenőrizzük az engedélyt
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndSave();
            } else {
                // Kérjük az engedélyt
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
    }

    private void getLastLocationAndSave() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastKnownLocation = location;
                    }
                    saveDataToCSV(lastKnownLocation);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Helyzet lekérése sikertelen", Toast.LENGTH_SHORT).show();
                    saveDataToCSV(null);
                });
    }

    private void saveDataToCSV(Location location) {
        String systolic = systolicInput.getText().toString();
        String diastolic = diastolicInput.getText().toString();
        String pulse = pulseInput.getText().toString();

        // Ellenőrzés: ha hiányzik adat, ne mentse
        if (systolic.isEmpty() || diastolic.isEmpty() || pulse.isEmpty()) {
            Toast.makeText(this, "Kérlek, töltsd ki az összes mezőt!", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        String latitude = (location != null) ? String.valueOf(location.getLatitude()) : "";
        String longitude = (location != null) ? String.valueOf(location.getLongitude()) : "";

        // A CSV sor
        String csvLine = date + "," + time + "," + systolic + "," + diastolic + "," + pulse + "," + latitude + "," + longitude + "\n";

        // Az app saját külső mappája (Scoped Storage kompatibilis)
        File folder = new File(getExternalFilesDir(null), "vernyomas");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File csvFile = new File(folder, "vernyomas_adatok.csv");

        try {
            boolean isNewFile = !csvFile.exists();
            FileWriter writer = new FileWriter(csvFile, true); // hozzáfűzés

            // Ha új fájl, akkor fejléc írása
            if (isNewFile) {
                String header = "Dátum,Idő,Systolés,Diasztolés,Pulzus,Latitude,Longitude\n";
                writer.append(header);
            }

            writer.append(csvLine);
            writer.flush();
            writer.close();

            Toast.makeText(this, "Adatok exportálva: " + csvFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Ürítsük az EditTexteket, ha akarod
            systolicInput.setText("");
            diastolicInput.setText("");
            pulseInput.setText("");

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Hiba történt az exportálás során", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences prefs = getSharedPreferences("MeasurementPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("tmp_systolic", systolicInput.getText().toString());
        editor.putString("tmp_diastolic", diastolicInput.getText().toString());
        editor.putString("tmp_pulse", pulseInput.getText().toString());

        editor.apply();
    }

    public void back(View view) {

        Intent intent = new Intent(this, UserPageActivity.class);
        startActivity(intent);
        finish();
    }


}
