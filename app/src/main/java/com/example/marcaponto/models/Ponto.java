package com.example.marcaponto.models;

public class Ponto {
    private String usuario;
    private double latitude;
    private double longitude;
    private String horario;

    public Ponto(String usuario, double latitude, double longitude, String horario) {
        this.usuario = usuario;
        this.latitude = latitude;
        this.longitude = longitude;
        this.horario = horario;
    }

    // Getters e Setters
    public String getUsuario() {
        return usuario;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public String getHorario() {
        return horario;
    }
}