package leon.fievet.skyfuel.views;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

import leon.fievet.skyfuel.R;
import leon.fievet.skyfuel.controler.Control;
import leon.fievet.skyfuel.models.Battery;

public class StorageActivity extends AppCompatActivity {
    private Control controle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        this.controle = Control.getInstance(this);
        afficheStorage();
    }

    private void afficheStorage() {
        ArrayList<Battery> lesBattery = controle.getAllBat();
        if (lesBattery.size() > 0) {
            // affichage de la liste
            ListView lv = findViewById(R.id.lsStorage);
            StorageListAdaptater adapteur = new StorageListAdaptater(lesBattery, this);
            lv.setAdapter(adapteur);
        }
    }
}