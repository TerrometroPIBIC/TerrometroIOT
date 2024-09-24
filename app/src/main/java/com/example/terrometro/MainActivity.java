package com.example.terrometro;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Handler handler = new Handler();
    private final int INTERVALO_TEMPO = 5000; // 5 segundos
    private Button botao;
    private TextView textViewResistencia, correnteText, tensaoText;
    private LineChart lineChart;
    private ArrayList<Entry> valoresResistencia = new ArrayList<>();
    private ArrayList<Double> resistenciasCalculadas = new ArrayList<>();
    private int tempoDecorrido = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        lineChart = findViewById(R.id.chart);
        botao = findViewById(R.id.botao);

        // Configura o evento de clique no botão
        botao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calcula as médias de resistência
                ArrayList<Double> mediasResistencias = calcularMediasResistencia();

                // Abre a Activity da tabela de resistências
                abrirTabelaResistencia(mediasResistencias);
            }
        });

        // Configura o gráfico de linha
        configurarGrafico();

        // Inicializa o TextView do layout
        textViewResistencia = findViewById(R.id.textViewResistencia);
        correnteText = findViewById(R.id.correnteText);
        tensaoText = findViewById(R.id.tensãoText);

        // Inicia o timer para ler os dados e calcular a resistência
        iniciarLeituraPeriodica();
    }
    private void abrirTabelaResistencia(ArrayList<Double> mediasResistencias) {
        Intent intent = new Intent(MainActivity.this, tabela.class);
        intent.putExtra("mediasResistencias", mediasResistencias);
        startActivity(intent);
    }

    // Função para calcular médias de resistência com base nos valores armazenados
    private ArrayList<Double> calcularMediasResistencia() {
        ArrayList<Double> mediasResistencias = new ArrayList<>();

        int totalIntervalos = 30; // Supondo que você queira calcular médias de 30 intervalos de tempo
        int intervaloPorMinuto = resistenciasCalculadas.size() / totalIntervalos;

        for (int i = 0; i < totalIntervalos; i++) {
            double soma = 0;
            int contador = 0;

            for (int j = i * intervaloPorMinuto; j < (i + 1) * intervaloPorMinuto && j < resistenciasCalculadas.size(); j++) {
                soma += resistenciasCalculadas.get(j);
                contador++;
            }

            if (contador > 0) {
                mediasResistencias.add(soma / contador); // Calcula a média para o intervalo
            }
        }

        return mediasResistencias;
    }

    private double gerarValorComVariacao(double valorAtual, double percentualVariacao) {
        double variacaoMaxima = valorAtual * percentualVariacao;
        double variacao = (Math.random() * 2 * variacaoMaxima) - variacaoMaxima; // Gera um valor entre -5% e +5%
        return valorAtual + variacao;
    }
    // Função que inicia a leitura periódica de corrente e tensão do Firebase
    private void iniciarLeituraPeriodica() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Executa a leitura do Firebase e o cálculo da resistência
                lerDadosECalcularResistencia();

                // Reagenda a execução após o intervalo definido
                handler.postDelayed(this, INTERVALO_TEMPO);
            }
        }, INTERVALO_TEMPO);
    }

    // Função para ler dados do Firebase e calcular a resistência
    private void lerDadosECalcularResistencia() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference medidasRef = database.getReference("Medidas");

        medidasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Double tensao = dataSnapshot.child("Tensao").getValue(Double.class);
                Double corrente = dataSnapshot.child("Corrente").getValue(Double.class);
                Double tensaoAtual = dataSnapshot.child("Tensao").getValue(Double.class);
                Double correnteAtual = dataSnapshot.child("Corrente").getValue(Double.class);
                if (tensao != null && corrente != null) {
                    try {
                        double resistencia = calcularResistencia(tensao, corrente);
                        Log.d(TAG, "A resistência calculada é: " + resistencia + " ohms.");
                        // Gerar valores de tensão e corrente com variação de 5%
                        double novaTensao = gerarValorComVariacao(tensaoAtual, 0.005);
                        double novaCorrente = gerarValorComVariacao(correnteAtual, 0.005);

                        // Atualizar o Firebase com os novos valores
                        medidasRef.child("Tensao").setValue(novaTensao);
                        medidasRef.child("Corrente").setValue(novaCorrente);

                        // Atualiza o TextView com o valor da resistência
                        atualizarTextViewResistencia(resistencia,corrente, tensao);
                        // Adiciona o valor da resistência ao gráfico
                        atualizarGraficoResistencia(resistencia);

                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Tensão ou corrente não encontradas no Firebase.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
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

    // Função para atualizar o valor do TextView
    private void atualizarTextViewResistencia(double resistencia, double corrente, double tensao) {
        // Atualiza o TextView no thread principal
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double textoresistencia;
                textoresistencia = resistencia / 1000000000;
                textViewResistencia.setText(String.format("Resistência: %.3f MΩ", textoresistencia));
                tensaoText.setText(String.format("Tensão %.3f Vrms", tensao));
                double textocorrente;
                textocorrente = corrente * 1000000;
                correnteText.setText(String.format("Corrente: %.5f mA", textocorrente));
            }
        });
    }
    // Função para configurar o gráfico de linha
    private void configurarGrafico() {
        lineChart.getDescription().setEnabled(false); // Desativa a descrição do gráfico
        lineChart.setTouchEnabled(true); // Habilita toque no gráfico
        lineChart.setDragEnabled(true); // Habilita arrastar
        lineChart.setScaleEnabled(true); // Habilita zoom

        // Configura os eixos do gráfico
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // Valor mínimo no eixo Y

        // Formata o eixo Y para mostrar os valores em milhões
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.3fM", value / 1000000000); // Divide por 1.000.000 para exibir em milhões
            }
        });

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false); // Desativa o eixo Y à direita

        lineChart.setVisibleXRangeMaximum(10); // Por exemplo, mostra os últimos 10 segundos
        lineChart.moveViewToX(valoresResistencia.size() - 1); // Move a visualização para o último ponto
    }
    private void atualizarGraficoResistencia(double resistencia) {
        // Adiciona o novo valor ao gráfico, usando o tempo como eixo X
        valoresResistencia.add(new Entry(tempoDecorrido, (float) resistencia));
        tempoDecorrido += INTERVALO_TEMPO / 1000; // Incrementa o tempo em segundos

        // Cria o conjunto de dados para o gráfico
        LineDataSet dataSet = new LineDataSet(valoresResistencia, "Resistência (ohms)");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS); // Cores para o gráfico
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);

        // Atualiza o gráfico com os novos dados
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // Atualiza o gráfico
    }
}
