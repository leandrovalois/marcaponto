package com.example.marcaponto.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.marcaponto.R;
import com.example.marcaponto.models.Ponto;
import com.example.marcaponto.network.ApiService;
import com.example.marcaponto.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VerificacaoPontoService extends Service {

    private Timer timer;
    private static final long INTERVALO_VERIFICACAO = 1 * 60 * 1000; // 5 minutos
    private static final int MAX_TENTATIVAS = 3; // Número máximo de tentativas
    private int[] horariosProgramados; // Horários programados pelo usuário (em minutos desde a meia-noite)
    private int tentativas = 0; // Contador de tentativas
    private ApiService apiService; // Interface do Retrofit para consultar o banco de dados

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Obter os horários programados do usuário (em minutos desde a meia-noite)
        horariosProgramados = intent.getIntArrayExtra("horariosProgramados");

        if (horariosProgramados == null || horariosProgramados.length != 4) {
            Log.e("VerificacaoPontoService", "Horários programados inválidos.");
            stopSelf(); // Encerra o serviço se os horários não forem válidos
            return START_NOT_STICKY;
        }

        // Configurar o Retrofit
        apiService = RetrofitClient.getApiService();

        iniciarVerificacao();
        return START_STICKY;
    }

    private void iniciarVerificacao() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                verificarPontos();
            }
        }, 0, INTERVALO_VERIFICACAO);
    }

    private void verificarPontos() {
        Calendar agora = Calendar.getInstance();
        int minutosAtuais = agora.get(Calendar.HOUR_OF_DAY) * 60 + agora.get(Calendar.MINUTE);

        for (int horario : horariosProgramados) {
            if (minutosAtuais >= horario && minutosAtuais <= horario + 5) {
                // Verificar se o ponto foi batido
                verificarPontoNoBanco(horario);
            }
        }
    }

    private void verificarPontoNoBanco(int horario) {
        String horarioFormatado = formatarHorario(horario);
        Call<List<Ponto>> call = apiService.buscarPontosPorHorario(horarioFormatado);

        call.enqueue(new Callback<List<Ponto>>() {
            @Override
            public void onResponse(Call<List<Ponto>> call, Response<List<Ponto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ponto> pontos = response.body();
                    if (pontos.isEmpty()) {
                        // Ponto não foi batido
                        tentativas++;
                        if (tentativas >= MAX_TENTATIVAS) {
                            exibirNotificacao(horario);
                            tentativas = 0; // Reinicia o contador de tentativas
                        }
                    } else {
                        // Ponto foi batido
                        tentativas = 0; // Reinicia o contador de tentativas
                    }
                } else {
                    Log.e("VerificacaoPontoService", "Erro ao buscar pontos no banco de dados.");
                }
            }

            @Override
            public void onFailure(Call<List<Ponto>> call, Throwable t) {
                Log.e("VerificacaoPontoService", "Falha na comunicação com o servidor: " + t.getMessage());
            }
        });
    }

    private void exibirNotificacao(int horario) {
        // Configurar o NotificationManager
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "alertas_ponto";

        // Criar o canal de notificação (necessário para Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Alertas de Ponto",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Criar a notificação
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Ícone da notificação
                .setContentTitle("Alerta de Ponto") // Título da notificação
                .setContentText("Você ainda não bateu o ponto no horário " + formatarHorario(horario) + "!") // Mensagem da notificação
                .setPriority(NotificationCompat.PRIORITY_HIGH); // Prioridade alta

        // Exibir a notificação
        notificationManager.notify(horario, builder.build()); // Usar o horário como ID da notificação
    }

    private String formatarHorario(int minutos) {
        int horas = minutos / 60;
        int minutosRestantes = minutos % 60;
        return String.format("%02d:%02d", horas, minutosRestantes);
    }

    @Override
    public void onDestroy() {
        pararVerificacao();
        super.onDestroy();
    }

    private void pararVerificacao() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}