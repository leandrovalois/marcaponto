package com.example.marcaponto.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.marcaponto.network.ApiService;
import com.example.marcaponto.models.ConfiguracaoHorarios;
import com.example.marcaponto.models.ContagemPontos;
import com.example.marcaponto.utils.FirebaseManager;
import com.example.marcaponto.models.Ponto;
import com.example.marcaponto.R;
import com.example.marcaponto.services.VerificacaoPontoService;
import com.example.marcaponto.utils.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private FloatingActionButton fabRegistrarPonto;
    private ApiService apiService;
    private FirebaseAuth firebaseManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // Configurar o Retrofit
        apiService = RetrofitClient.getApiService();
        //Inicialização do firebase
        firebaseManager =  FirebaseManager.getInstance().getAuth();
        //
        buscarHorariosEIniciarService();
        //
        verificarPontosDoDia(firebaseManager.getCurrentUser().getEmail());
        //
        Intent serviceIntent = new Intent(this, VerificacaoPontoService.class);
        startService(serviceIntent);
        // Inicializar o provedor de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Configurar o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        //
        navigationView = findViewById(R.id.nav_view);
        //
        toolbar = findViewById(R.id.toolbar);
        // Configurar a Toolbar
        setSupportActionBar(toolbar);
        // Configurar o Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        //
        toggle.syncState();
        // Configurar o listener do menu
        navigationView.setNavigationItemSelectedListener(this);
        // Obter o SupportMapFragment e notificar quando o mapa estiver pronto
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Configurar o botão flutuante
        fabRegistrarPonto = findViewById(R.id.fab_registrar_ponto);
        fabRegistrarPonto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarPonto();
            }
        });
    }

    private void registrarPonto() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Obter a localização atual
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Obter coordenadas
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Obter horário atual
                    String horario = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());

                    // Obter o usuário logado (substitua pelo método correto)
                    String usuario = firebaseManager.getCurrentUser().getEmail();

                    // Criar objeto Ponto
                    Ponto ponto = new Ponto(usuario, latitude, longitude, horario);

                    // Enviar para o servidor
                    apiService.salvarPonto(ponto).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(HomeActivity.this, "Ponto registrado com sucesso!", Toast.LENGTH_SHORT).show();

                                // Verificar pontos do dia após salvar
                                verificarPontosDoDia(usuario);
                            } else {
                                Toast.makeText(HomeActivity.this, "Erro ao registrar ponto", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(HomeActivity.this, "Falha na comunicação com o servidor", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(HomeActivity.this, "Localização não disponível", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verificar permissões de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Ativar a camada de localização do mapa
        mMap.setMyLocationEnabled(true);

        // Obter a localização atual e centralizar o mapa
        getLastLocation();
    }
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Centralizar o mapa na localização atual
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLocation).title("Você está aqui"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15)); // Zoom 15
                } else {
                    Toast.makeText(HomeActivity.this, "Localização não disponível", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_relatorio) {
            Intent intent = new Intent(this, EspelhoDePontoActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_home) {
            // Navegar para a Home
        } else if (id == R.id.nav_profile) {
            // Navegar para o Perfil
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
            // Navegar para Configurações
        } else if (id == R.id.nav_logout) {
            // Fazer logout
        }

        // Fechar o menu após a seleção
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    private void verificarPontosDoDia(String usuario) {
        Call<ContagemPontos> call = apiService.contarPontosDoDia(usuario);
        call.enqueue(new Callback<ContagemPontos>() {
            @Override
            public void onResponse(Call<ContagemPontos> call, Response<ContagemPontos> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalPontos = response.body().getTotal();
                    //Log.d("CONTAGEM" , String.valueOf(totalPontos));
                    if (totalPontos >= 400) {
                        // Desativar o botão de marcar ponto
                        fabRegistrarPonto.setAlpha(0.5f); // Torna o botão translúcido
                        fabRegistrarPonto.setEnabled(false); // Desativa o botão

                        // Feedback visual
                        Toast.makeText(HomeActivity.this, "Você já registrou todos os pontos de hoje !", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Erro ao verificar pontos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ContagemPontos> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Falha na comunicação com o servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void buscarHorariosEIniciarService() {
        String usuario = firebaseManager.getCurrentUser().getEmail(); // Substitua pelo método correto para obter o usuário logado

        Call<ConfiguracaoHorarios> call = apiService.buscarHorariosConfigurados(usuario);
        call.enqueue(new Callback<ConfiguracaoHorarios>() {
            @Override
            public void onResponse(Call<ConfiguracaoHorarios> call, Response<ConfiguracaoHorarios> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ConfiguracaoHorarios configuracao = response.body();

                    // Converter os horários para minutos desde a meia-noite
                    int[] horariosProgramados = {
                            converterHorarioParaMinutos(configuracao.getEntrada1()),
                            converterHorarioParaMinutos(configuracao.getSaida1()),
                            converterHorarioParaMinutos(configuracao.getEntrada2()),
                            converterHorarioParaMinutos(configuracao.getSaida2())
                    };

                    // Iniciar o Service com os horários programados
                    Intent serviceIntent = new Intent(HomeActivity.this, VerificacaoPontoService.class);
                    serviceIntent.putExtra("horariosProgramados", horariosProgramados);
                    startService(serviceIntent);
                } else {
                    Toast.makeText(HomeActivity.this, "Erro ao buscar horários configurados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ConfiguracaoHorarios> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Falha na comunicação com o servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private int converterHorarioParaMinutos(String horario) {
        String[] partes = horario.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);
        return horas * 60 + minutos;
    }
}