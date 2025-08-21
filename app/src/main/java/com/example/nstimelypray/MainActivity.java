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
    private TextView statusText;

    private final String ZIP_URL = "https://github.com/elmika-333/nstimelypray-assets/releases/download/v1.0/videodangambar.zip";
    private final File COMPLETE_MARKER = new File(getExternalFilesDir(null), "assets/.complete");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        assetsDir = new File(getExternalMediaDirs()[0], "assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

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

        // Cek marker selesai
        if (COMPLETE_MARKER.exists()) {
            Toast.makeText(this, "Video/gambar sudah siap offline.", Toast.LENGTH_SHORT).show();
            loadOfflineHTML();
        } else {
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
        statusText = downloadDialog.findViewById(R.id.statusText);

        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressText.setText("0%");
        statusText.setText("Mengunduh data video/gambar… pastikan koneksi internet stabil");

        downloadDialog.show();
    }

    private class DownloadAndUnzipTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                // Download ZIP
                publishProgress("download", "0");
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                int totalSize = conn.getContentLength();
                File zipFile = new File(getCacheDir(), "assets.zip");
                InputStream input = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(zipFile);

                byte[] buffer = new byte[4096];
                int read;
                int downloaded = 0;
                boolean hasSize = totalSize > 0;

                while ((read = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    downloaded += read;
                    if (hasSize) {
                        int progress = (int)((downloaded / (float) totalSize) * 50);
                        publishProgress("download", String.valueOf(progress));
                    }
                }
                fos.close();
                input.close();

                // Unzip
                ZipInputStream zipCountStream = new ZipInputStream(new FileInputStream(zipFile));
                int totalEntries = 0;
                while (zipCountStream.getNextEntry() != null) totalEntries++;
                zipCountStream.close();

                ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry entry;
                int extracted = 0;

                while ((entry = zipInput.getNextEntry()) != null) {
                    File outFile = new File(assetsDir, entry.getName());
                    if (entry.isDirectory()) outFile.mkdirs();
                    else {
                        outFile.getParentFile().mkdirs();
                        FileOutputStream out = new FileOutputStream(outFile);
                        int count;
                        while ((count = zipInput.read(buffer)) != -1) out.write(buffer, 0, count);
                        out.close();
                    }
                    zipInput.closeEntry();

                    // update progress unzip 50–100%
                    extracted++;
                    int progress = 50 + (int)((extracted / (float) totalEntries) * 50);
                    publishProgress("unzip", String.valueOf(progress));
                }
                zipInput.close();

                // hapus zip sementara
                zipFile.delete();

                // buat marker selesai
                File marker = new File(assetsDir, ".complete");
                marker.createNewFile();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String type = values[0];
            int progress = Integer.parseInt(values[1]);
            if (progressBar != null && progressText != null && statusText != null) {
                progressBar.setProgress(progress);
                progressText.setText(progress + "%");

                if ("download".equals(type))
                    statusText.setText("Mengunduh data video/gambar… pastikan koneksi internet stabil");
                else if ("unzip".equals(type))
                    statusText.setText("Mengekstrak file… tunggu sebentar");
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
