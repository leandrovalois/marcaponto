package com.example.marcaponto.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.marcaponto.network.ApiService;
import com.example.marcaponto.models.Ponto;
import com.example.marcaponto.R;
import com.example.marcaponto.utils.FirebaseManager;
import com.example.marcaponto.utils.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EspelhoDePontoActivity extends AppCompatActivity {

    private ListView listViewPontos;
    private ApiService apiService;
    private FirebaseAuth firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_espelho_de_ponto);

        firebaseManager =  FirebaseManager.getInstance().getAuth();

        listViewPontos = findViewById(R.id.listViewPontos);

        // Configurar o Retrofit
        apiService = RetrofitClient.getApiService();

        // Buscar pontos do dia
        buscarPontosDoDia(firebaseManager.getCurrentUser().getEmail());
    }

    private void buscarPontosDoDia(String usuario) {
        Call<List<Ponto>> call = apiService.buscarPontosDoDia(usuario);
        call.enqueue(new Callback<List<Ponto>>() {
            @Override
            public void onResponse(Call<List<Ponto>> call, Response<List<Ponto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ponto> pontos = response.body();
                    exibirPontos(pontos);
                    Log.d(">>>>>",response.body().toString());
                } else {
                    Toast.makeText(EspelhoDePontoActivity.this, "Erro ao buscar pontos", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Ponto>> call, Throwable t) {
                Toast.makeText(EspelhoDePontoActivity.this, "Falha na comunicação com o servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exibirPontos(List<Ponto> pontos) {
        List<String> pontosFormatados = new ArrayList<>();

        for (Ponto ponto : pontos) {
            // Formatar o horário
            String horarioFormatado = formatarHorario(ponto.getHorario());

            String pontoFormatado = "\n"+
                                    "Usuário: " + ponto.getUsuario() + "\n" +
                                    "Local: " + "(" + ponto.getLatitude() + ", " + ponto.getLongitude() + ") " + "\n" +
                                    "Horário: " + horarioFormatado +
                                    "\n";
            pontosFormatados.add(pontoFormatado);

        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                pontosFormatados
        );
        listViewPontos.setAdapter(adapter);
    }

    private String formatarHorario(String horario) {
        try {
            // Converter o horário recebido (UTC) para o fuso horário local
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Define o fuso horário de entrada como UTC

            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            outputFormat.setTimeZone(TimeZone.getDefault()); // Define o fuso horário de saída como o local do dispositivo

            Date date = inputFormat.parse(horario);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return horario; // Retorna o horário original em caso de erro
        }
    }
}