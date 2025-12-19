package com.example.lightweb20;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Intent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch javascriptSwitch, googleSwitch;
    private TextView saveSettingsButton, licenseButton, creditButton,theme;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // MaterialSwitch に変更する!!
        javascriptSwitch = findViewById(R.id.jsEnabledSwitch);
        googleSwitch = findViewById(R.id.switch_google);

        saveSettingsButton = findViewById(R.id.saveSettingsButton);
        licenseButton = findViewById(R.id.licenseButton);
        creditButton = findViewById(R.id.creditButton);
        // ここを追加（レイアウト内の id が theme の場合）
        theme = findViewById(R.id.theme);
        theme.setOnClickListener(v -> {
          //  Intent themeIntent = new Intent(SettingsActivity.this, ThemeActivity.class);
        //    startActivity(themeIntent);
        });

        SharedPreferences preferences = getSharedPreferences("appPreferences", MODE_PRIVATE);

        // 設定を表示
        javascriptSwitch.setChecked(preferences.getBoolean("jsEnabled", true));
        googleSwitch.setChecked(preferences.getBoolean("googleLiteEnabled", false));

        // 設定保存ボタン
        saveSettingsButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("jsEnabled", javascriptSwitch.isChecked());
            editor.putBoolean("googleLiteEnabled", googleSwitch.isChecked());
            editor.apply();

            new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setMessage("設定が保存されました。アプリを再起動してください...")
                    .setCancelable(false)
                    .setPositiveButton("再起動!", (dialog, id) -> restartApp())
                    .setNegativeButton("後で...", null)
                    .create()
                    .show();
        });

        // ライセンス
        licenseButton.setOnClickListener(v -> {
            Intent licenseIntent = new Intent(SettingsActivity.this, LicenseActivity.class);
            startActivity(licenseIntent);
        });

        // クレジット
        creditButton.setOnClickListener(v -> {
            Intent creditIntent = new Intent(SettingsActivity.this, CreditActivity.class);
            startActivity(creditIntent);
        });
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }
}
