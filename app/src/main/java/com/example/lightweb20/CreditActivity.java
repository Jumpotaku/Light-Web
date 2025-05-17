package com.example.lightweb20;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class CreditActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit);
    }

    // Twitter（X）のURLを開く
    public void openTwitterUrl(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/bKXipkiiRU7R8wb"));
        startActivity(intent);
    }

    // YouTube のURLを開く
    public void openYoutubeUrl(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@%E3%82%B9%E3%83%9E%E3%83%9B%E3%82%AA%E3%82%BF%E3%82%AF"));
        startActivity(intent);
    }
}
