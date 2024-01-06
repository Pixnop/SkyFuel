package leon.fievet.skyfuel.views;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONException;

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
        try {
            this.controle = Control.getInstance(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        try {
            afficheStorage();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void afficheStorage() throws JSONException {
        ArrayList<Battery> lesBattery = controle.getAllBat();
        if (lesBattery.size() > 0) {
            ListView lv = findViewById(R.id.lsStorage);
            StorageListAdaptater adapteur = new StorageListAdaptater(lesBattery, this);
            lv.setAdapter(adapteur);
        } else {
            Log.d("StorageActivity", "Aucune batterie à afficher.");
        }
    }

}