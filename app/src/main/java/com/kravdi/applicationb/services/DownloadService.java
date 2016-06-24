package com.kravdi.applicationb.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.kravdi.applicationb.MainActivity;
import com.kravdi.applicationb.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class DownloadService extends Service {

    private final String BASE_URI = "content://com.kravdi.applicationa.provider/links";
    private final Uri LINKS_URI = Uri.parse(BASE_URI);
    private final String EXTERNAL_PATH = "/BIGDIG/test/B";
    private final int NOTIF_ID = 101;
    private File imageFile;
    private  String filename;
    final String LOG_TAG = "myLogs";

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "start service");
        String tmpLocation = Environment.getExternalStorageDirectory().getPath() + EXTERNAL_PATH;
        imageFile = new File(tmpLocation);
        if (!imageFile.exists()) {
            imageFile.mkdirs();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        String remoteUrl = intent.getExtras().getString(MainActivity.LINK_TAG);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.btn_default_small)
                        .setContentTitle("Сохранение картинки")
                        .setContentText(remoteUrl);

        Notification notification = mBuilder.build();
        startForeground(NOTIF_ID, notification);
        final int id = intent.getIntExtra(MainActivity.LINK_ID, 0);
        filename = "image" + id + remoteUrl.substring(remoteUrl.lastIndexOf("."));
        new saveImageTask(MainActivity.image).execute(remoteUrl);
        deleteFromDB(id);
        return START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    void deleteFromDB(final int id){
        new CountDownTimer(15000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                getContentResolver().delete(LINKS_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null);
                Toast.makeText(DownloadService.this, R.string.delete_fromDB, Toast.LENGTH_SHORT).show();
                stopSelf();
            }

        }.start();

    }

    private class saveImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        Bitmap bmp;

        public saveImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            bmp = null;
            try {
                MainActivity.in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(MainActivity.in);
                storeImage(bmp, filename );
            } catch (Exception e) {
                Log.e("Error", "" + e.getMessage());
                e.printStackTrace();
            }
            return bmp;
        }

        protected void onPostExecute(Bitmap result) {
            if (MainActivity.progressBar != null) {
                MainActivity.progressBar.setVisibility(View.GONE);
            }
            if (result != null) {
                bmImage.setImageBitmap(result);
            } else {
                bmImage.setImageResource(R.drawable.error);
            }
        }
    }
    private void storeImage(Bitmap bmp, String filename) {
        File pictureFile = new File(imageFile.getPath() + File.separator + filename);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("TAG", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("TAG", "Error accessing file: " + e.getMessage());
        }
    }
}
