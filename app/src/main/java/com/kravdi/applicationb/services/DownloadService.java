package com.kravdi.applicationb.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.kravdi.applicationb.MainActivity;
import com.kravdi.applicationb.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;


public class DownloadService extends Service {

    private final String BASE_URI = "content://com.kravdi.applicationa.provider/links";
    private final Uri LINKS_URI = Uri.parse(BASE_URI);
    private final String EXTERNAL_PATH = "/BIGDIG/test/B";
    private final int NOTIF_ID = 101;
    private File image;
    final String LOG_TAG = "myLogs";

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "start service");
        String tmpLocation = Environment.getExternalStorageDirectory().getPath() + EXTERNAL_PATH;
        image = new File(tmpLocation);
        if (!image.exists()) {
            image.mkdirs();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        String remoteUrl = intent.getExtras().getString("link");

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.btn_default_small)
                        .setContentTitle("Сохранение картинки")
                        .setContentText(remoteUrl);

        Notification notification = mBuilder.build();
        startForeground(NOTIF_ID, notification);
        final int id = intent.getIntExtra(MainActivity.LINK_ID, 0);
        byte[] byteArray = intent.getByteArrayExtra(MainActivity.SAVE_IMAGE);
        String filename = "image" + id + remoteUrl.substring(remoteUrl.lastIndexOf("."));
        saveImage(byteArray, filename);
        deleteFromDB(id);
        return START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    void saveImage(final byte[] byteArray, final String filename) {
        new Thread(new Runnable() {
            public void run() {
                File tmp = new File(image.getPath() + File.separator + filename);

                BufferedOutputStream bos;
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(tmp));
                    bos.write(byteArray);
                    bos.flush();
                    bos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
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
}
