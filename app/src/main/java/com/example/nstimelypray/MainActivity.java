package com.example.nstimelypray;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressDialog progressDialog;
    private File assetsDir;

    // Link ZIP video/gambar GitHub Releases
    private final String ZIP_URL = "https://github.com/elmika-333/nstimelypray-assets/releases/download/v1.0/videodangambar.zip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        // WebView harus bisa fokus agar keydown remote masuk
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        // WebView settings
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setTextZoom(100);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Folder internal/public di Android/media
        assetsDir = new File(getExternalMediaDirs()[0], "assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

        // Mulai unduh & unzip video/gambar (ZIP)
        new DownloadAndUnzipTask().execute(ZIP_URL);

        // Load index.html dari APK assets
        loadOfflineHTML();
    }

    private void loadOfflineHTML() {
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class DownloadAndUnzipTask extends AsyncTask<String, Integer, Boolean> {

        private Dialog downloadDialog;
        private ProgressBar progressBar;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Buat custom dialog
            downloadDialog = new Dialog(MainActivity.this);
            downloadDialog.setContentView(R.layout.dialog_download);
            downloadDialog.setCancelable(false);

            // Set background sekeliling gelap
            if (downloadDialog.getWindow() != null) {
                downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
                downloadDialog.getWindow().setDimAmount(0.8f); // semi-transparent
            }

            progressBar = downloadDialog.findViewById(R.id.progress_bar);
            downloadDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                int totalSize = connection.getContentLength();

                InputStream input = connection.getInputStream();
                ZipInputStream zipInput = new ZipInputStream(input);
                ZipEntry entry;
                int extractedBytes = 0;
                byte[] buffer = new byte[4096];

                while ((entry = zipInput.getNextEntry()) != null) {
                    File outFile = new File(assetsDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(outFile);
                        int count;
                        while ((count = zipInput.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                            extractedBytes += count;
                            if (totalSize > 0)
                                publishProgress((int) ((extractedBytes / (float) totalSize) * 100));
                        }
                        fos.close();
                    }
                    zipInput.closeEntry();
                }
                zipInput.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (progressBar != null) progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (downloadDialog != null && downloadDialog.isShowing()) {
                downloadDialog.dismiss();
            }
            if (success) {
                Toast.makeText(MainActivity.this, "Video/gambar siap!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Gagal download video/gambar.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onBackPressed() { }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F5 || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) {
            webView.reload();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
