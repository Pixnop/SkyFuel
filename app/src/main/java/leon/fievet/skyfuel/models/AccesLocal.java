package leon.fievet.skyfuel.models;

import static leon.fievet.skyfuel.tools.outils.stringToDate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.time.OffsetDateTime;
import java.util.ArrayList;

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
        String req = "INSERT INTO battery (nbCells, capacity, etatCharge, dateEnregistrement, dateDerniereMisAJour, data) VALUES (?, ?, ?, ?, ?, ?)";
        SQLiteStatement statement = bd.compileStatement(req);
        statement.bindLong(1, battery.getNbCells());
        statement.bindLong(2, battery.getCapacity());
        statement.bindLong(3, battery.getEtatCharge());
        statement.bindString(4, battery.getDateEnregistrement().toString());
        statement.bindString(5, battery.getDateDerniereMisAJour().toString());
        statement.bindString(6, battery.getData());
        long id = statement.executeInsert();
        statement.close();
        bd.close();
        return id;
    }

    public Battery recupDernier() {
        bd = accesBD.getReadableDatabase();
        Battery battery = null;
        String req = "select * from battery;";
        Cursor curseur = bd.rawQuery(req, null);
        curseur.moveToLast();
        if (!curseur.isAfterLast()){
            Integer id = curseur.getInt(0);
            Integer nbCells = curseur.getInt(1);
            Integer capacity = curseur.getInt(2);
            Integer EtatCharge = curseur.getInt(3);
            OffsetDateTime dateEnregistrement = stringToDate(curseur.getString(4));
            OffsetDateTime dateDerniereMisAJour = stringToDate(curseur.getString(5));
            String data = curseur.getString(6);
            battery = new Battery(nbCells, capacity, EtatCharge, dateEnregistrement);
            battery.setDateDerniereMisAJour(dateDerniereMisAJour);
            battery.setData(data);
        }
        curseur.close();
        return battery;
    }

    public static ArrayList<Battery> recupTous() {
        bd = accesBD.getReadableDatabase();
        ArrayList<Battery> lesBatteries = new ArrayList<Battery>();
        String req = "select * from battery;";
        Cursor curseur = bd.rawQuery(req, null);
        curseur.moveToFirst();
        while (!curseur.isAfterLast()){
            Integer id = curseur.getInt(0);
            Integer nbCells = curseur.getInt(1);
            Integer capacity = curseur.getInt(2);
            Integer EtatCharge = curseur.getInt(3);
            OffsetDateTime dateEnregistrement = stringToDate(curseur.getString(4));
            OffsetDateTime dateDerniereMisAJour = stringToDate(curseur.getString(5));
            String data = curseur.getString(6);
            Battery battery = new Battery(nbCells, capacity, EtatCharge, dateEnregistrement);
            battery.setId(id);
            battery.setDateDerniereMisAJour(dateDerniereMisAJour);
            battery.setData(data);
            lesBatteries.add(battery);
            curseur.moveToNext();
        }
        curseur.close();
        return lesBatteries;
    }

    public static void suprimmerProfil(Battery battery) {
        bd = accesBD.getWritableDatabase();
        String req = "delete from battery where id = '" + battery.getId() + "';";
        bd.execSQL(req);
    }

}
