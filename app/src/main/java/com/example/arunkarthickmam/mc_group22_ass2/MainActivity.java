package com.example.arunkarthickmam.mc_group22_ass2;
/***
 * File Created by Arun Karthick M A M
 */

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.hardware.*;
import android.os.*;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    //DataBase Variables
    SQLiteDatabase db, db1;
    //UI Buttons
    Button runButton, stopButton, uploadButton, downloadButton;

    //Inputs
    EditText patientName, patientid, age;
    RadioGroup gendergroup;

    //Graph variables
    private static final String TimeStamp = "TimeStamp";
    private static final String x_Axis = "x_Axis";
    private static final String y_Axis = "y_Axis";
    private static final String z_Axis = "z_Axis";

    //Accelerometer sensors
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    public static String patientidText, patientNameText, ageText, genderText; //Patient Info
    public static int value;
    private float lastStored_x, lastStored_y, lastStored_z;
    private long lastUpdate = 0;
    boolean senseFlag = false;
    RadioButton radiobtn;
    boolean running = false;

    //-------------------------------------------------------

    private Runnable runTimer;
    boolean dataStoredInDB = false;
    //-------------------------------------------------------
    private double graph2LastXValue = 0d;
    private final Handler mHandler = new Handler();

    //X Y Z Series
    private LineGraphSeries<DataPoint> mSeries1;
    private LineGraphSeries<DataPoint> mSeries2;
    private LineGraphSeries<DataPoint> mSeries3;
    //-------------------------------------------------------
    String tableName = "";
    ArrayList<Float> xArrayList, yArrayList, zArrayList;

    int secCounter=0;
    int currentElement=0;
    boolean tableExistCheck=false;
    boolean tableExistCheck_onSensorChanged=false;

    //-------------------------------------------------------
    //Directories
    String downloadDir = "/Android/Data/CSE535_ASSIGNMENT2_DOWN/";
    String fileName = "Assignment2_Group22.db";
    String baseDbDir="/Android/Data/CSE535_ASSIGNMENT2";
    String phpPath = "http://impact.asu.edu/CSE535Spring18Folder/UploadToServer.php";
    String serverPath = "http://impact.asu.edu/CSE535Spring18Folder/";
    //-------------------------------------------------------
    boolean clearTextFlag=false;
    boolean isStart = true;
    boolean isFinished = false;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //File Access Permissions
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        //These array  list holds the values for the three graph axises to be plotted
        xArrayList=new ArrayList<Float>();
        yArrayList=new ArrayList<Float>();
        zArrayList=new ArrayList<Float>();

        //fetching the UI components
        runButton = (Button) findViewById(R.id.Run);
        stopButton = (Button) findViewById(R.id.Stop);
        uploadButton = (Button) findViewById(R.id.upload);
        downloadButton = (Button) findViewById(R.id.download);

        patientid = (EditText) findViewById(R.id.PatientID);
        patientName = (EditText) findViewById(R.id.PatientName);
        age = (EditText) findViewById(R.id.Age);

        //Initial Customization
        patientName.requestFocus();
        stopButton.setEnabled(false);
        gendergroup = (RadioGroup) findViewById(R.id.Gendergroup);

        //Sensor Declaration section
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor , SensorManager.SENSOR_DELAY_NORMAL);

        //Graph Initialization
        final GraphView graph = (GraphView) findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<>();
        mSeries2 = new LineGraphSeries<>();
        mSeries3 = new LineGraphSeries<>();

        //Giving Distinct Colors
        mSeries1.setColor(Color.RED);
        mSeries2.setColor(Color.GREEN);
        mSeries3.setColor(Color.BLUE);
        //adding series to graph
        graph.addSeries(mSeries1);
        graph.addSeries(mSeries2);
        graph.addSeries(mSeries3);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        graph.getGridLabelRenderer().setLabelVerticalWidth(60);

        GridLabelRenderer gridLabel;
        gridLabel = graph.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Timestamp");
        gridLabel.setVerticalAxisTitle("Acc Data");
        gridLabel.setHorizontalAxisTitleColor(-65536);
        gridLabel.setHorizontalAxisTitleTextSize(50);
        gridLabel.setVerticalAxisTitleTextSize(50);

        gridLabel.setVerticalAxisTitleColor(-65536);

        patientid.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                patientid.setInputType(InputType.TYPE_CLASS_TEXT);
                patientidText = patientid.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable editable)
            {

            }
        });

        patientName.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                patientName.setInputType(InputType.TYPE_CLASS_TEXT);
                patientNameText = patientName.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable editable)
            {

            }
        });

        age.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                age.setInputType(InputType.TYPE_CLASS_NUMBER);
                ageText = age.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable editable)
            {

            }
        });

        gendergroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i)
            {
                value = radioGroup.getCheckedRadioButtonId();
                radiobtn = (RadioButton) findViewById(value);
                genderText = radiobtn.getText().toString();
            }
        });

        runButton.setOnClickListener(new View.OnClickListener() //Run button Click listener
        {
            public void onClick(View v)
            {
                if (currentElement == 0)
                {
                    boolean isTableCreated = createTableForPatient();
                    if (isTableCreated) {
                        tableExistCheck_onSensorChanged = false;
                        secCounter = 0;
                        resetTheGraph();
                        flagBasedUpdate(false);
                        Toast.makeText(MainActivity.this, "In Progress..", Toast.LENGTH_SHORT).show();
                        if (!running)
                        {
                            running = true;
                            runButton.setEnabled(false);
                            stopButton.setEnabled(true);
                            uploadButton.setEnabled(true);
                            downloadButton.setEnabled(false);
                            onResume();
                            Log.d("Running", "running flag is true");
                        }
                    }
                }
                else
                {
                    if (!running)
                    {
                        running = true;
                        runButton.setEnabled(false);
                        stopButton.setEnabled(true);
                        uploadButton.setEnabled(true);
                        downloadButton.setEnabled(false);
                        graph.addSeries(mSeries1);
                        graph.addSeries(mSeries2);
                        graph.addSeries(mSeries3);
                        onResume();
                        Log.d("Running", "running flag is true");
                    }
                }
            }

        });

        stopButton.setOnClickListener(new View.OnClickListener() //Stop button Click Listener
        {
            public void onClick(View v)
            {
                isStart = true;
                isFinished = false;
                running = false;
                flagBasedUpdate(true);
                runButton.setEnabled(true);
                stopButton.setEnabled(false);
                uploadButton.setEnabled(true);
                downloadButton.setEnabled(true);
                resetTheGraph();
                graph.removeAllSeries();
                Log.d("Stopping", "running flag is false");
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener()  //Upload button Click Listener
        {
            public void onClick(View v)
            {
                AsyncUploadFile upTask = new AsyncUploadFile();
                upTask.execute();
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener()   //Download button Click Listener
        {
            public void onClick(View v)
            {
                //Resetting UI components

                if(patientNameText!=null && !patientNameText.equalsIgnoreCase(""))
                {
                    if(patientidText!=null && !patientidText.equalsIgnoreCase(""))
                    {
                        if(ageText!=null && !ageText.equalsIgnoreCase(""))
                        {
                            if(genderText!=null)
                            {
                                resetTheGraph();
                                tableName = patientNameText + "_" + patientidText + "_" + ageText + "_" + genderText;
                                //Asynchronous File Download
                                AsyncDownloadFile downTask = new AsyncDownloadFile();
                                downTask.execute();
                                flagBasedUpdate(false);
                                stopButton.setEnabled(true);
                            }
                            else
                            {
                                gendergroup.requestFocus();
                                Toast.makeText(MainActivity.this, "Please enter Patient Sex", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this, "Please enter Patient Age", Toast.LENGTH_SHORT).show();
                            age.requestFocus();
                        }
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Please enter Patient Id", Toast.LENGTH_SHORT).show();
                        patientid.requestFocus();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please enter Patient Name", Toast.LENGTH_SHORT).show();
                    patientName.requestFocus();

                }



            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int rCode, String permissions[], int[] grantResults)
    {
        switch (rCode)
        {
            case 1:
            {
                // Checks if the permissions are granted - do nothing
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                }
                else
                {
                    Toast.makeText(MainActivity.this, "Permission denied - For Storage Read", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    //Sensor section
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) //Accelerometer sensor
    {
        Sensor mySensor = sensorEvent.sensor;
        if(clearTextFlag)
        {
            clearTextFlag=false;
            flagBasedUpdate(true);
            stopButton.setEnabled(false);
            RadioButton male, female;
            male = (RadioButton) findViewById(R.id.Male);
            female = (RadioButton) findViewById(R.id.Female);
            patientid.setText("");
            patientName.setText("");
            age.setText("");
            male.setChecked(true);
            female.setChecked(false);
        }
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER && senseFlag && running)
        {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            long curTime = System.currentTimeMillis();


            if ((curTime - lastUpdate) > 1000)
            {
                lastUpdate = curTime;

                if(tableExistCheck && !isFinished)
                {
                    if(isStart)
                    {
                        isStart = false;
                        Toast.makeText(MainActivity.this, "Table already created - Fetching data", Toast.LENGTH_SHORT).show();
                        fetchData();
                        currentElement = xArrayList.size() > 10 ? xArrayList.size() - 11 : 0;
                    }

                    if(currentElement>=xArrayList.size()) {
                        isFinished = true;
                    }
                    else
                    {
                        mSeries1.appendData(new DataPoint(graph2LastXValue, xArrayList.get(currentElement)), true, 15);
                        mSeries2.appendData(new DataPoint(graph2LastXValue, yArrayList.get(currentElement)), true, 15);
                        mSeries3.appendData(new DataPoint(graph2LastXValue, zArrayList.get(currentElement)), true, 15);
                        graph2LastXValue = graph2LastXValue + 1d;
                        currentElement++;
                    }
                }
                else
                {
                    lastStored_x = x;
                    lastStored_y = y;
                    lastStored_z = z;
                    try {
                        ContentValues values = new ContentValues();
                        values.put("TimeStamp", System.currentTimeMillis());
                        values.put("x_Axis", lastStored_x);
                        values.put("y_Axis", lastStored_y);
                        values.put("z_Axis", lastStored_z);
                        db.beginTransaction();
                        db.insert(patientNameText + "_" + patientidText + "_" + ageText + "_" + genderText, null, values);
                        db.setTransactionSuccessful();

                        mSeries1.appendData(new DataPoint(graph2LastXValue, lastStored_x), true, 15);
                        mSeries2.appendData(new DataPoint(graph2LastXValue, lastStored_y), true, 15);
                        mSeries3.appendData(new DataPoint(graph2LastXValue, lastStored_z), true, 15);
                        graph2LastXValue = graph2LastXValue + 1d;
                    }
                    catch (SQLiteException e) {
                        e.printStackTrace();
                    }
                    finally {
                        db.endTransaction();
                    }
                }
            }

        }
    }

    public void resetTheGraph()
    {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.removeAllSeries();
        mSeries1 = new LineGraphSeries<>();
        mSeries2 = new LineGraphSeries<>();
        mSeries3 = new LineGraphSeries<>();
        mSeries1.setColor(Color.RED);
        mSeries2.setColor(Color.GREEN);
        mSeries3.setColor(Color.BLUE);
        graph.addSeries(mSeries1);
        graph.addSeries(mSeries2);
        graph.addSeries(mSeries3);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        graph2LastXValue=0d;
    }

    public void flagBasedUpdate(boolean flag)
    {
        patientid.setEnabled(flag);
        patientName.setEnabled(flag);
        age.setEnabled(flag);
        runButton.setEnabled(flag);
        stopButton.setEnabled(flag);
        uploadButton.setEnabled(flag);
        downloadButton.setEnabled(flag);
        RadioButton male, female;
        male = (RadioButton) findViewById(R.id.Male);
        female = (RadioButton) findViewById(R.id.Female);
        male.setEnabled(flag);
        female.setEnabled(flag);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }

    protected void onPause()
    {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    //Check if the table exists in the DB
    public boolean checkTableExistence(String tableName)
    {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+tableName+"'", null);
        if(cursor!=null)
        {
            if(cursor.getCount()>0)
            {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    //Create Table with the given attributes
    public boolean createTableForPatient()
    {
        if(patientNameText!=null && !patientNameText.equalsIgnoreCase(""))
        {
            if(patientidText!=null && !patientidText.equalsIgnoreCase(""))
            {
                if(ageText!=null && !ageText.equalsIgnoreCase(""))
                {
                    if(genderText!=null)
                    {
                        try
                        {
                            File folder = new File(Environment.getExternalStorageDirectory() + baseDbDir);
                            boolean success = true;
                            if (!folder.exists())
                            {
                                success = folder.mkdirs();
                            }
                            if (success)
                            {

                            }
                            else
                            {

                            }
                            db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + baseDbDir + "/" + fileName, null);
                            db.beginTransaction();
                            try
                            {
                                tableName = patientNameText + "_" + patientidText + "_" + ageText + "_" + genderText;
                                tableExistCheck = checkTableExistence(tableName);
                                if(!tableExistCheck)
                                {
                                    db.execSQL("CREATE TABLE " + patientNameText + "_" + patientidText + "_" + ageText + "_" + genderText +
                                            "(" + TimeStamp + " BIGINT PRIMARY KEY, " + x_Axis + " INTEGER, " + y_Axis + " INTEGER, " + z_Axis + " INTEGER" + ")");
                                    db.setTransactionSuccessful();
                                }
                                senseFlag = true;
                            }
                            catch (SQLiteException e)
                            {
                                Toast.makeText(MainActivity.this, "SQL Exception" + e.toString(), Toast.LENGTH_SHORT).show();
                            }
                            finally
                            {
                                db.endTransaction();
                            }
                        }
                        catch (SQLException e)
                        {

                        }
                        return true;
                    }
                    else
                    {
                        gendergroup.requestFocus();
                        Toast.makeText(MainActivity.this, "Please enter Patient Sex", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please enter Patient Age", Toast.LENGTH_SHORT).show();
                    age.requestFocus();
                    return false;
                }
            }
            else
            {
                Toast.makeText(MainActivity.this, "Please enter Patient Id", Toast.LENGTH_SHORT).show();
                patientid.requestFocus();
                return false;
            }
        }
        else
        {
            Toast.makeText(MainActivity.this, "Please enter Patient Name", Toast.LENGTH_SHORT).show();
            patientName.requestFocus();
            return false;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        runTimer = new Runnable()
        {
            @Override
            public void run()
            {
                if(running && dataStoredInDB && currentElement < xArrayList.size())
                {
                    mSeries1.appendData(new DataPoint(graph2LastXValue, xArrayList.get(currentElement)), true, 15);
                    mSeries2.appendData(new DataPoint(graph2LastXValue, yArrayList.get(currentElement)), true, 15);
                    mSeries3.appendData(new DataPoint(graph2LastXValue, zArrayList.get(currentElement)), true, 15);
                    graph2LastXValue = graph2LastXValue + 1d;
                    currentElement++;
                    mHandler.postDelayed(this, 1000);
                }
                else if(currentElement != 0 && currentElement>=xArrayList.size())
                {
                    dataStoredInDB=false;
                    xArrayList.clear();
                    yArrayList.clear();
                    zArrayList.clear();
                    currentElement=0;
                    //running=false;
                    runButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    downloadButton.setEnabled(true);
                }
            }
        };
        mHandler.postDelayed(runTimer, 1000);
    }

    //Read the existing data from Database we created
    public void fetchData()
    {
        final String TABLE_NAME = tableName;
        String selectQuery = "SELECT  * FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
        {
            do
            {
                xArrayList.add(Float.parseFloat(cursor.getString(1)));
                yArrayList.add(Float.parseFloat(cursor.getString(2)));
                zArrayList.add(Float.parseFloat(cursor.getString(3)));

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    //Download File from the server
    private class AsyncDownloadFile extends AsyncTask<Void, String, Void>
    {
        //Status Flag
        boolean flag = false;
        ProgressDialog waitDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            waitDialog = ProgressDialog.show(MainActivity.this,"","Downloading... Please wait!",true);
        }

        protected Void doInBackground(Void... params) {
            try {
                //create a local directory to save the downloaded table
                File downloadedFile1 = new File(android.os.Environment.getExternalStorageDirectory(), downloadDir);

                if(!downloadedFile1.isDirectory())
                    downloadedFile1.mkdir();

                //Downloaded File full local path
                File downloadedFile = new File(downloadedFile1, fileName);
                URL urlPath = new URL(serverPath + fileName);

                //Http Connection
                HttpURLConnection urlConnect = (HttpURLConnection) urlPath.openConnection();
                int contentLength = urlConnect.getContentLength();
                DataInputStream iStream = new DataInputStream(urlPath.openStream());
                byte[] buffer = new byte[contentLength];

                int length;
                FileOutputStream fStream = new FileOutputStream(downloadedFile);
                DataOutputStream oStream = new DataOutputStream(fStream);

                //input buffer to output buffer
                while ((length = iStream.read(buffer)) != -1) {
                    oStream.write(buffer, 0, length);
                }
                iStream.close();
                oStream.flush();
                oStream.close();
            }

            catch (FileNotFoundException e)
            {
                flag = true;
                System.out.println(e.getMessage());
                publishProgress(e.getMessage());
            }
            catch (MalformedURLException e)
            {
                flag = true;
                publishProgress(e.getMessage());
                e.printStackTrace();
            }
            catch (IOException e)
            {
                flag = true;
                publishProgress(e.getMessage());
            }
            return null;
        }

        protected void onProgressUpdate(String... value) {
            super.onProgressUpdate(value);
            //Error Message if any
            if(flag)
                Toast.makeText(MainActivity.this, "ERROR:  "+value[0], Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            waitDialog.dismiss();

            //Do this only if Download is completed succesfully
            if(!flag)
            {
                try {
                    Toast.makeText(MainActivity.this, "Download completed!", Toast.LENGTH_SHORT).show();

                    db1 = SQLiteDatabase.openOrCreateDatabase(android.os.Environment.getExternalStorageDirectory() + downloadDir + fileName, null);
                    db1.beginTransaction();

                    Cursor c = db1.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
                    c.moveToLast();

                    final String TABLE_NAME = tableName;
                    String selectQuery = "SELECT  * FROM " + TABLE_NAME;
                    Cursor cursor = db1.rawQuery(selectQuery, null);

                    if (cursor.moveToFirst())
                    {
                        do
                        {
                            xArrayList.add(Float.parseFloat(cursor.getString(1)));
                            yArrayList.add(Float.parseFloat(cursor.getString(2)));
                            zArrayList.add(Float.parseFloat(cursor.getString(3)));

                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                    currentElement = xArrayList.size() > 10 ? xArrayList.size() - 11 : 0;
                    //Plot the downloaded data in the graph
                    final Timer timer = new Timer();
                    timer.scheduleAtFixedRate( new TimerTask() {
                        public void run() {

                            if(currentElement<xArrayList.size())
                            {

                                mSeries1.appendData(new DataPoint(graph2LastXValue, xArrayList.get(currentElement)), true, 15);
                                mSeries2.appendData(new DataPoint(graph2LastXValue, yArrayList.get(currentElement)), true, 15);
                                mSeries3.appendData(new DataPoint(graph2LastXValue, zArrayList.get(currentElement)), true, 15);
                                graph2LastXValue += 1d;
                                currentElement++;
                            }
                            else if(currentElement != 0 && currentElement>=xArrayList.size())
                            {
                                xArrayList.clear();
                                yArrayList.clear();
                                zArrayList.clear();
                                currentElement=0;
                                //clearTextFlag=true;
                                timer.cancel();
                                timer.purge();

                            }
                        }
                    }, 0, 1000);
                }
                catch(Exception ex)
                {
                    Toast.makeText(MainActivity.this, "Patient Data Doesn't exsists", Toast.LENGTH_SHORT).show();
                }
                finally {
                    db1.endTransaction();
                    flagBasedUpdate(true);
                }

            }
        }
    }

    //Upload the patient table to the server
    private class AsyncUploadFile extends AsyncTask<Void, String, Void>
    {
        //Status Flag
        int flag = 0;
        ProgressDialog waitDialog;
        //string formatter
        String n = "\r\n";
        String hyphen = "--";
        String boundary =  "*****";

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            waitDialog = ProgressDialog.show(MainActivity.this,"","Uploading to the server... Please Wait!",true);
        }

        protected Void doInBackground(Void... params) {
            try {
                //fetching the local db file
                File fileToUpload = new File(String.valueOf(android.os.Environment.getExternalStorageDirectory()) + baseDbDir + "/", fileName);
                //Impact lab server php file path
                URL upPath = new URL(phpPath);
                HttpURLConnection urlConnect = (HttpURLConnection) upPath.openConnection();
                urlConnect.setDoOutput(true);
                urlConnect.setDoInput(true);
                urlConnect.setUseCaches(false);
                //creating a post request
                urlConnect.setRequestMethod("POST");
                urlConnect.setRequestProperty("Connection", "Keep-Alive");
                urlConnect.setRequestProperty("ENCTYPE", "multipart/form-data");
                urlConnect.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                urlConnect.setRequestProperty("uploaded_file", String.valueOf(fileToUpload));
                DataOutputStream upStream = new DataOutputStream(urlConnect.getOutputStream());

                //open output stream in server
                upStream.writeBytes(hyphen + boundary + n);
                upStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + String.valueOf(fileToUpload) + "\""+n);
                upStream.writeBytes(n);
                FileInputStream fStream = new FileInputStream(fileToUpload);
                DataInputStream readStream = new DataInputStream(fStream);
                int length;
                byte[] buffer = new byte[4096];
                //Loop until eof
                while ((length = readStream.read(buffer)) != -1) {
                    upStream.write(buffer, 0, length);
                }

                upStream.writeBytes(n);
                upStream.writeBytes(hyphen + boundary + hyphen + n);

                //File Upload Response
                if(urlConnect.getResponseCode() == 200)
                {
                    flag = 1;
                }
                readStream.close();
                fStream.close();
                upStream.close();

            }
            catch (MalformedURLException ex)
            {
                flag = 2;
                publishProgress(ex.getMessage());
                Log.e("Debug", "error: " + ex.getMessage(), ex);
            }
            catch (FileNotFoundException nf)
            {
                flag = 2;
                publishProgress(nf.getMessage());
            }
            catch (IOException ioe)
            {
                Log.e("Debug", "error: " + ioe.getMessage(), ioe);
                flag = 2;
                publishProgress(ioe.getMessage());
            }
            return null;
        }

        protected void onProgressUpdate(String... value) {
            super.onProgressUpdate(value);
            //Error Message to diplay if any
            if(flag == 2)
                Toast.makeText(MainActivity.this, "ERROR: "+ value[0], Toast.LENGTH_LONG).show();
        }


        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //Success Message
            if(flag == 1)
                Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();

        }
    }


}
