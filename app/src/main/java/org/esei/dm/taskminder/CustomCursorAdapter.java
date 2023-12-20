package org.esei.dm.taskminder;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Calendar;


/* Con el esta clase derivada de CursorAdapter gestionaremos los elementos de la lista de una forma más personalizada*/
public class CustomCursorAdapter extends CursorAdapter {

    //Constructor
    public CustomCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }



    //Infla la vista para un nuevo elemento de lista.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflar el diseño de cada elemento de la lista
        return LayoutInflater.from(context).inflate(R.layout.lv_item_activity, parent, false);
    }


    //Configura los datos en la vista para un elemento existente.
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Recuperamos los elementos de la lista para trabjar con ellos
        TextView nombreTextView = view.findViewById(R.id.tv_lista_nombre);
        TextView descripcionTextView = view.findViewById(R.id.tv_lista_descripcion);
        TextView fechaTextView = view.findViewById(R.id.tv_lista_fecha);
        Button finalizarButton = view.findViewById(R.id.bt_lista_finalizar);
        TextView idTextView = view.findViewById(R.id.tv_lista_id);
        LinearLayout item = view.findViewById(R.id.lv_item);

        //Recuperamos los datos de la tarea
        final int id = cursor.getInt(cursor.getColumnIndex(DBManager.COL_ID));
        final boolean completada = (cursor.getInt(cursor.getColumnIndex(DBManager.COL_COMPLETADO)) == 1);
        finalizarButton.setEnabled(!completada);

        // Configurar los valores en las vistas
        nombreTextView.setText(cursor.getString(cursor.getColumnIndex(DBManager.COL_NOMBRE)));
        descripcionTextView.setText(cursor.getString(cursor.getColumnIndex(DBManager.COL_DESCRIPCION)));
        fechaTextView.setText(cursor.getString(cursor.getColumnIndex(DBManager.COL_FECHA)));
        idTextView.setText(Integer.toString(id));

        //Si la tarea esta finalizada cambia su apariencia
        if (completada) {
            //Cambiar el color background de item por el color de tarea completada
            finalizarButton.setBackgroundColor(ContextCompat.getColor(context, R.color.boton_desactivado));
            item.setBackgroundColor(ContextCompat.getColor(context, R.color.tarea_finalizada));
        } else {
            // Restablecer el color de fondo original
            finalizarButton.setBackgroundColor(ContextCompat.getColor(context, R.color.boton_activado));
            item.setBackgroundColor(ContextCompat.getColor(context, R.color.tarea_pendiente));
        }

        // Configurar OnClickListener solo para el botón
        finalizarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int filtro = ((MainActivity) context).getFiltro(); //Recuperamos el filtro que se esta aplicando para poder actualizar

                Log.i("Boton", "BOTON FINALIZAR PULSADO - ID: " + id);

                //Marcamos la tarea como finalizada y actualizamos la lista
                ((MainActivity) context).gestorDB.marcarTareaComoCompletada(Integer.toString(id));
                ((MainActivity) context).actualizar(filtro);
            }
        });
    }
}
