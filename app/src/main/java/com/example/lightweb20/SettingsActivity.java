package com.example.lightweb20;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public class SettingsActivity extends AppCompatActivity {

    private Switch javascriptSwitch, googleSwitch;
    private Button saveSettingsButton, licenseButton, creditButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        javascriptSwitch = findViewById(R.id.jsEnabledSwitch);
        googleSwitch = findViewById(R.id.switch_google);
        saveSettingsButton = findViewById(R.id.saveSettingsButton);
        licenseButton = findViewById(R.id.licenseButton);
        creditButton = findViewById(R.id.creditButton);

        SharedPreferences preferences = getSharedPreferences("appPreferences", MODE_PRIVATE);

        // 設定を表示
        javascriptSwitch.setChecked(preferences.getBoolean("jsEnabled", true));
        googleSwitch.setChecked(preferences.getBoolean("googleLiteEnabled", false));

        // 設定保存ボタン
        saveSettingsButton.setOnClickListener(v -> {
            // 設定を保存
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("jsEnabled", javascriptSwitch.isChecked());
            editor.putBoolean("googleLiteEnabled", googleSwitch.isChecked());
            editor.apply();

            // 再起動を促すダイアログ表示
            new AlertDialog.Builder(SettingsActivity.this)
                    .setMessage("設定が保存されました。アプリを再起動してください。")
                    .setCancelable(false)
                    .setPositiveButton("再起動", (dialog, id) -> restartApp())
                    .setNegativeButton("後で", null)
                    .create()
                    .show();
        });

        // ライセンスボタンを押した時
        licenseButton.setOnClickListener(v -> {
            Intent licenseIntent = new Intent(SettingsActivity.this, LicenseActivity.class);
            startActivity(licenseIntent);
        });

        // クレジットボタンを押した時
        creditButton.setOnClickListener(v -> {
            Intent creditIntent = new Intent(SettingsActivity.this, CreditActivity.class);
            startActivity(creditIntent);
        });
    }

    private void restartApp() {
        // アプリの再起動
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
