package com.example.marcaponto.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.marcaponto.utils.FirebaseManager;
import com.example.marcaponto.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText editTextEmail, editTextPassword;
    private Button btn_entrar;
    private TextView textViewUserEmail;
    private TextView textView_register;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Iniciar o processo de solicitação de permissões
        requestPermissionsSequentially();

        // Inicializar FirebaseAuth
        mAuth = FirebaseManager.getInstance().getAuth();

        // Configurar login com Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Referenciar os elementos do layout
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btn_entrar = findViewById(R.id.buttonAuth);
        SignInButton buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        textView_register = findViewById(R.id.txt_register);

        // Configurar cliques nos botões
        buttonGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        btn_entrar.setOnClickListener(v -> checkUserAndAuthenticate());
        textView_register.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        // Verifica se há um usuário logado e exibe o e-mail
        updateUI(mAuth.getCurrentUser());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void requestPermissionsSequentially() {
        // Solicitar permissão de notificação primeiro
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            } else {
                // Se a permissão de notificação já foi concedida, solicitar permissão de localização
                requestLocationPermission();
            }
        } else {
            // Para versões anteriores ao Android 13, não é necessário solicitar permissão de notificação
            requestLocationPermission();
        }
    }
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissão de localização já concedida
            Toast.makeText(this, "Permissão de localização concedida", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão de notificação concedida, agora solicitar permissão de localização
                requestLocationPermission();
            } else {
                // Permissão de notificação negada
                Toast.makeText(this, "Permissão de notificação negada", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão de localização concedida
                Toast.makeText(this, "Permissão de localização concedida", Toast.LENGTH_SHORT).show();
            } else {
                // Permissão de localização negada
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void checkUserAndAuthenticate() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tenta logar
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                       goToHome();
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        // Tratamento de erros
                        if (task.getException() instanceof FirebaseAuthException) {
                            String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                            if (errorCode.equals("ERROR_USER_NOT_FOUND")) {
                                Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Erro ao autenticar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Erro desconhecido", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("GoogleLogin", "Falha no login", e);
                Toast.makeText(this, "Falha ao logar com Google", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login com Google bem-sucedido!", Toast.LENGTH_SHORT).show();
                        goToHome();
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Erro ao logar com Google", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            //textViewUserEmail.setText(user.getEmail());
        } else {
            //textViewUserEmail.setText("Nenhum usuário logado");
        }
    }
    private void goToHome(){
        Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(MainActivity.this,HomeActivity.class);
        startActivity(i);
    }
}