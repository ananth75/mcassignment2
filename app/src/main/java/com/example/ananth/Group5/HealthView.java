package com.example.ananth.Group5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class HealthView extends AppCompatActivity {


    float[] points = {1,2,3,-1,-2};
    String[] horLabels = {"0", "10", "20", "30","40", "50"};
    String[] verLabels = {"50", "40", "30", "20", "10", "0"};
    LinearLayout graphUILayout;
    float[] randomPoints = new float[10];
    float[] downloadedPoints = new float[10];
    GraphView graph;
    boolean running = false;
    Thread runState,dbOpen;;
    SQLiteDatabase db, down_db;
    private IntentFilter mFilter;
    public static final String mBroadcastAccData = "com.example.ananth.Group5";
    private static Context ctx;
    private static final String TAG = "MyActivity";
    float rcvdXcoord,rcvdYcoord,rcvdZcoord;
    private static String patientID = "",patientAge = "",patientName = "", patientSex= "";
    private static String dbTableName = "";
    private static String uploadURI = "https://impact.asu.edu/CSE535Spring17Folder/UploadToServer.php";
    private static int httpResponse = 0;
    public static String httpResponseMsg = "";
    public static boolean registered = false;

    public static final String DATABASE_LOCATION = "/mnt/sdcard/CSE535_ASSIGNMENT2/Group5.db";
    public static final String D_DATABASE_LOCATION = "/mnt/sdcard/CSE535_ASSIGNMENT2/Group5_downloaded.db";
    public static boolean inserted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Intent intent = new Intent(this, sensorHandlerService.class);
        startService(intent);
        graph = new GraphView(this, points, "HealthMonitor", horLabels, verLabels, true);
        graphUILayout = (LinearLayout) findViewById(R.id.graphLinearLayout);
        graphUILayout.addView(graph);
        mFilter = new IntentFilter();
        mFilter.addAction(mBroadcastAccData);
        Log.e(TAG, "Registering for receiever");
        Arrays.fill(randomPoints, 0);
        Arrays.fill(downloadedPoints, 0);
    }

    protected void onResume() {
        super.onResume();
    }

    protected  void onPause() {
        if(registered) {
            unregisterReceiver(reciever);
        }
        super.onPause();
    }

    public void onDatabaseStoreClicked(View v) throws InterruptedException {
        EditText patientIDText = (EditText) findViewById(R.id.editText);
        patientID = patientIDText.getText().toString();

        EditText patientAgeText = (EditText) findViewById(R.id.editText2);
        patientAge = patientAgeText.getText().toString();

        EditText patientNameText = (EditText) findViewById(R.id.editText3);
        patientName = patientNameText.getText().toString();

        RadioButton maleB = (RadioButton)findViewById(R.id.male);
        boolean isMale = maleB.isChecked();
        RadioButton femaleB = (RadioButton)findViewById(R.id.female);
        boolean isFemale = femaleB.isChecked();


        if(patientID.isEmpty() || patientAge.isEmpty() || patientName.isEmpty() || ((isMale == false) && (isFemale == false))) {
            Toast.makeText(this, "Please enter all the patient's details", Toast.LENGTH_SHORT).show();
            return;
        }

        patientSex = "Male";
        if(isFemale) {
            patientSex = "Female";
        }
        dbTableName = patientName + "_" + patientID + "_" + patientAge + "_" + patientSex;
        try{
                dbOpen = new Thread(new Runnable() {
                    @Override
                    public void run() {
                            checkAndFixDBPath();
                            db = SQLiteDatabase.openOrCreateDatabase(DATABASE_LOCATION, null);
                            db.beginTransaction();
                            Log.d(TAG, "Database path " +db.getPath());
                            try {
                                db.execSQL("create table " + dbTableName + " ("
                                        + " patientID integer PRIMARY KEY autoincrement, "
                                        + " timestamp int, "
                                        + "  xcoord real,"
                                        + "  ycoord real,"
                                        + "  zcoord real ); ");

                                db.setTransactionSuccessful(); //commit your changes
                                Log.e(TAG,"Db  created");
                            } catch (SQLiteException e) {
                                Log.e("Exceptiontr", e.getMessage());
                            } finally {
                                db.endTransaction();
                            }
                        }
                });
                dbOpen.start();
        }catch (SQLException e){

            Toast.makeText(HealthView.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    public void checkAndFixDBPath() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/CSE535_ASSIGNMENT2");
        if(dir.exists() && dir.isDirectory()) {

        }
        else {
            dir.mkdir();
        }
    }
    public void onRunClicked(View v) throws InterruptedException {


        if(patientID.isEmpty() || patientAge.isEmpty() || patientName.isEmpty() || (patientSex).isEmpty()) {
            Toast.makeText(this, "Please enter all the patient's details", Toast.LENGTH_SHORT).show();
            return;
        }
        //Getting focus to the graph's linear layout. COde reused from StackOverflow
        //https://stackoverflow.com/questions/2150656/how-to-set-focus-on-a-view-when-a-layout-is-created-and-displayed
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        if(!registered){
            registered = true;
            registerReceiver(reciever, mFilter);
        }
        runState = new Thread(new Runnable() {
            @Override
            public void run() {

                while(!inserted) {

                }
                randomPoints = fetchFromDB();
                running = true;
                while (running) {
                    randomPoints = fetchFromDB();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                     /* Turnaround of the values to create a variadic graph pattern */
                        float init = randomPoints[0];

                        randomPoints[randomPoints.length - 1] = init;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            graph.setValues(randomPoints);
                            graphUILayout.removeView(graph);
                            graphUILayout.addView(graph);
                        }
                    });
                }
            }
        });
        if(!running)
        runState.start();
    }

    public void onStopClicked(View v) throws InterruptedException {
        //Getting focus to the graph's linear layout. COde reused from StackOverflow
        //https://stackoverflow.com/questions/2150656/how-to-set-focus-on-a-view-when-a-layout-is-created-and-displayed
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        running = false;
        graphUILayout.removeView(graph);
        Arrays.fill(randomPoints, 0);
        graph.setValues(randomPoints);
        graphUILayout.addView(graph);
    }

    public void onDatabaseUploadClicked(View V) throws InterruptedException {

        new Thread (new Runnable() {
            public void run() {
                uploadDB();
            }

        }).start();
    }


    public void onDatabaseDownloadClicked(View V) throws InterruptedException {

        new Thread (new Runnable() {
            public void run() {
                boolean status = downloadDB();
                if(status) {
                    downloadedPoints = fetchFromDownloadedDB();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            graph.setValues(downloadedPoints);
                            graphUILayout.removeView(graph);
                            graphUILayout.addView(graph);
                        }
                    });
                }
            }

        }).start();
    }
    BroadcastReceiver reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            if(intent.getAction().equals(mBroadcastAccData)){
                rcvdXcoord = intent.getFloatExtra("xcoord", 0);
                rcvdYcoord = intent.getFloatExtra("ycoord", 0);
                rcvdZcoord = intent.getFloatExtra("zcoord", 0);
                Log.d(TAG, "Accelerometer values received " + rcvdXcoord);
                pushIntoDB(rcvdXcoord, rcvdYcoord, rcvdZcoord);
            }
        }
    };

    private void uploadDB() {
        {
            if(patientID.isEmpty() || patientAge.isEmpty() || patientName.isEmpty() || (patientSex).isEmpty()) {
                //Toast.makeText(HealthView.this, "Please enter all the patient's details", Toast.LENGTH_SHORT).show();
                return;
            }
            /* Referred from http://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83&aaid=106 */
            String fileName = "Group5.db";
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            int bytesRead = 0, bytesAvailable = 0, bufferSize = 0;
            byte[] buffer;

            //1MB
            int maxBufferSize = 1024 * 1024;
            Log.d(TAG, db.getPath());
            File sourceFile = new File("/mnt/sdcard/CSE535_ASSIGNMENT2/Group5.db");

            if(sourceFile.isFile()) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                                }

                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }

                                public void checkServerTrusted(
                                        X509Certificate[] certs, String authType) {
                                }
                            }
                    };

                    try {
                        SSLContext sc = SSLContext.getInstance("TLS");
                        sc.init(null, trustAllCerts, new SecureRandom());
                        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    URL url = new URL(uploadURI);


                    // Open a HTTP  connection to  the URL
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + "*****");
                    conn.setRequestProperty("uploaded_file", fileName);

                    dos = new DataOutputStream(conn.getOutputStream());

                    /* HTTP Formatted Data */
                    dos.writeBytes("--" + "*****" + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                    + fileName + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);

                    bytesAvailable = fileInputStream.available();
                    bufferSize = (bytesAvailable > maxBufferSize) ? maxBufferSize: bytesAvailable;
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    //Read them in chunks of max buffer size i.e. 1M
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = (bytesAvailable > maxBufferSize) ? maxBufferSize: bytesAvailable;
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    dos.writeBytes(lineEnd);
                    dos.writeBytes("--" + "*****" + "--" + lineEnd);

                    httpResponse = conn.getResponseCode();
                    httpResponseMsg = conn.getResponseMessage();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(HealthView.this, "File Upload successful. Response " + httpResponseMsg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    //close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();

                } catch (MalformedURLException exception) {
                    exception.printStackTrace();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(HealthView.this, "MalformedURLException",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e(TAG, "error: " + exception.getMessage(), exception);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    Log.e(TAG, "Exception : " + exception.getMessage(), exception);
                }
            }
            else {

                Log.e(TAG, "No source File"  + db.getPath());

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(HealthView.this, "No source file to upload", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }
    }


    public boolean downloadDB() {
        HttpURLConnection conn = null;
        boolean success = false;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkServerTrusted(
                                X509Certificate[] certs, String authType) {
                        }
                    }
            };

            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
                URL url = new URL("https://impact.asu.edu/CSE535Spring17Folder/Group5.db");

                conn = (HttpsURLConnection) url.openConnection();

                BufferedInputStream inputBufferStream = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream readBufferStream = new ByteArrayOutputStream(250);

                int index = 0;
                while ((index = inputBufferStream.read()) != -1) {
                    readBufferStream.write((byte) index);
                }
                FileOutputStream fos = new FileOutputStream(new File("/mnt/sdcard/CSE535_ASSIGNMENT2/Group5_downloaded.db"));
                fos.write(readBufferStream.toByteArray());
                fos.close();

                success = true;
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(HealthView.this, "File download successful. Response " + httpResponseMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        } catch(IOException ex) {
            ex.printStackTrace();
        }
         catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    private void pushIntoDB(float x, float y, float z) {
                try {
                    db.execSQL( "insert into " +  dbTableName + "(timestamp, xcoord, ycoord, zcoord) values ('"+ System.currentTimeMillis() + "', '"+ x +"', '"+ y +"', '"+ z +"' );" );
                    inserted = true;
                    Log.v(TAG, "Insert successful");
                }
                catch (SQLiteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "SQL Exception"  + db.getPath());

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(HealthView.this, "SQLite Exeception", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
    }

    private float[] fetchFromDB() {
        try {
            String query = "select * FROM "+ dbTableName+" ORDER BY timestamp DESC limit 10";
            Log.e(TAG,query);
            Cursor c = db.rawQuery(query, null);
            float[] coords = new float[10];
            Log.v(TAG, "Number of records " + c.getCount()+ " ");

            Arrays.fill(coords, 0);
            float xval, yval, zval;
            int index = 0;
            if(c.moveToFirst()) {
                do {
                    xval = c.getFloat(c.getColumnIndex("xcoord"));
                    yval = c.getFloat(c.getColumnIndex("ycoord"));
                    zval = c.getFloat(c.getColumnIndex("zcoord"));
                    float acclRoot = (xval * xval + yval * yval + zval * zval)
                            / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                    double acclValue = Math.sqrt(acclRoot);
                    coords[index++] = (float)acclValue;
                } while(c.moveToNext());
            }
            return coords;
        }
        catch (SQLiteException e) {
        }
        return new float[0];
    }

    private float[] fetchFromDownloadedDB() {
        try {

            down_db = SQLiteDatabase.openOrCreateDatabase(D_DATABASE_LOCATION, null);
            down_db.beginTransaction();
            String query = "select * FROM "+ dbTableName+" ORDER BY timestamp DESC limit 10";
            Log.e(TAG,query);
            Cursor c = down_db.rawQuery(query, null);
            Log.e(TAG, "Cursor size " + c.getCount());
            float[] coords = new float[10];
            Log.v(TAG, "Number of records " + c.getCount()+ " ");

            Arrays.fill(coords, 0);
            float xval, yval, zval;
            int index = 0;
            if(c.moveToFirst()) {
                do {
                    xval = c.getFloat(c.getColumnIndex("xcoord"));
                    yval = c.getFloat(c.getColumnIndex("ycoord"));
                    zval = c.getFloat(c.getColumnIndex("zcoord"));
                    float acclRoot = (xval * xval + yval * yval + zval * zval)
                            / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                    double acclValue = Math.sqrt(acclRoot);
                    coords[index++] = (float)acclValue;
                } while(c.moveToNext());
            }
            down_db.setTransactionSuccessful(); //commit your changes
            return coords;
        }
        catch (SQLiteException e) {
        } finally {
            down_db.endTransaction();
        }
        return new float[0];
    }

}
