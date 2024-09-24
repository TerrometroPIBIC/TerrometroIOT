package com.example.terrometro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class tabela extends AppCompatActivity {

    private TableLayout tabelaResistencia;
private Button voltar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabela);
voltar = findViewById(R.id.voltar);
        tabelaResistencia = findViewById(R.id.tabelaResistencia);
        voltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(tabela.this, MainActivity.class);

                startActivity(intent);
            }
        });

        // Recebe os dados passados pelo Intent
        ArrayList<Double> mediasResistencias = (ArrayList<Double>) getIntent().getSerializableExtra("mediasResistencias");

        if (mediasResistencias != null) {
            // Preenche a tabela com os dados recebidos
            for (int i = 0; i < mediasResistencias.size(); i++) {
                adicionarLinhaTabela(i + 1, mediasResistencias.get(i));
            }
        }
    }

    private void adicionarLinhaTabela(int tempo, double mediaResistencia) {
        TableRow row = new TableRow(this);

        TextView tvTempo = new TextView(this);
        tvTempo.setText(String.format("%d min", tempo));
        tvTempo.setPadding(8, 8, 8, 8);

        TextView tvMedia = new TextView(this);
        tvMedia.setText(String.format("%.3f MΩ", mediaResistencia/10));
        tvMedia.setPadding(8, 8, 8, 8);

        row.addView(tvTempo);
        row.addView(tvMedia);

        tabelaResistencia.addView(row);
    }



}