package com.example.nstimelypray;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private File assetsDir;
    private Dialog downloadDialog;
    private ProgressBar progressBar;
    private TextView progressText;

    private final String ZIP_URL = "https://github.com/elmika-333/nstimelypray-assets/releases/download/v1.0/videodangambar.zip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

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

        assetsDir = new File(getExternalMediaDirs()[0], "assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

        // Cek jika folder sudah ada isinya
        if (assetsDir.listFiles() != null && assetsDir.listFiles().length > 0) {
            Toast.makeText(this, "Video/gambar sudah ada, langsung load...", Toast.LENGTH_SHORT).show();
            loadOfflineHTML();
        } else {
            // Jika belum ada, mulai download
            showDownloadDialog();
            new DownloadAndUnzipTask().execute(ZIP_URL);
        }
    }

    private void loadOfflineHTML() {
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void showDownloadDialog() {
        downloadDialog = new Dialog(this);
        downloadDialog.setContentView(R.layout.dialog_download);
        downloadDialog.setCancelable(false);
        downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        progressBar = downloadDialog.findViewById(R.id.progressBar);
        progressText = downloadDialog.findViewById(R.id.progressText);

        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressText.setText("0%");

        downloadDialog.show();
    }

    private class DownloadAndUnzipTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                // 1️⃣ Download ZIP
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                int totalSize = connection.getContentLength();

                File zipFile = new File(getCacheDir(), "assets.zip");
                InputStream input = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(zipFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                int downloaded = 0;

                while ((bytesRead = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    if (totalSize > 0) {
                        int progress = (int)((downloaded / (float) totalSize) * 50); // 0–50% untuk download
                        publishProgress(progress);
                    }
                }
                fos.close();
                input.close();

                // 2️⃣ Hitung jumlah entry ZIP
                ZipInputStream zipCountStream = new ZipInputStream(new FileInputStream(zipFile));
                int totalEntries = 0;
                while (zipCountStream.getNextEntry() != null) totalEntries++;
                zipCountStream.close();

                // 3️⃣ Ekstrak ZIP
                ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry entry;
                int extractedEntries = 0;

                while ((entry = zipInput.getNextEntry()) != null) {
                    File outFile = new File(assetsDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        FileOutputStream out = new FileOutputStream(outFile);
                        int count;
                        while ((count = zipInput.read(buffer)) != -1) out.write(buffer, 0, count);
                        out.close();
                    }
                    zipInput.closeEntry();

                    // Update progress unzip 50–100%
                    extractedEntries++;
                    int progress = 50 + (int)((extractedEntries / (float) totalEntries) * 50);
                    publishProgress(progress);
                }
                zipInput.close();

                // Hapus file ZIP sementara
                zipFile.delete();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (progressBar != null && progressText != null) {
                progressBar.setProgress(values[0]);
                progressText.setText(values[0] + "%");
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (downloadDialog != null) downloadDialog.dismiss();
            if (success) {
                Toast.makeText(MainActivity.this, "Video/gambar siap!", Toast.LENGTH_SHORT).show();
                loadOfflineHTML();
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
