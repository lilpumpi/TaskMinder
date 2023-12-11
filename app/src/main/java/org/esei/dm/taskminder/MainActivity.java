package org.esei.dm.taskminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public DBManager gestorDB;
    private CustomCursorAdapter adaptadorDB;
    private Calendar calendar;
    private int COD_INSERTAR = 100;
    private int COD_MODIFICAR = 102;
    private int filtroActual = 0; //0->todas    1->pendientes   2->finalizadas

    private ListView lvLista;
    private Button btAdd;


    //----------------------------------------------------------------------------------------
    //  GESTION DEL CICLO DE VIDA
    //----------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_main );

        lvLista = findViewById(R.id.lv_lista);
        btAdd = findViewById(R.id.bt_add);
        this.gestorDB = new DBManager(this.getApplicationContext());

        //Habilitamos el boton de insertar
        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendar = Calendar.getInstance(); //Guardamos la fecha actual
                lanzaEditor(COD_INSERTAR, -1, "", "", calendar, false);
            }
        });

        lvLista.setLongClickable(true);
        this.registerForContextMenu(lvLista);
    }


    //Se ejecuta cuando la actividad vuelve a ejecutarse despues de un onPause
    @Override
    public void onStart() {
        super.onStart();

        //Configuramos la lista con su adaptador para que muestre los valores de la base de datos
        lvLista = findViewById(R.id.lv_lista);

        this.adaptadorDB = new CustomCursorAdapter(this, null);


        lvLista.setAdapter(this.adaptadorDB);
        actualizar(filtroActual);
        comprobarFechas();
    }

    public void onPause(){
        super.onPause();

        this.gestorDB.close();
        if(this.adaptadorDB.getCursor() != null){
            this.adaptadorDB.getCursor().close();
        }
    }

    public void actualizar(int filtro){
        this.adaptadorDB.changeCursor(gestorDB.getTareasFiltradas(filtro)); //Actualizar lista en funcion de los parametros de filtrado
    }


    //--------------------------------------------------------------------------
    // GESTION DE LANZAMIENTO DE ACTIVIDADES
    //--------------------------------------------------------------------------


    //Se encarga de lanzar la actividad de editor diferenciando entre modificacion e insercion
    private void lanzaEditor(int codigo, int id, String nombre, String descripcion, Calendar fecha, boolean completado){
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);

        intent.putExtra("id", id);
        intent.putExtra("nombre", nombre);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("fechaString", formatDateToString(fecha));
        intent.putExtra("completado", completado);

        MainActivity.this.startActivityForResult(intent, codigo);
    }


    //Realizamos los cambios en funcion de los datos introducidos en la actividad Editor

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            //Recuperamos los datos del intent
            String stringId = Integer.toString(data.getExtras().getInt("id"));
            String nombre = data.getExtras().getString("nombre");
            String descripcion = data.getExtras().getString("descripcion");
            String fechaString = data.getExtras().getString("fechaString");
            boolean completado = data.getBooleanExtra("completado", false);

            this.gestorDB.insertarTarea(stringId, nombre, descripcion, fechaString, completado);

        }
    }


    //--------------------------------------------------------------------------
    // GESTION DE MENUS
    //--------------------------------------------------------------------------

    //Creamos el menu de opciones
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.options_borrar:
                gestorDB.eliminarTodo();
                actualizar(filtroActual);
                break;

            case R.id.options_resumen:
                mostrarResumen();
                break;

            case R.id.options_filtrar:
                filtrarTareas();
                break;
        }

        return true;
    }

    //Creamos el menu contextual
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, v, info);
        Log.i("Menu", "------------------------------------------------------------Creasndo menu contextual");
        if(v.getId() == R.id.lv_lista){
            this.getMenuInflater().inflate(R.menu.context_menu, menu);
        }
    }

    public boolean onContextItemSelected(MenuItem menuItem){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        Cursor cursor = this.adaptadorDB.getCursor();
        int pos = info.position;

        switch(menuItem.getItemId()) {

            case R.id.context_borrar:
                if (cursor.moveToPosition(pos)) {
                    int id = cursor.getInt(0);//0 porque el id es el primer argumento
                    gestorDB.eliminarTarea(id);
                    actualizar(filtroActual);
                } else {
                    Log.e("Context.eliminar", "Posicion incorrecta");
                    Toast.makeText(this, "Posicion incorrecta", Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.context_modificar:
                if (cursor.moveToPosition(pos)) {
                    int id = cursor.getInt(0);//0 porque el id es el primer argumento
                    String nombre = cursor.getString(1); //Segundo argumento
                    String descripcion = cursor.getString(2); //Tercer argumento
                    Log.i("DESCRIPCION", "-------------------------------------------------------------- " + descripcion);
                    String fechaString = cursor.getString(3); //Cuarto argumento
                    boolean completado = (cursor.getInt(4) == 1); //El quinto argumento es booleando, pero se almacena como 1 o 0 en la bd

                    lanzaEditor(COD_MODIFICAR, id, nombre, descripcion, formatStringToDate(fechaString), completado);
                } else {
                    Log.e("Context.modicficar", "Posicion incorrecta");
                    Toast.makeText(this, "Posicion incorrecta", Toast.LENGTH_SHORT).show();
                }

                break;
        }

        return true;
    }


    //----------------------------------------------------------------------------------------
    //  GESTION DE FECHAS
    //----------------------------------------------------------------------------------------

    // Método para parsear la fecha a partir de un String
    private Calendar formatStringToDate(String fechaStr) {
        Calendar fecha = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            fecha.setTime(dateFormat.parse(fechaStr));
        } catch (ParseException e) {
            Log.e("DBManager", "Error al parsear la fecha: " + e.getMessage());
        }
        return fecha;
    }

    // Método para formatear la fecha a texto
    private String formatDateToString(Calendar fecha) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(fecha.getTime());
    }

    //Comprueba que la fecha de cada tarea sea superior a la actual
    private void comprobarFechas(){
        Log.i("ComprobarFecha", "Comprobadno fechas");
        Calendar fechaActual = Calendar.getInstance();
        String fechaActualString = formatDateToString(fechaActual);
        Log.i("ComprobarFecha", "Elementos del adaptador: " + adaptadorDB.getCount());

        //Recorremos los elementos de la lista
        for(int i=0; i<adaptadorDB.getCount(); i++){
            Log.i("ComprobarFecha", "Comprobadno fechas -------- for");
            Cursor cursor = (Cursor) adaptadorDB.getItem(i);
            String fechaString = cursor.getString(cursor.getColumnIndex(DBManager.COL_FECHA));
            int idTarea = cursor.getInt(cursor.getColumnIndex(DBManager.COL_ID));
            Calendar fechaCaducidad = formatStringToDate(fechaString);

            Log.i("FECHAS", "Fecha actual: " + fechaActualString + "------------ Fecha caducidad: " + fechaString);

            if(fechaActual.after(fechaCaducidad)){ //La tarea ha caducado, la marcamos como finalizada
                gestorDB.marcarTareaComoCompletada(Integer.toString(idTarea));
                advertenciaCaducidad(cursor);
                Log.i("FechaCaducidad", "--------------------------------------------- La tarea ha caducado ID:" + idTarea);
            }
        }

        actualizar(filtroActual);
    }


    //----------------------------------------------------------------------------------------
    //  GESTION DE ALTER DIALOGS
    //----------------------------------------------------------------------------------------

    private void advertenciaCaducidad(Cursor cursor){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Datos del cursor
        int idTarea = cursor.getInt(cursor.getColumnIndex(DBManager.COL_ID));
        String nombreTarea = cursor.getString(cursor.getColumnIndex(DBManager.COL_NOMBRE));

        String titulo = "La tarea " + nombreTarea + " (ID: " + idTarea + ") ha caducado!";

        builder.setTitle(titulo);
        builder.setMessage("La tarea se ha marcado como finalizada ya que ha caducado.");
        builder.setMessage("Borre la tarea o modifique su fecha de caducidad.");

        builder.setNegativeButton("Cerrar", null);
        builder.create().show();

    }

    private void mostrarResumen() {
        int tareasFinalizadas = 0;
        int tareasPendientes = 0;

        Cursor cursor = gestorDB.getTareas(); // Obtener todas las tareas directamente desde la base de datos

        // Recorremos los elementos del cursor
        if (cursor.moveToFirst()) {
            do {
                int completado = cursor.getInt(cursor.getColumnIndex(DBManager.COL_COMPLETADO));

                if (completado == 1) { // Tarea completada
                    tareasFinalizadas++;
                } else if (completado == 0) { // Tarea pendiente
                    tareasPendientes++;
                }
            } while (cursor.moveToNext());
        }

        cursor.close(); // Cerrar el cursor después de usarlo

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View resumenView = this.getLayoutInflater().inflate(R.layout.resumen_activity, null);
        builder.setView(resumenView);
        builder.setTitle("Resumen");

        TextView tvTotal = resumenView.findViewById(R.id.tv_resumen_numTotal);
        TextView tvPendientes = resumenView.findViewById(R.id.tv_resumen_numPendientes);
        TextView tvFinalizadas = resumenView.findViewById(R.id.tv_resumen_numFinalizadas);

        // Mostramos los datos
        tvTotal.setText(Integer.toString(tareasFinalizadas + tareasPendientes));
        tvPendientes.setText(Integer.toString(tareasPendientes));
        tvFinalizadas.setText(Integer.toString(tareasFinalizadas));

        builder.setNegativeButton("Cerrar", null);
        builder.create().show();
    }



    private void filtrarTareas(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Filtrar lista de tareas");
        String[] options = {"Todas", "Pendientes", "Finalizadas"};

        builder.setSingleChoiceItems(options, filtroActual, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int opcion) {
                switch (opcion){
                    //0->todas    1->pendientes   2->finalizadas
                    case 0: filtroActual = DBManager.FILTRO_TODAS;
                            break;

                    case 1: filtroActual = DBManager.FILTRO_PENDIENTES;

                            break;
                    case 2: filtroActual = DBManager.FILTRO_FINALIZADAS;
                            break;
                }
            }
        });

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Cerrar el diálogo al hacer clic en Aceptar
                actualizar(filtroActual); // Llamar al método de filtrado con la opción seleccionada
            }
        });

        builder.create().show();

    }


    //Devuelve el filtro, es necesario para el adaptador
    public int getFiltro(){
        return filtroActual;
    }


}
