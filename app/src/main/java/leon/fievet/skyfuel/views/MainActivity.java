package leon.fievet.skyfuel.views;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import leon.fievet.skyfuel.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ecouteBtn(((Button) findViewById(R.id.btnAddBat)), AddBatActivity.class);
        ecouteBtn(((Button) findViewById(R.id.btnStock)), StorageActivity.class);
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
}