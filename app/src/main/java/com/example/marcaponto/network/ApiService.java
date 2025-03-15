package com.example.marcaponto.network;

import com.example.marcaponto.models.ConfiguracaoHorarios;
import com.example.marcaponto.models.ContagemPontos;
import com.example.marcaponto.models.Ponto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/salvar-ponto")
    Call<Void> salvarPonto(@Body Ponto ponto);


    @GET("/pontos-do-dia/{usuario}")
    Call<List<Ponto>> buscarPontosDoDia(@Path("usuario") String usuario);

    @GET("/contar-pontos-do-dia/{usuario}")
    Call<ContagemPontos> contarPontosDoDia(@Path("usuario") String usuario);

    @POST("/salvar-configuracao")
    Call<Void> salvarConfiguracaoHorarios(@Body ConfiguracaoHorarios configuracao);

    @GET("/buscar-pontos-por-horario")
    Call<List<Ponto>> buscarPontosPorHorario(@Query("horario") String horario);

    @GET("/buscar-horarios-configurados")
    Call<ConfiguracaoHorarios> buscarHorariosConfigurados(@Query("usuario") String usuario);

}