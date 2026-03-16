package com.example.sportify;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Temporary detail screen launched when a dashboard card is tapped.
public class CardDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "detail_title";
    public static final String EXTRA_DESC  = "detail_desc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_card_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String desc  = getIntent().getStringExtra(EXTRA_DESC);

        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDesc  = findViewById(R.id.tvDetailDesc);

        if (title != null) tvTitle.setText(title);
        if (desc  != null) tvDesc.setText(desc);
    }
}