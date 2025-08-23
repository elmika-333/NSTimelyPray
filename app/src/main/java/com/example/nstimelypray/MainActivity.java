package com.example.nstimelypray;

import android.app.Dialog;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Dialog downloadDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private boolean isDownloading = false;
    private File assetsDir;

    // Cursor overlay
    private ImageView cursor;
    private PointF cursorPos = new PointF(500, 500); // posisi awal cursor
    private final int CURSOR_STEP = 50; // jarak pindah per tombol D-pad

    // Double-click tracking
    private long lastClickTime = 0;
    private final long DOUBLE_CLICK_THRESHOLD = 300; // ms

    // Auto-hide cursor
    private Handler cursorHandler = new Handler();
    private Runnable hideCursorRunnable;

    private FrameLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        webView = findViewById(R.id.webView);

        // ===== buat cursor overlay =====
        cursor = new ImageView(this);
        cursor.setImageResource(R.drawable.ic_cursor); // icon cursor PNG
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cursor.setLayoutParams(lp);
        cursor.setX(cursorPos.x);
        cursor.setY(cursorPos.y);
        rootLayout.addView(cursor);
        cursor.bringToFront(); // ⬅️ pastikan di atas WebView

        // Setup hide cursor runnable
        hideCursorRunnable = () -> {
            cursor.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                cursor.setVisibility(View.INVISIBLE);
            }).start();
        };
        resetCursorHideTimer();

        // Folder cache
        assetsDir = new File(getCacheDir(), "nstimely_assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

        File completed = new File(assetsDir, ".completed");
        if (completed.exists()) {
            loadWebView();
        } else {
            showDownloadDialog();
            new DownloadAndUnzipTask().execute(
                    "https://github.com/elmika-333/nstimelypray-assets/releases/download/v1.0/videodangambar.zip"
            );
        }
    }

    private void loadWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }

        String desktopUA =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/119.0.0.0 Safari/537.36";
        webSettings.setUserAgentString(desktopUA);

        webView.setInitialScale(100);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void showDownloadDialog() {
        downloadDialog = new Dialog(this);
        downloadDialog.setContentView(R.layout.dialog_download);
        downloadDialog.setCancelable(false);

        progressBar = downloadDialog.findViewById(R.id.progressBar);
        progressText = downloadDialog.findViewById(R.id.progressText);

        if (progressBar != null) {
            progressBar.setMax(100);
            progressBar.setProgress(0);
        }
        if (progressText != null) {
            progressText.setText("0%");
        }

        downloadDialog.show();
    }

    private class DownloadAndUnzipTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            isDownloading = true;
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            File zipFile = new File(getCacheDir(), "assets.zip");
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                int totalSize = conn.getContentLength();

                InputStream input = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(zipFile);
                byte[] buffer = new byte[4096];
                int len, downloaded = 0;

                while ((len = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    downloaded += len;
                    if (totalSize > 0) {
                        publishProgress((int) ((downloaded / (float) totalSize) * 50));
                    }
                }
                fos.close();
                input.close();

                java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile);
                int totalEntries = zip.size();
                int filesExtracted = 0;

                java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    File outFile = new File(assetsDir, entry.getName());

                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        InputStream is = zip.getInputStream(entry);
                        FileOutputStream out = new FileOutputStream(outFile);
                        while ((len = is.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                        out.close();
                        is.close();
                    }

                    filesExtracted++;
                    publishProgress(50 + (int) ((filesExtracted / (float) totalEntries) * 50));
                }
                zip.close();

                new File(assetsDir, ".completed").createNewFile();
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                zipFile.delete();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (progressBar != null && progressText != null) {
                progressBar.setProgress(values[0]);
                progressText.setText(values[0] + "%");
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            isDownloading = false;
            if (downloadDialog != null) downloadDialog.dismiss();
            if (success) {
                loadWebView();
                Toast.makeText(MainActivity.this, "Download & ekstrak selesai!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Gagal mengunduh data", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ===== Tombol back + cursor =====
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        boolean movedCursor = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                cursorPos.y -= CURSOR_STEP;
                movedCursor = true;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                cursorPos.y += CURSOR_STEP;
                movedCursor = true;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                cursorPos.x -= CURSOR_STEP;
                movedCursor = true;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                cursorPos.x += CURSOR_STEP;
                movedCursor = true;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                handleClick();
                movedCursor = true;
                break;
        }

        if (movedCursor) {
            // batas agar tidak keluar layar
            cursorPos.x = Math.max(0, Math.min(cursorPos.x, rootLayout.getWidth() - cursor.getWidth()));
            cursorPos.y = Math.max(0, Math.min(cursorPos.y, rootLayout.getHeight() - cursor.getHeight()));

            cursor.setX(cursorPos.x);
            cursor.setY(cursorPos.y);
            cursor.setVisibility(ImageView.VISIBLE);
            cursor.bringToFront(); // ⬅️ supaya tidak ketutupan WebView
            resetCursorHideTimer();
            return true; // <- biar tidak tembus ke WebView
        }

        return super.onKeyDown(keyCode, event);
    }

    // ===== Klik + double-click =====
    private void handleClick() {
        long now = SystemClock.uptimeMillis();
        if (now - lastClickTime < DOUBLE_CLICK_THRESHOLD) {
            // Double click → kirim 2x event dengan jeda
            performClickAtCursor();
            cursorHandler.postDelayed(this::performClickAtCursor, 120);
        } else {
            // Single click
            performClickAtCursor();
        }
        lastClickTime = now;
    }

    private void performClickAtCursor() {
        float x = cursorPos.x + cursor.getWidth() / 2f;
        float y = cursorPos.y + cursor.getHeight() / 2f;

        long downTime = SystemClock.uptimeMillis();
        // ACTION_DOWN
        MotionEvent down = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
        );
        // ACTION_UP (beri delay 50ms)
        MotionEvent up = MotionEvent.obtain(
                downTime,
                downTime + 50,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );

        webView.dispatchTouchEvent(down);
        webView.dispatchTouchEvent(up);

        down.recycle();
        up.recycle();
    }

    // ===== Auto-hide cursor dengan fade =====
    private void resetCursorHideTimer() {
        cursorHandler.removeCallbacks(hideCursorRunnable);

        cursor.animate().alpha(1f).setDuration(200).start();
        cursor.setVisibility(ImageView.VISIBLE);
        cursor.bringToFront(); // ⬅️ pastikan setiap muncul ada di atas

        cursorHandler.postDelayed(hideCursorRunnable, 3000);
    }
}
