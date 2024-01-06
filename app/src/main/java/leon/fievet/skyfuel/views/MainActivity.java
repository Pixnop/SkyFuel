package leon.fievet.skyfuel.views;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mikepenz.aboutlibraries.LibsBuilder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import leon.fievet.skyfuel.R;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Log.d("MainActivity", "Cancelled scan");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else if(originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Log.d("MainActivity", "Cancelled scan due to missing camera permission");
                        Toast.makeText(MainActivity.this, "Cancelled due to missing camera permission", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d("MainActivity", "Scanned");

                    // Extraction des données du scan
                    String[] data = result.getContents().split("/");
                    // Vérification que l'ID est au format UUID
                    Toast.makeText(MainActivity.this, "Battery ID: " + data[0], Toast.LENGTH_LONG).show();
                    try {
                        UUID batteryId = UUID.fromString(data[0]);
                        Log.d("MainActivity", "Battery ID: " + batteryId.toString());

                        // Redirection vers EditBatteryActivity avec l'ID de la batterie
                        Intent intent = new Intent(MainActivity.this, EditBatteryActivity.class);
                        intent.putExtra("batteryId", batteryId.toString());
                        startActivity(intent);
                    } catch (IllegalArgumentException e) {
                        Log.e("MainActivity", "Invalid UUID format: " + data[0], e);
                        Toast.makeText(MainActivity.this, "Invalid battery ID", Toast.LENGTH_LONG).show();
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ecouteBtn(((Button) findViewById(R.id.btnAddBat)), AddBatActivity.class);
        ecouteBtn(((Button) findViewById(R.id.btnStock)), StorageActivity.class);
        ecouteBtnScan();
    }

    /**
     * Ecouteur sur le bouton passé en paramètre
     * @param btn le bouton
     * @param classe la classe à lancer
     */
    private void ecouteBtn(Button btn, Class classe) {
        btn.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, classe);
                startActivity(intent);
            }
        });
    }

    private void ecouteBtnScan() {
        ((Button) findViewById(R.id.btnScan)).setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBarcode(v);
            }
        });
    }

    public void scanBarcode(View view) {
        ScanOptions options = new ScanOptions();
        options.setCaptureActivity(ScanActivity.class);
        options.setPrompt("Scan something");
        options.setOrientationLocked(false);
        options.setBeepEnabled(true);
        barcodeLauncher.launch(options);
    }
}