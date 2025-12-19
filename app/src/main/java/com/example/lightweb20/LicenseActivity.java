package com.example.lightweb20;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge to Edge モードを有効化
        EdgeToEdge.enable(this);

        // レイアウトの設定
        setContentView(R.layout.activity_license);

        // メインビューの取得
        View mainView = findViewById(R.id.main);

        // メインビューがnullでないことを確認
        if (mainView != null) {
            // WindowInsetsリスナーを設定
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                // システムバーのインセットを取得
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                // ビューのパディングを設定
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

                return insets;
            });
        } else {
            // ビューが見つからない場合のエラーログ
            Log.e("LicenseActivity", "Main view not found!");
        }
    }
}
