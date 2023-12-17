package org.esei.dm.taskminder;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity {

    private Calendar calendar;
    private TextView tv_operacion, tv_fecha;
    private EditText et_nombre;
    private TextInputEditText et_descripcion;
    private Button bt_guardar, bt_cancelar, bt_fecha;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.editor_activity);

        //Identificamos los elementos del Layout
        tv_operacion = findViewById(R.id.tv_operacion);
        tv_fecha = findViewById(R.id.tv_fecha);
        et_nombre = findViewById(R.id.et_nombre);
        et_descripcion = findViewById(R.id.et_descripcion);
        bt_guardar = findViewById(R.id.bt_guardar);
        bt_cancelar = findViewById(R.id.bt_cancelar);
        bt_fecha = findViewById(R.id.bt_fecha);

        //Recuperamos los datos del intent
        Intent datosEnviados = this.getIntent();
        int id = datosEnviados.getExtras().getInt("id"); //Id si es -1 es una insercion nueva
        String nombre = datosEnviados.getExtras().getString("nombre");
        String descripcion = datosEnviados.getExtras().getString("descripcion");
        String fechaString = datosEnviados.getExtras().getString("fechaString");
        boolean completado = datosEnviados.getBooleanExtra("completado", false);

        //Mostramos los datos actuales por la pantalla
        if(id < 0){ //Operacion de creacion
            tv_operacion.setText("Nueva Tarea");
        } else{ //Operacion de modificacion
            tv_operacion.setText("Editar tarea: " + nombre);
        }
        et_nombre.setText(nombre);
        et_descripcion.setText(descripcion);
        tv_fecha.setText(fechaString);
        Log.i("Completado?", "TAREA COMPLETADA?????????: " + completado + "--------------------------------------------------");

        //Habilitamos los botones

        //Configuramos el click del boton para seleccionar una fecha
        bt_fecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Fecha", "--------------------------------------------------- " + fechaString);
                seleccionarFecha(formatStringToDate(fechaString));
            }
        });

        //Configuramos el click del boton para cancelar la operacion
        bt_cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditorActivity.this.setResult(Activity.RESULT_CANCELED);
                EditorActivity.this.finish();
            }
        });


        //Detectamos cuando cambian los inputs, si cambian y son validos se habilita el boton de guardar
        bt_guardar.setEnabled(false);
        et_nombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) { //Cuando se cambie el nombre se habilitara el boton guardar solo si los campos son validos
                bt_guardar.setEnabled(comprobarInputs(et_nombre.getText().toString(), et_descripcion.getText().toString()));
            }
        });

        et_descripcion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) { //Cuando se cambie la descripcion se habilitara el boton guardar solo si los campos son validos
                bt_guardar.setEnabled(comprobarInputs(et_nombre.getText().toString(), et_descripcion.getText().toString()));
            }
        });



        //Configuramos el click del boton para guardar los cambios
        bt_guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nombre = et_nombre.getText().toString();
                String descripcion = et_descripcion.getText().toString();
                String fechaString = tv_fecha.getText().toString();

                Intent intent = new Intent();
                intent.putExtra("id", id);
                intent.putExtra("nombre", nombre);
                intent.putExtra("descripcion", descripcion);
                intent.putExtra("fechaString", fechaString);
                intent.putExtra("completado", false);

                EditorActivity.this.setResult(Activity.RESULT_OK, intent);
                EditorActivity.this.finish();
            }
        });


    }

    //Desde aqui comprobaremos que el nombre y la descripcion no sean vacios
    private boolean comprobarInputs(String nombre, String descripcion){
        if((nombre.trim().length()) > 0 && (descripcion.trim().length() > 0)){
            return true;
        }

        return false;
    }



    //----------------------------------------------------------------------------------------
    //  GESTION DE FECHAS
    //----------------------------------------------------------------------------------------

    //Metodo para abrir un DatePickerDialog y seleccionar una fecha
    private void seleccionarFecha(Calendar fecha){
        tv_fecha = (TextView) this.findViewById(R.id.tv_fecha);

        //Fecha actual de la tarea
        int currentYear = fecha.get(Calendar.YEAR);
        int currentMonth = fecha.get(Calendar.MONTH);
        int currentDay = fecha.get(Calendar.DAY_OF_MONTH);

        //Creamos un DatePickerDialog para seleccionar la fecha
        DatePickerDialog elegirFecha = new DatePickerDialog(EditorActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                //Guardamos en el objeto calendar la fecha escogida
                fecha.set(year, month, dayOfMonth);

                tv_fecha.setText(formatDateToString(fecha));
                bt_guardar.setEnabled(comprobarInputs(et_nombre.getText().toString(), et_descripcion.getText().toString()));

            }
        }, currentYear, currentMonth, currentDay);

        elegirFecha.show();
    }


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

}
