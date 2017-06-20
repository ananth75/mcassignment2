package com.example.ananth.Group5;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import static android.content.ContentValues.TAG;
import static java.security.AccessController.getContext;


/**
 * Created by kisho on 18-06-2017.
 */

public class DataBaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME = "Patient.db";
    private static final String ACCELEROMETER_TABLE = "kishore";
    private static final String TIME_STAMP = "timestamp";
    private static final String X_VALUE = "x";
    private static final String Y_VALUE = "y";
    private static final String Z_VALUE = "z";

    DataBaseOpenHelper(Context context/*,String tableName*/){

        super(context, DB_NAME, null, DATABASE_VERSION);
        //Toast.makeText(getApplicationContext(), "Please enter all the patient's details", Toast.LENGTH_SHORT).show();
        //ACCELEROMETER_TABLE = tableName;

    }



    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
