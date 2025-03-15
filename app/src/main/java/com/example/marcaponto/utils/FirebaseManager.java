package com.example.marcaponto.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseManager {

    private static FirebaseManager instance;
    private FirebaseAuth mAuth;

    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public FirebaseUser getUser() {
        return mAuth.getCurrentUser(); // Retorna o usuário atual (pode ser null)
    }
}