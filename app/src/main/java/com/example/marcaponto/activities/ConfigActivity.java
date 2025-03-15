package com.example.marcaponto.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.marcaponto.network.ApiService;
import com.example.marcaponto.models.ConfiguracaoHorarios;
import com.example.marcaponto.utils.FirebaseManager;
import com.example.marcaponto.R;
import com.example.marcaponto.utils.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConfigActivity extends AppCompatActivity {

    private Button buttonEntrada1, buttonSaida1, buttonEntrada2, buttonSaida2;
    private Button buttonSalvarConfig;
    private int entrada1Hora = -1, entrada1Minuto = -1;
    private int saida1Hora = -1, saida1Minuto = -1;
    private int entrada2Hora = -1, entrada2Minuto = -1;
    private int saida2Hora = -1, saida2Minuto = -1;

    private FirebaseAuth firebase;
    private ApiService apiService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        buttonEntrada1 = findViewById(R.id.buttonEntrada1);
        buttonSaida1 = findViewById(R.id.buttonSaida1);
        buttonEntrada2 = findViewById(R.id.buttonEntrada2);
        buttonSaida2 = findViewById(R.id.buttonSaida2);
        buttonSalvarConfig = findViewById(R.id.buttonSalvarConfig);

        firebase = FirebaseManager.getInstance().getAuth();

        // Inicializar o Retrofit
        apiService = RetrofitClient.getApiService();


        buttonEntrada1.setOnClickListener(v -> abrirTimePicker("Entrada 1", new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hora, int minuto) {
                entrada1Hora = hora;
                entrada1Minuto = minuto;
                buttonEntrada1.setText(String.format("%02d:%02d", hora, minuto));
            }
        }));

        buttonSaida1.setOnClickListener(v -> abrirTimePicker("Saída 1", new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hora, int minuto) {
                saida1Hora = hora;
                saida1Minuto = minuto;
                buttonSaida1.setText(String.format("%02d:%02d", hora, minuto));
            }
        }));

        buttonEntrada2.setOnClickListener(v -> abrirTimePicker("Entrada 2", new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hora, int minuto) {
                entrada2Hora = hora;
                entrada2Minuto = minuto;
                buttonEntrada2.setText(String.format("%02d:%02d", hora, minuto));
            }
        }));

        buttonSaida2.setOnClickListener(v -> abrirTimePicker("Saída 2", new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hora, int minuto) {
                saida2Hora = hora;
                saida2Minuto = minuto;
                buttonSaida2.setText(String.format("%02d:%02d", hora, minuto));
            }
        }));

        buttonSalvarConfig.setOnClickListener(v -> salvarConfiguracao());
    }

    private void abrirTimePicker(String titulo, TimePickerDialog.OnTimeSetListener listener) {
        Calendar calendar = Calendar.getInstance();
        int horaAtual = calendar.get(Calendar.HOUR_OF_DAY);
        int minutoAtual = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                listener,
                horaAtual,
                minutoAtual,
                true
        );
        timePickerDialog.setTitle(titulo);
        timePickerDialog.show();
    }

    private void salvarConfiguracao() {
        if (entrada1Hora == -1 || saida1Hora == -1 || entrada2Hora == -1 || saida2Hora == -1) {
            Toast.makeText(this, "Preencha todos os horários!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Formatar os horários como strings
        String entrada1 = String.format("%02d:%02d", entrada1Hora, entrada1Minuto);
        String saida1 = String.format("%02d:%02d", saida1Hora, saida1Minuto);
        String entrada2 = String.format("%02d:%02d", entrada2Hora, entrada2Minuto);
        String saida2 = String.format("%02d:%02d", saida2Hora, saida2Minuto);

        // Obter o usuário logado (substitua pelo método correto)
        String usuario = firebase.getCurrentUser().getEmail();

        // Criar objeto ConfiguracaoHorarios
        ConfiguracaoHorarios configuracao = new ConfiguracaoHorarios(usuario, entrada1, saida1, entrada2, saida2);

        // Enviar para o servidor
        Call<Void> call = apiService.salvarConfiguracaoHorarios(configuracao);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ConfigActivity.this, "Configuração salva com sucesso!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConfigActivity.this, "Erro ao salvar configuração", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ConfigActivity.this, "Falha na comunicação com o servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
}