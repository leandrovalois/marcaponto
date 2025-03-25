package com.example.marcaponto.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class VerificacaoPontoService extends Service {

    private static final String TAG = "VerificacaoPontoService";
    private Timer timer;
    private static final long INTERVALO_VERIFICACAO = 60 * 1000; // 1 minuto
    private int[] horariosProgramados; // Horários programados pelo usuário (em minutos desde a meia-noite)

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Obter os horários programados do usuário (em minutos desde a meia-noite)
        horariosProgramados = intent.getIntArrayExtra("horariosProgramados");

        if (horariosProgramados == null || horariosProgramados.length != 4) {
            Log.e(TAG, "Horários programados inválidos.");

           //stopSelf(); // Encerra o serviço se os horários não forem válidos
            return START_NOT_STICKY;
        }else{
            Log.d(TAG,"Horarios programados: "+ horariosProgramados);
        }

        iniciarVerificacao();
        return START_STICKY;
    }

    private void iniciarVerificacao() {
        Log.d(TAG, "iniciarVerificacao");
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

        // Encontrar o horário programado mais próximo
        int horarioMaisProximo = encontrarHorarioMaisProximo(minutosAtuais, horariosProgramados);

        if (horarioMaisProximo != -1) {
            int diferenca = minutosAtuais - horarioMaisProximo;

            // Verificar se a diferença está entre 1 e 3 minutos
            if (diferenca >= 1 && diferenca <= 3) {
                Log.d(TAG, "Disparar notificação: " + diferenca + " minutos após o horário programado.");
                enviarNotificacao("Lembrete de Ponto", "Você esqueceu de bater o ponto às " + formatarHorario(horarioMaisProximo) + ".");
            }
        }
    }

    private int encontrarHorarioMaisProximo(int minutosAtuais, int[] horariosProgramados) {
        if (horariosProgramados == null || horariosProgramados.length == 0) {
            return -1; // Retorna -1 se não houver horários programados
        }

        int horarioMaisProximo = horariosProgramados[0];
        int menorDiferenca = Math.abs(minutosAtuais - horarioMaisProximo);

        for (int horario : horariosProgramados) {
            int diferenca = Math.abs(minutosAtuais - horario);
            if (diferenca < menorDiferenca) {
                menorDiferenca = diferenca;
                horarioMaisProximo = horario;
            }
        }

        return horarioMaisProximo;
    }

    private void enviarNotificacao(String titulo, String mensagem) {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        serviceIntent.putExtra("titulo", titulo);
        serviceIntent.putExtra("mensagem", mensagem);
        startService(serviceIntent);
    }

    private String formatarHorario(int horario) {
        int horas = horario / 60;
        int minutos = horario % 60;
        return String.format("%02d:%02d", horas, minutos);
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