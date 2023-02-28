package leon.fievet.skyfuel.views;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.zxing.WriterException;

import java.io.IOException;

import leon.fievet.skyfuel.R;
import leon.fievet.skyfuel.controler.Control;

public class AddBatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bat);
        init();
    }

    private EditText txtNbCells;
    private EditText txtCapacity;
    private RadioButton rbFull;
    private RadioButton rbStorage;
    private ImageView imgQr;
    private Control control;


    public void init(){
        txtNbCells = findViewById(R.id.txtNbCells);
        txtCapacity = findViewById(R.id.txtCapacity);
        rbFull = findViewById(R.id.rbFull);
        rbStorage = findViewById(R.id.rbStorage);
        imgQr = findViewById(R.id.imgQr);
        this.control = Control.getInstance(this);
        ecouteSaveBat();
    }

    public void ecouteSaveBat() {
        ((Button) findViewById(R.id.btnSaveBat)).setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                int nbCellsEc = 0;
                int capacityEc = 0;
                int etatChargeEC = 0;
                // récupération des valeurs saisies
                try {
                    nbCellsEc = Integer.parseInt(txtNbCells.getText().toString());
                    capacityEc = Integer.parseInt(txtCapacity.getText().toString());
                } catch (Exception ignored) {
                }
                if (rbStorage.isChecked()) {
                    etatChargeEC = 1;
                } else if (rbFull.isChecked()) {
                    etatChargeEC = 2;
                }
                // contrôle des valeurs saisies
                if (nbCellsEc == 0 || capacityEc == 0) {
                    Toast.makeText(AddBatActivity.this, "Veuillez saisir toutes les valeurs", Toast.LENGTH_SHORT).show();
                } else {
                    // affichage du résultat
                    try {
                        afficheResultat(nbCellsEc, capacityEc, etatChargeEC);
                    } catch (WriterException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void afficheResultat(Integer nbCellsEc, Integer capacityEc, Integer etatChargeEC) throws WriterException, IOException {
        this.control.creerBattery(nbCellsEc, capacityEc, etatChargeEC);
        imgQr.setImageBitmap(this.control.getQRCode(this));
    }
}