package com.example.terrometro;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
    private TextView textViewResistencia, correnteText, tensaoText; // TextView para mostrar a resistência
    private LineChart lineChart; // Gráfico de linha para exibir a resistência
    private ArrayList<Entry> valoresResistencia = new ArrayList<>(); // Lista de valores para o gráfico
    private int tempoDecorrido = 0; // Para o eixo X (tempo)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        lineChart = findViewById(R.id.chart);

        // Configura o gráfico de linha
        configurarGrafico();
        // Inicializa o TextView do layout
        textViewResistencia = findViewById(R.id.textViewResistencia);
        correnteText = findViewById(R.id.correnteText);
        tensaoText = findViewById(R.id.tensãoText);
        // Inicia o timer para ler os dados e calcular a resistência
        iniciarLeituraPeriodica();

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

                if (tensao != null && corrente != null) {
                    try {
                        double resistencia = calcularResistencia(tensao, corrente);
                        Log.d(TAG, "A resistência calculada é: " + resistencia + " ohms.");

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
                textViewResistencia.setText("Resistência: " + resistencia + " ohms");
                tensaoText.setText("Tensão:"+tensao);
                correnteText.setText("Corrente:"+corrente);
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
