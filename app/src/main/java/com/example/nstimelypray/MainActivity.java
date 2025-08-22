package com.example.nstimelypray;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    // Cursor variables
    private float cursorX = 500;
    private float cursorY = 300;
    private long lastClickTime = 0;
    private final int DOUBLE_CLICK_DELAY = 300; // ms
    private ImageView cursorOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        cursorOverlay = findViewById(R.id.cursorOverlay);
        cursorOverlay.setVisibility(View.VISIBLE);

        // siapkan folder cache
        assetsDir = new File(getCacheDir(), "nstimely_assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

        // kalau sudah ada .completed → langsung load
        File completed = new File(assetsDir, ".completed");
        if (completed.exists()) {
            loadWebView();
        } else {
            showDownloadDialog();
            new DownloadAndUnzipTask().execute("https://github.com/elmika-333/nstimelypray-assets/releases/download/v1.0/videodangambar.zip");
        }

        // set cursor awal posisi overlay
        updateCursorOverlay();
    }

    private void loadWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setTextZoom(100);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }
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
                        publishProgress((int) ((downloaded / (float) totalSize) * 50)); // 0–50%
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
                    publishProgress(50 + (int) ((filesExtracted / (float) totalEntries) * 50)); // 50–100%
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

    // Tombol remote + cursor
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int moveStep = 30;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                cursorY -= moveStep;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                cursorY += moveStep;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                cursorX -= moveStep;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                cursorX += moveStep;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                handleClick();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) webView.goBack();
                else webView.reload();
                return true;
        }

        // Update overlay posisi cursor
        updateCursorOverlay();

        return super.onKeyDown(keyCode, event);
    }

    private void updateCursorOverlay() {
        cursorOverlay.setX(cursorX);
        cursorOverlay.setY(cursorY);
    }

    private void handleClick() {
        long now = System.currentTimeMillis();

        if (now - lastClickTime < DOUBLE_CLICK_DELAY) {
            simulateClick(cursorX, cursorY); // klik pertama
            simulateClick(cursorX, cursorY); // klik kedua → double click
            lastClickTime = 0;
        } else {
            simulateClick(cursorX, cursorY);
            lastClickTime = now;
        }
    }

    private void simulateClick(float x, float y) {
        long now = System.currentTimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0);
        webView.dispatchTouchEvent(down);
        webView.dispatchTouchEvent(up);
    }
}
