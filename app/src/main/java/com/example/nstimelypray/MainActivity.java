package com.example.nstimelypray;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- ambil resolusi layar ---
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        int widthPx = metrics.widthPixels;
        int heightPx = metrics.heightPixels;
        int dpi = metrics.densityDpi;

        // convert ke "dp" (density-independent pixels)
        float widthDp = widthPx / (dpi / 160f);
        float heightDp = heightPx / (dpi / 160f);

        String info = "Resolusi (px): " + widthPx + " x " + heightPx +
                      "\nDPI: " + dpi +
                      "\nWidth DP: " + String.format("%.1f", widthDp) +
                      "\nHeight DP: " + String.format("%.1f", heightDp);

        // --- bikin overlay TextView ---
        TextView textView = new TextView(this);
        textView.setText(info);
        textView.setTextSize(28);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // --- tampilkan di atas WebView (placeholder URL) ---
        FrameLayout root = new FrameLayout(this);
        root.addView(textView);
        setContentView(root);
    }
}
