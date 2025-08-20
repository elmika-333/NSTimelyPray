package com.example.nstimelypray;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
    private final String ASSETS_PATH = Environment.getExternalStorageDirectory() + "/nstimelypray/assets/";
    private final String ZIP_URL = "https://drive.google.com/uc?export=download&id=1VeT2_9HDkTNBV8uLpnV9eLWa7XHO2HJ3"; // Ganti link zip asli
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

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

        // Pastikan izin storage
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        } else {
            checkAndDownloadAssets();
        }
    }

    private void checkAndDownloadAssets() {
        File folder = new File(ASSETS_PATH);
        if (folder.exists() && folder.listFiles() != null && folder.listFiles().length > 0) {
            loadOfflineHTML();
        } else {
            new DownloadAndUnzipTask().execute(ZIP_URL);
        }
    }

    private void loadOfflineHTML() {
        File indexFile = new File(ASSETS_PATH + "index.html");
        if (indexFile.exists()) {
            webView.loadUrl("file://" + indexFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "index.html tidak ditemukan di folder assets!", Toast.LENGTH_LONG).show();
        }
    }

    private class DownloadAndUnzipTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Mengunduh & mengekstrak assets...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.show();
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

                File dir = new File(ASSETS_PATH);
                if (!dir.exists()) dir.mkdirs();

                byte[] buffer = new byte[4096];

                while ((entry = zipInput.getNextEntry()) != null) {
                    File outFile = new File(ASSETS_PATH + entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
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
            if (progressDialog != null) {
                progressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (progressDialog != null) progressDialog.dismiss();
            if (success) {
                Toast.makeText(MainActivity.this, "Download & unzip selesai!", Toast.LENGTH_SHORT).show();
                loadOfflineHTML();
            } else {
                Toast.makeText(MainActivity.this, "Gagal download assets.", Toast.LENGTH_LONG).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 200) {
            checkAndDownloadAssets();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
