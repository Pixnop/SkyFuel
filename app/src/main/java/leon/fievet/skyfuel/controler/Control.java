package leon.fievet.skyfuel.controler;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.zxing.WriterException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import leon.fievet.skyfuel.models.AccesLocal;
import leon.fievet.skyfuel.models.Battery;

public final class Control {
    private static Control instance = null;
    private static Battery battery;
    private Integer id = 1;
    private static AccesLocal accesLocal;

    /**
     * Constructeur privé
     */
    private Control() {
        super();
    }

    public static final Control getInstance(Context contexte) {
        if (Control.instance == null) {
            Control.instance = new Control();
            accesLocal = new AccesLocal(contexte);
            battery = accesLocal.recupDernier();

        }
        return Control.instance;
    }

    public void creerBattery(Integer nbCells, Integer capacity, Integer etatCharge) {
        battery = new Battery(nbCells, capacity, etatCharge, OffsetDateTime.now());
        int id = (int) accesLocal.ajout(battery);
        battery.setId(id);
    }

    public Integer getNbCells() {
        return battery.getNbCells();
    }

    public Integer getCapacity() {
        return battery.getCapacity();
    }

    public Integer getEtatCharge() {
        return battery.getEtatCharge();
    }

    public OffsetDateTime getDateEnregistrement() {
        return battery.getDateEnregistrement();
    }

    public OffsetDateTime getDateDerniereMisAJour() {
        return battery.getDateDerniereMisAJour();
    }

    public String getData() {
        return battery.getData();
    }

    public Bitmap getQRCode(Context context) throws WriterException, IOException {
        return battery.getQRCode(context);
    }

    public ArrayList<Battery> getAllBat() {
        return AccesLocal.recupTous();
    }
}
