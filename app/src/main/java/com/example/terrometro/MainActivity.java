package com.example.terrometro;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private TextView edtresistencia1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        edtresistencia1 = findViewById(R.id.edtresistencia);

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference medidasRef = database.getReference("medidas");
        medidasRef.setValue("Hello, World!");

        // Listener para buscar os valores de tensão e corrente
        medidasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Obtendo os valores de corrente e tensão do Firebase
                Double tensao = dataSnapshot.child("tensao").getValue(Double.class);
                Double corrente = dataSnapshot.child("corrente").getValue(Double.class);

                // Verifica se os valores não são nulos
                if (tensao != null && corrente != null) {
                    try {
                        // Calcula a resistência usando a fórmula da Lei de Ohm
                        double resistencia = calcularResistencia(tensao, corrente);
                        Log.d(TAG, "A resistência calculada é: " + resistencia + " ohms.");

                        // Atualiza o valor do EditText com a resistência calculada
                        edtresistencia1.setText(String.valueOf(resistencia));

                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Tensão ou corrente não encontradas no Firebase.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Lida com erros de leitura do Firebase
                Log.w(TAG, "Falha ao ler os valores.", error.toException());
            }
        });
    }

    // Função para calcular a resistência
    private double calcularResistencia(double tensao, double corrente) {
        if (corrente == 0) {
            throw new IllegalArgumentException("A corrente não pode ser zero.");
        }
        return tensao / corrente;
    }
}
