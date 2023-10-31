package leon.fievet.skyfuel.views;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.RadioGroup;

import leon.fievet.skyfuel.R;
import leon.fievet.skyfuel.models.Battery;
import leon.fievet.skyfuel.controler.Control;

import androidx.appcompat.app.AppCompatActivity;

public class EditBatteryActivity extends AppCompatActivity {
    private int batteryId;
    private Battery battery;
    private Control controle;

    private EditText editNbCells;
    private EditText editCapacity;
    private Button btnSaveChanges;
    private RadioGroup rgEtatCharge;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_battery); // Layout hypothétique pour l'édition

        controle = Control.getInstance(this);

        batteryId = getIntent().getIntExtra("batteryId", -1);
        if (batteryId != -1) {
            battery = controle.getBatteryById(batteryId); // Méthode hypothétique pour obtenir une batterie par ID
            initViews();
            displayBatteryInfo();
        }
    }

    private void initViews() {
        editNbCells = findViewById(R.id.editNbCells);
        editCapacity = findViewById(R.id.editCapacity);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        rgEtatCharge = findViewById(R.id.rgEtatCharge);

        btnSaveChanges.setOnClickListener(v -> {
            updateBatteryInfo();
        });
    }

    private void displayBatteryInfo() {
        editNbCells.setText(String.valueOf(battery.getNbCells()));
        editCapacity.setText(String.valueOf(battery.getCapacity()));

        int etatCharge = battery.getEtatCharge();
        if (etatCharge == 0) { // Supposons que 0 représente "Low"
            rgEtatCharge.check(R.id.rbLow);
        } else if (etatCharge == 1) { // Supposons que 1 représente "Storage"
            rgEtatCharge.check(R.id.rbStorage);
        } else if (etatCharge == 2) { // Supposons que 2 représente "Full"
            rgEtatCharge.check(R.id.rbFull);
        }
    }


    private void updateBatteryInfo() {
        int newNbCells = Integer.parseInt(editNbCells.getText().toString());
        int newCapacity = Integer.parseInt(editCapacity.getText().toString());
        int newEtatCharge;

        int checkedId = rgEtatCharge.getCheckedRadioButtonId();
        if (checkedId == R.id.rbLow) {
            newEtatCharge = 0;
        } else if (checkedId == R.id.rbStorage) {
            newEtatCharge = 1;
        } else {
            newEtatCharge = 2;
        }

        battery.updateBattery(newNbCells, newCapacity, newEtatCharge);
        controle.updateBattery(battery);

        finish();
    }

}
