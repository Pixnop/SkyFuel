package leon.fievet.skyfuel.tools;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    // propriétés
    private String creation = "CREATE TABLE battery (" +
            "id TEXT PRIMARY KEY, " +
            "nbCells INTEGER NOT NULL, " +
            "capacity INTEGER NOT NULL, " +
            "etatCharge INTEGER NOT NULL, " +
            "dateEnregistrement TEXT NOT NULL, " +
            "dateDerniereMisAJour TEXT NOT NULL, " +
            "data TEXT NOT NULL);";


    /**
     * Constructeur de la classe
     * @param context contexte de l'application
     * @param name nom de la base de données
     * @param factory factory
     * @param version version de la base de données
     */
    public MySQLiteOpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /**
     * Création de la base de données
     * @param sqLiteDatabase base de données
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(creation);
    }

    /**
     * Mise à jour de la base de données
     * @param sqLiteDatabase base de données
     * @param i ancienne version
     * @param i1 nouvelle version
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
