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

    public DBManager gestorDB; //Gestiona la base de datos
    private CustomCursorAdapter adaptadorDB; //Adaptador para mostrar los datos en la lista
    private Calendar calendar;
    private int COD_INSERTAR = 100;
    private int COD_MODIFICAR = 102;
    private int filtroActual = 0; //Todas->0    Pendientes->1   Finalizadas->2

    private ListView lvLista;
    private Button btAdd;
    private TextView tv_filtro;


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
                lanzaEditor(COD_INSERTAR, -1, "", "", calendar, false); //Lanzamos editor para crear
            }
        });

        lvLista.setLongClickable(true); //Habilitamos pulsacion larga para le menu contextual
        this.registerForContextMenu(lvLista);
    }


    //Se ejecuta cuando la actividad vuelve a ejecutarse despues de un onPause
    @Override
    public void onStart() {
        super.onStart();
        lvLista = findViewById(R.id.lv_lista);

        //Creamos el adaptador personalizado para los elementos de la base de datos
        this.adaptadorDB = new CustomCursorAdapter(this, null);

        //Asignamos el adaptador a la lista para que muestre los datos
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

    //Actualizar lista en funcion de los parametros de filtrado
    public void actualizar(int filtro) {
        this.adaptadorDB.changeCursor(gestorDB.getTareasFiltradas(filtro));

        tv_filtro = (TextView) this.findViewById(R.id.tv_filtro);

        switch (filtro){
            case 0: tv_filtro.setText("Todas"); break;
            case 1: tv_filtro.setText("Pendientes"); break;
            case 2: tv_filtro.setText("Finalizadas"); break;

        }

    }


    //--------------------------------------------------------------------------
    // GESTION DE LANZAMIENTO DE ACTIVIDADES
    //--------------------------------------------------------------------------


    //Se encarga de lanzar la actividad de editor diferenciando entre modificacion e insercion
    private void lanzaEditor(int codigo, int id, String nombre, String descripcion, Calendar fecha, boolean completado){
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);

        intent.putExtra("id", id); //Si es -1 significa que va a ser una tarea nueva
        intent.putExtra("nombre", nombre);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("fechaString", formatDateToString(fecha));
        intent.putExtra("completado", completado);

        MainActivity.this.startActivityForResult(intent, codigo);
    }


    //Recibimos los datos del editor para crear/modificar la tarea
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

            //Inserta la tarea, si ya existe se actualiza
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

    //Gestionamos las opciones del menu de opciones
    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.options_borrar:
                confirmarBorrar(-1); //Borrar todas las tareas (id=-1)
                break;

            case R.id.options_resumen:
                mostrarResumen(); //Muestra el resumen con las tareas totales, finalizadas y pendientes
                break;

            case R.id.options_filtrar:
                filtrarTareas(); //Cambia la lista de tareas en funcion del filtro que seleccione
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

    //Gestionamos las opciones del menu contextual
    public boolean onContextItemSelected(MenuItem menuItem){
        //Recuperamos el cursor del adaptador y la posicion de la tarea seleccionada para trabajar sobre ella
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        Cursor cursor = this.adaptadorDB.getCursor();
        int pos = info.position;

        switch(menuItem.getItemId()) {

            case R.id.context_borrar:
                if (cursor.moveToPosition(pos)) {
                    int id = cursor.getInt(0);//0 porque el id es el primer argumento
                    confirmarBorrar(id); //Borrar la tarea con ese id
                } else {
                    Log.e("Context.eliminar", "Posicion incorrecta");
                    Toast.makeText(this, "Posicion incorrecta", Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.context_modificar:
                if (cursor.moveToPosition(pos)) {
                    //Recuperamos los argumentos para pasarlos al editor
                    int id = cursor.getInt(0); //0 porque el id es el primer argumento
                    String nombre = cursor.getString(1); //Segundo argumento
                    String descripcion = cursor.getString(2); //Tercer argumento
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

    // Método para pasar la fecha de formato String a formato Calendar
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

    // Método para pasar la fecha de formato Calendar a formato String
    private String formatDateToString(Calendar fecha) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(fecha.getTime());
    }

    //Comprueba que la fecha de cada tarea sea superior a la actual
    private void comprobarFechas(){
        Log.i("ComprobarFecha", "Comprobadno fechas");
        Calendar fechaActual = Calendar.getInstance(); //Guardamos la fecha actual

        //Recorremos los elementos de la lista
        for(int i=0; i<adaptadorDB.getCount(); i++){
            Cursor cursor = (Cursor) adaptadorDB.getItem(i); //Recuperamos cada tarea de la lista como un cursor

            //Guardamos la fecha e id de cada tarea
            String fechaString = cursor.getString(cursor.getColumnIndex(DBManager.COL_FECHA));
            Calendar fechaCaducidad = formatStringToDate(fechaString);
            int idTarea = cursor.getInt(cursor.getColumnIndex(DBManager.COL_ID));

            //Comprobamos si la fehca actual es superior o igual a la de la tarea
            if(fechaActual.after(fechaCaducidad)){ //La tarea ha caducado, la marcamos como finalizada
                gestorDB.marcarTareaComoCompletada(Integer.toString(idTarea));
                advertenciaCaducidad(cursor); //Mostramos advertencia de que ha caducado la tarea
            }
        }

        actualizar(filtroActual); //Actualizamos la lista
    }


    //----------------------------------------------------------------------------------------
    //  GESTION DE ALTERT DIALOGS
    //----------------------------------------------------------------------------------------


    //Pedirá confirmación al usuario antes de borrar un elemento, recibe el id de la tarea que hay que borrar, si el id de la tarea
    //es -1 (No hay ID negativos), se entiende que hay que borrar todas las tareas
    private void confirmarBorrar(int id){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(id < 0){ //Es una operacion para borrar todas las actividades
            builder.setTitle("Borrar todas las tareas");
            builder.setMessage("¿Estás seguro de que quieres borrar todas las tareas? Una vez eliminadas no se podrán recuperar");

            //Habilitamos el boton de confirmar y realizamos el borrado de todas las tareas
            builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    gestorDB.eliminarTodo();
                    actualizar(filtroActual);
                }
            });

        } else{ //Es una operacion para borrar una actividad en concreto

            builder.setTitle("Borrar la tarea seleccionada (ID: " + id + ")");
            builder.setMessage("¿Estás seguro de que quieres borrar la tarea seleccionada? Una vez eliminada no se podrá recuperar");

            //Habilitamos el boton de confirmar y realizamos el borrado de la tarea
            builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    gestorDB.eliminarTarea(id);
                    actualizar(filtroActual);
                }
            });
        }

        //Habilitamos el boton de cancelar
        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }


    //Muestra un AlertDialog para informar de que una tarea concreta ha caducado
    private void advertenciaCaducidad(Cursor cursor){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Recuperamos los datos relevantes del cursor, es decir, de la tarea caducada
        int idTarea = cursor.getInt(cursor.getColumnIndex(DBManager.COL_ID));
        String nombreTarea = cursor.getString(cursor.getColumnIndex(DBManager.COL_NOMBRE));

        //Personalizamos el AlertDialog
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

        //Obtener todas las tareas directamente desde la base de datos
        Cursor cursor = gestorDB.getTareas();

        //Si el cursor tiene elementos los recorremos
        if (cursor.moveToFirst()) {
            do {
                //Recuperamos la variable que nos dice si la tarea esta completada o no
                int completado = cursor.getInt(cursor.getColumnIndex(DBManager.COL_COMPLETADO));

                if (completado == 1) { // Tarea completada
                    tareasFinalizadas++;
                } else if (completado == 0) { // Tarea pendiente
                    tareasPendientes++;
                }
            } while (cursor.moveToNext());
        }

        cursor.close(); // Cerrar el cursor después de usarlo

        //Creamos el AlertDialog con Layout personalizado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Inflamos el layout personalizado
        View resumenView = this.getLayoutInflater().inflate(R.layout.resumen_activity, null);
        builder.setView(resumenView);

        //Confirguramos los elementos del AlertDialog
        builder.setTitle("Resumen");

        TextView tvTotal = resumenView.findViewById(R.id.tv_resumen_numTotal);
        TextView tvPendientes = resumenView.findViewById(R.id.tv_resumen_numPendientes);
        TextView tvFinalizadas = resumenView.findViewById(R.id.tv_resumen_numFinalizadas);

        //Mostramos los datos
        tvTotal.setText(Integer.toString(tareasFinalizadas + tareasPendientes));
        tvPendientes.setText(Integer.toString(tareasPendientes));
        tvFinalizadas.setText(Integer.toString(tareasFinalizadas));

        builder.setNegativeButton("Cerrar", null);
        builder.create().show();
    }


    //Muestra un AlertDialog con los diferentes filtros que podemos aplicar a la lista, este filtro se guarda en una variable global
    private void filtrarTareas(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Configuramos los elementos del AlertDialog con sus diferentes opciones
        builder.setTitle("Filtrar lista de tareas");
        String[] options = {"Todas", "Pendientes", "Finalizadas"};

        //Añadimos multiples opciones pero que solo se pueda seleccionar una
        builder.setSingleChoiceItems(options, filtroActual, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int opcion) {
                //En funcion de la opcion simplemente cambiamos la variable global que indica el filtro
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
                dialog.dismiss(); //Cerrar el diálogo al hacer clic en Aceptar
                actualizar(filtroActual); //Actualiza la lista en funcion del nuevo valor de filtrado
            }
        });

        builder.create().show();

    }


    //Devuelve el filtro, es necesario para el adaptador
    public int getFiltro(){
        return filtroActual;
    }


}
