package leon.fievet.skyfuel.views;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.RadioGroup;

import leon.fievet.skyfuel.R;
import leon.fievet.skyfuel.models.Battery;
import leon.fievet.skyfuel.controler.Control;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class EditBatteryActivity extends AppCompatActivity {
    private int batteryId;
    private Battery battery;
    private Control controle;
    private Button btnSaveChanges;
    private RadioGroup rgEtatCharge;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_battery);

        controle = Control.getInstance(this);

        // Récupération de l'ID de la batterie sous forme de String
        String batteryIdString = getIntent().getStringExtra("batteryId");
        UUID batteryId = null;

        // Conversion de la String en UUID
        if (batteryIdString != null && !batteryIdString.isEmpty()) {
            try {
                batteryId = UUID.fromString(batteryIdString);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        if (batteryId != null) {
            battery = controle.getBatteryById(batteryId);
            initViews();
            displayBatteryInfo();
        }
    }


    private void initViews() {
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        rgEtatCharge = findViewById(R.id.rgEtatCharge);

        btnSaveChanges.setOnClickListener(v -> {
            updateBatteryInfo();
        });
    }

    private void displayBatteryInfo() {
        int etatCharge = battery.getEtatCharge();
        if (etatCharge == 0) {
            rgEtatCharge.check(R.id.rbLow);
        } else if (etatCharge == 1) {
            rgEtatCharge.check(R.id.rbStorage);
        } else if (etatCharge == 2) {
            rgEtatCharge.check(R.id.rbFull);
        }
    }


    private void updateBatteryInfo() {
        int newEtatCharge;

        int checkedId = rgEtatCharge.getCheckedRadioButtonId();
        if (checkedId == R.id.rbLow) {
            newEtatCharge = 0;
        } else if (checkedId == R.id.rbStorage) {
            newEtatCharge = 1;
        } else {
            newEtatCharge = 2;
        }

        battery.updateBattery(newEtatCharge);
        controle.updateBattery(battery);

        finish();
    }

}
