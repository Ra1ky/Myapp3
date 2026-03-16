package com.example.sportify;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// KOMPONENTŲ PAVYZDŽIŲ EKRANAS
// Iš čia galima kopijuoti XML fragmentus (mygtukus, teksto stilius, progreso juostas, korteles...) į kitus ekranus

// KAIP NAUDOTI:
// 1. Atidarykite res/layout/activity_component_showcase.xml
// 2. Raskite reikiamą komponentą
// 3. Nukopijuokite XML bloką į savo layout'ą
// 4. Pasižiūrėkite komentare nurodytą style="@style/..." pavadinimą
public class ComponentShowcaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_component_showcase);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.showcase), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}