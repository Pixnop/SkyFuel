package leon.fievet.skyfuel.models;

import static leon.fievet.skyfuel.tools.outils.stringToDate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import leon.fievet.skyfuel.tools.MySQLiteOpenHelper;

public class AccesLocal {
    // propriétés
    private String nomBase = "bdSkyFuel.sqlite";
    private Integer versionBase = 1;
    private static MySQLiteOpenHelper accesBD;
    private static SQLiteDatabase bd;

    /**
     * Constructeur de la classe
     * @param contexte contexte de l'application
     */
    public AccesLocal(Context contexte) {
        accesBD = new MySQLiteOpenHelper(contexte, nomBase,null, versionBase);
    }

    /**
     * Ajoute une batterie dans la base de données
     * @param battery batterie à ajouter
     */
    public long ajout(Battery battery) {
        bd = accesBD.getWritableDatabase();
        String req = "INSERT INTO battery (id, nbCells, capacity, etatCharge, dateEnregistrement, dateDerniereMisAJour, data) VALUES (?, ?, ?, ?, ?, ?, ?)";
        SQLiteStatement statement = bd.compileStatement(req);
        statement.bindString(1, battery.getId().toString());
        statement.bindLong(2, battery.getNbCells());
        statement.bindLong(3, battery.getCapacity());
        statement.bindLong(4, battery.getEtatCharge());
        statement.bindString(5, battery.getDateEnregistrement().toString());
        statement.bindString(6, battery.getDateDerniereMisAJour().toString());
        statement.bindString(7, battery.getData());
        long id = statement.executeInsert();
        statement.close();
        bd.close();
        return id;
    }

    public Battery recupDernier() throws JSONException {
        bd = accesBD.getReadableDatabase();
        Battery battery = null;
        String req = "select * from battery;";
        Cursor curseur = bd.rawQuery(req, null);
        curseur.moveToLast();
        if (!curseur.isAfterLast()){
            UUID id = UUID.fromString(curseur.getString(0));
            Integer nbCells = curseur.getInt(1);
            Integer capacity = curseur.getInt(2);
            Integer EtatCharge = curseur.getInt(3);
            OffsetDateTime dateEnregistrement = stringToDate(curseur.getString(4));
            OffsetDateTime dateDerniereMisAJour = stringToDate(curseur.getString(5));
            String data = curseur.getString(6);
            battery = new Battery(id,nbCells, capacity, EtatCharge, dateEnregistrement);
            battery.setDateDerniereMisAJour(dateDerniereMisAJour);
            battery.setData(data);
        }
        curseur.close();
        return battery;
    }

    public static ArrayList<Battery> recupTous() throws JSONException {
        bd = accesBD.getReadableDatabase();
        ArrayList<Battery> lesBatteries = new ArrayList<>();
        String req = "select * from battery;";
        Cursor curseur = bd.rawQuery(req, null);
        curseur.moveToFirst();
        while (!curseur.isAfterLast()) {
            String uuidString = curseur.getString(0);
            UUID id = null;
            if (uuidString != null && !uuidString.isEmpty()) {
                try {
                    id = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            if (id != null) {
                Integer nbCells = curseur.getInt(1);
                Integer capacity = curseur.getInt(2);
                Integer etatCharge = curseur.getInt(3);
                OffsetDateTime dateEnregistrement = stringToDate(curseur.getString(4));
                OffsetDateTime dateDerniereMisAJour = stringToDate(curseur.getString(5));
                String data = curseur.getString(6);
                Battery battery = new Battery(id, nbCells, capacity, etatCharge, dateEnregistrement);
                battery.setId(id);
                battery.setDateDerniereMisAJour(dateDerniereMisAJour);
                battery.setData(data);
                lesBatteries.add(battery);
            }
            curseur.moveToNext();
        }
        curseur.close();
        return lesBatteries;
    }


    public static void suprimmerProfil(Battery battery) {
        bd = accesBD.getWritableDatabase();
        String req = "delete from battery where id = '" + battery.getId().toString() + "';";
        bd.execSQL(req);
    }

    public static void updateBattery(Battery battery) {
        bd = accesBD.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nbCells", battery.getNbCells());
        values.put("capacity", battery.getCapacity());
        values.put("etatCharge", battery.getEtatCharge());
        values.put("dateDerniereMisAJour", battery.getDateDerniereMisAJour().toString());
        values.put("data", battery.getData());
        bd.update("battery", values, "id = ?", new String[]{battery.getId().toString()});
        bd.close();
    }

}
