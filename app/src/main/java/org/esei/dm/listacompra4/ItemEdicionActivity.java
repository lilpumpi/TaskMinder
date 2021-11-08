package org.esei.dm.listacompra4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ItemEdicionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_item_edition );

        final ImageButton btGuardar = this.findViewById( R.id.btGuardar );
        final ImageButton btCancelar = this.findViewById( R.id.btCancelar );
        final EditText edCantidad = this.findViewById( R.id.edCantidad );
        final EditText edNombre = this.findViewById( R.id.edNombre );

        final Intent datosEnviados = this.getIntent();
        final String nombre = datosEnviados.getExtras().getString( "nombre", "ERROR" );
        final int cantidad = datosEnviados.getExtras().getInt( "num", -1 );

        edNombre.setText( nombre );
        edCantidad.setText( String.format(Locale.getDefault(), "%d", cantidad ) );

        btCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemEdicionActivity.this.setResult( Activity.RESULT_CANCELED );
                ItemEdicionActivity.this.finish();
            }
        });

        btGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String nombre = edNombre.getText().toString();
                final int cantidad = Integer.parseInt( edCantidad.getText().toString() );
                final Intent retData = new Intent();

                retData.putExtra( "nombre", nombre );
                retData.putExtra( "num", cantidad );

                ItemEdicionActivity.this.setResult( Activity.RESULT_OK, retData );
                ItemEdicionActivity.this.finish();
            }
        });
        btGuardar.setEnabled( false );

        edNombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                btGuardar.setEnabled( edNombre.getText().toString().trim().length() > 0 );
            }
        });

        edCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int cantidad = 0;

                try {
                    cantidad = Integer.parseInt( edCantidad.getText().toString() );
                } catch(NumberFormatException exc) {
                    Log.w( "ItemEdicionActivity", "edCantidad no puede ser convertido a número" );
                }

                btGuardar.setEnabled( cantidad > 0 );
            }
        });
    }
}
