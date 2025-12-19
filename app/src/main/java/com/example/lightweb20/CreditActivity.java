package com.example.lightweb20;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CreditActivity extends AppCompatActivity {

    private ImageView imageView1;
    private TextView programmerLabel;
    private TextView loveBlueArchiveLabel;
    private TextView developerMessageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit); // ← XML のファイル名に合わせて変更！

        // --- View を取得（最低限） ---
        imageView1 = findViewById(R.id.imageView1);
        programmerLabel = findViewById(R.id.programmerLabel);
        loveBlueArchiveLabel = findViewById(R.id.a);
        developerMessageInput = findViewById(R.id.developerMessageInput);

        // ここに、必要なら後でクリック処理など追加できます
    }
}
