package org.esei.dm.listacompra4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/** Maneja el acceso a la base de datos. */
public class DBManager extends SQLiteOpenHelper {
    public static final String DB_NOMBRE = "ListaCompra";
    public static final int DB_VERSION = 2;

    public static final String TABLA_COMPRA = "compra";
    public static final String COMPRA_COL_NOMBRE = "_id";
    public static final String COMPRA_COL_NUM = "num";

    public DBManager(Context context)
    {
        super( context, DB_NOMBRE, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        Log.i(  "DBManager",
                "Creando BBDD " + DB_NOMBRE + " v" + DB_VERSION);

        try {
            db.beginTransaction();
            db.execSQL( "CREATE TABLE IF NOT EXISTS " + TABLA_COMPRA + "( "
                            + COMPRA_COL_NOMBRE + " string(255) PRIMARY KEY NOT NULL, "
                            + COMPRA_COL_NUM + " int NOT NULL)");
            db.setTransactionSuccessful();
        }
        catch(SQLException exc)
        {
            Log.e( "DBManager.onCreate", exc.getMessage() );
        }
        finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        Log.i(  "DBManager",
                "DB: " + DB_NOMBRE + ": v" + oldVersion + " -> v" + newVersion );

        try {
            db.beginTransaction();
            db.execSQL( "DROP TABLE IF EXISTS " + TABLA_COMPRA );
            db.setTransactionSuccessful();
        }  catch(SQLException exc) {
            Log.e( "DBManager.onUpgrade", exc.getMessage() );
        }
        finally {
            db.endTransaction();
        }

        this.onCreate( db );
    }

    /** Devuelve todas las compras en la BD
     * @return Un Cursor con las compras. */
    public Cursor getCompras()
    {
        return this.getReadableDatabase().query( TABLA_COMPRA,
                null, null, null, null, null, null );
    }

    /** Inserta un nuevo item.
     * @param nombre El nombre del item.
     * @param num La cantidad del item.
     * @return true si se pudo insertar (o modificar), false en otro caso.
     */
    public boolean insertaItem(String nombre, int num)
    {
        Cursor cursor = null;
        boolean toret = false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put( COMPRA_COL_NOMBRE, nombre );
        values.put( COMPRA_COL_NUM, num );

        try {
            db.beginTransaction();
            cursor = db.query( TABLA_COMPRA,
                                    null,
                                    COMPRA_COL_NOMBRE + "=?",
                                    new String[]{ nombre },
                                    null, null, null, null );

            if ( cursor.getCount() > 0 ) {
                db.update( TABLA_COMPRA,
                            values, COMPRA_COL_NOMBRE + "= ?", new String[]{ nombre } );
            } else {
                db.insert( TABLA_COMPRA, null, values );
            }

            db.setTransactionSuccessful();
            toret = true;
        } catch(SQLException exc)
        {
            Log.e( "DBManager.inserta", exc.getMessage() );
        }
        finally {
            if ( cursor != null ) {
                cursor.close();
            }

            db.endTransaction();
        }

        return toret;
    }

    /** Elimina un elemento de la base de datos
     * @param nombre El identificador del elemento.
     * @return true si se pudo eliminar, false en otro caso.
     */
    public boolean eliminaItem(String nombre)
    {
        boolean toret = false;
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();
            db.delete( TABLA_COMPRA, COMPRA_COL_NOMBRE + "=?", new String[]{ nombre } );
            db.setTransactionSuccessful();
            toret = true;
        } catch(SQLException exc) {
            Log.e( "DBManager.elimina", exc.getMessage() );
        } finally {
            db.endTransaction();
        }

        return toret;
    }
}
