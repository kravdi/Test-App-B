package com.kravdi.applicationb;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kravdi.applicationb.services.DownloadService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private final String BASE_URI = "content://com.kravdi.applicationa.provider/links";

    private final Uri LINKS_URI = Uri.parse(BASE_URI);

    public static final String LINK_COLUMN = "link";
    public static final String STATE_COLUMN = "state";
    public static final String TIME_COLUMN = "time";

    private final String LINK_TAG = "link";
    private final String FROM_A = "from_A";
    private final String LINK_STATE = "link_state";
    public static final String LINK_ID = "_ID";

    public static final String SAVE_IMAGE = "image";
    private TextView mTextField;
    private ProgressBar progressBar;
    private  ImageView image;
    private int state = 3;
    private String link;
    private Intent intent;
    private InputStream in;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mTextField = (TextView) findViewById(R.id.timer);
        image = (ImageView) findViewById(R.id.image);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        if (mTextField != null) {
            mTextField.setVisibility(View.GONE);
        }
        if (image != null) {
            image.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        Bitmap bmp;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            bmp = null;
            try {
                in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(in);
                state = 1;
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
                state = 2;
            }
            return bmp;
        }

        protected void onPostExecute(Bitmap result) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (state == 2) {
                bmImage.setImageResource(R.drawable.error);
            } else {
                bmImage.setImageBitmap(result);
            }
            changeDB(intent, bmp);
        }
    }

    private void changeDB(Intent intent, Bitmap bmp) {
        ContentValues cv = new ContentValues();
        if (intent.hasExtra(FROM_A)) {
            switch (intent.getStringExtra(FROM_A)) {
                case "from_test":
                    cv.put(LINK_COLUMN, link);
                    cv.put(STATE_COLUMN, state);
                    cv.put(TIME_COLUMN, System.currentTimeMillis());
                    getContentResolver().insert(LINKS_URI, cv);
                    break;
                case "from_history":
                    int old_state = intent.getIntExtra(LINK_STATE, 4);
                    Log.d("STATE", "" + old_state);
                    switch (old_state) {
                        case 1:
                            if(bmp != null) {
                                Intent service = new Intent(MainActivity.this, DownloadService.class);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                byte[] byteArray = stream.toByteArray();
                                startService(service.putExtra(LINK_ID, intent.getIntExtra(LINK_ID, 0)).putExtra(LINK_TAG, link).putExtra(SAVE_IMAGE, byteArray));
                            } else {
                                cv.put(STATE_COLUMN, state);
                                Toast.makeText(MainActivity.this, R.string.check_connection, Toast.LENGTH_SHORT).show();
                                getContentResolver().update(LINKS_URI.buildUpon().appendPath(String.valueOf(intent.getIntExtra(LINK_ID, 0))).build(), cv, null, null);
                            }
                            break;
                        case 2:
                        case 3:
                            if (old_state != state) {
                                cv.put(STATE_COLUMN, state);
                                getContentResolver().update(LINKS_URI.buildUpon().appendPath(String.valueOf(intent.getIntExtra(LINK_ID, 0))).build(), cv, null, null);
                            }
                            break;
                        case 4:
                            cv.put(STATE_COLUMN, intent.getIntExtra(LINK_STATE, 3));
                            getContentResolver().update(LINKS_URI.buildUpon().appendPath(String.valueOf(intent.getIntExtra(LINK_ID, 0))).build(), cv, null, null);
                            break;
                    }
                    break;
            }
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (in != null)
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (timer != null)
            timer.cancel();

        this.finishAffinity();

    }

    @Override
    protected void onResume() {
        super.onResume();

        intent = getIntent();
        if (intent.hasExtra(FROM_A)) {

            progressBar.setVisibility(View.VISIBLE);
            if (image != null) {
                image.setVisibility(View.VISIBLE);
            }
            link = intent.getStringExtra(LINK_TAG);
            new DownloadImageTask(image).execute(link);

        } else {
            mTextField.setVisibility(View.VISIBLE);
            timer = new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {
                    mTextField.setText("Приложение не является самостоятельным и будет закрыто через:  " + millisUntilFinished / 1000 + " сек");
                }

                public void onFinish() {
                    System.exit(0);
                }

            }.start();
        }

    }
}


