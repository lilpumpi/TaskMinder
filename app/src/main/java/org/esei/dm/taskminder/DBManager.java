package org.esei.dm.taskminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBManager extends SQLiteOpenHelper {

    //Datos de la base de datos
    public static final String DB_NOMBRE = "Taskminder";
    public static final String TABLA_TAREA = "tarea";
    public static final int DB_VERSION = 2;

    //Atributos de la tarea
    public static final String COL_ID = "_id";
    public static final String COL_NOMBRE = "nombre";
    public static final String COL_DESCRIPCION = "descripcion";
    public static final String COL_FECHA = "fecha";
    public static final String COL_COMPLETADO = "completado";

    //Atributos para filtrar
    public static final int FILTRO_TODAS = 0;
    public static final int FILTRO_PENDIENTES = 1;
    public static final int FILTRO_FINALIZADAS = 2;



    //Constructor
    public DBManager(Context context){
        super(context, DB_NOMBRE, null, DB_VERSION);
    }

    //Se encarga de crear una tabla para las tareas con los atributos correspondientes
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("DBManager", "Creando BBDD " + DB_NOMBRE + " v" + DB_VERSION);

        try{
            db.beginTransaction();
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLA_TAREA + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " //ID de la tarea, numero incremental automatico
                + COL_NOMBRE + " TEXT NOT NULL, " //Nombre de la tarea
                + COL_DESCRIPCION + " TEXT NOT NULL, " //Descripcion de la tarea
                + COL_FECHA + " TEXT NOT NULL, " //Fecha de caducidad de la tarea
                + COL_COMPLETADO + " INTEGER DEFAULT 0)"); //Booleano que indica si se ha completado o no

            db.setTransactionSuccessful();
        } catch (SQLException exc){
            Log.e("DBManager.onCreate()", exc.getMessage());
        } finally {
            db.endTransaction();
        }
    }


    //Actualiza la base de datos
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DBManager", "DB " + DB_NOMBRE + ": v" + oldVersion + " -> v" + newVersion);

        try{
            db.beginTransaction();
            db.execSQL("DROP TABLE IF EXISTS " + TABLA_TAREA);
            db.setTransactionSuccessful();

        } catch(SQLException exc){
            Log.e("DBManager.onUpgrade", exc.getMessage());
        } finally {
            db.endTransaction();
        }

        this.onCreate(db);
    }


    //Devuelve todas las tareas que hay en la tabla
    public Cursor getTareas(){
        return this.getReadableDatabase().query(TABLA_TAREA, null, null, null, null, null, null);
    }


    public Cursor getTareasFiltradas(int filtro) {
        SQLiteDatabase db = this.getReadableDatabase();
        String where = null;
        String[] whereArgs = null;

        switch (filtro) {
            case FILTRO_PENDIENTES:
                where = COL_COMPLETADO + "=?";
                whereArgs = new String[]{"0"}; //Devuelve las que su atributo completado=0, es decir pendientes
                break;
            case FILTRO_FINALIZADAS:
                where = COL_COMPLETADO + "=?";
                whereArgs = new String[]{"1"}; //Devuelve las que su atributo completado=1, es decir finalizadas
                break;
            // Caso por defecto para obtener todas las tareas
            default:
                where = null;
                whereArgs = null; //Null significa que devuelve todos
                break;
        }

        return db.query(TABLA_TAREA, null, where, whereArgs, null, null, null);
    }


    //Inserta una actividad
    public boolean insertarTarea(String stringId, String nombre, String descripcion, String fecha, boolean comlpetado){
        Cursor cursor = null;
        boolean toret = false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues(); //Aqui guardaremos los valores que queramos añadir

        values.put(COL_NOMBRE, nombre);
        values.put(COL_DESCRIPCION, descripcion);
        values.put(COL_FECHA, fecha);
        values.put(COL_COMPLETADO, comlpetado? 1:0);

        try{
            db.beginTransaction();
            cursor = db.query(
                    TABLA_TAREA, //Nombre de la tabla
                    null, //Lista de columnas (null = todas)
                    COL_ID + "=?", //Clausula WHERE
                    new String[]{stringId}, //Atributos de la clausula where
                    null, null, null, null
            );

            if(cursor.getCount() > 0){ //Existe la tarea con ese ID, se actualiza sus datos
                db.update(TABLA_TAREA, values, COL_ID + "=?", new String[]{stringId});
            } else{ //No existe la tarea con ese ID, crear nueva
                db.insert(TABLA_TAREA, null, values);
            }

            db.setTransactionSuccessful();
            toret = true;
            Log.i("TAREA INSERTADA", "Tarea insertada-> id:" + stringId + " completada:" + comlpetado + "-------------------");
        } catch (SQLException exc){
            Log.e("DBManager.inserta", exc.getMessage());
        } finally {
            if(cursor != null){ cursor.close(); }

            db.endTransaction();
        }

        return toret;
    }

    //Elimina una tarea de la base de datos
    public boolean eliminarTarea(int id){
        boolean toret = false;
        SQLiteDatabase db = this.getWritableDatabase();
        String stringId = Integer.toString(id);

        try{
            db.beginTransaction();
            db.delete(TABLA_TAREA, COL_ID + "=?", new String[]{stringId});
            db.setTransactionSuccessful();
            toret = true;

        } catch(SQLException exc){
            Log.e("DBManager.elimina", exc.getMessage());
        } finally{
            db.endTransaction();
        }

        return toret;
    }

    //Borra todas las tareas de la base de datos
    public void eliminarTodo(){
        SQLiteDatabase db = this.getWritableDatabase();

        try{
            db.beginTransaction();
            db.delete(TABLA_TAREA, null, null);
            db.setTransactionSuccessful();

        } catch(SQLException exc){
            Log.e("DBManager.borrarTodo", exc.getMessage());
        } finally{
            db.endTransaction();
        }

    }


    public void marcarTareaComoCompletada(String stringId){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_COMPLETADO, 1); //1 representa true, que esta completada la tarea

        try{
            db.beginTransaction();
            db.update(TABLA_TAREA, values, COL_ID + "=?", new String[]{stringId});
            db.setTransactionSuccessful();
            Log.i("Completar", "Tarea marcada como finalizada--------------------------------------------------------------");
        } catch(SQLException exc){
            Log.e("DBManager.completarTarea", exc.getMessage());
        } finally{
            db.endTransaction();
        }

    }


}
