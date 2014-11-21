package com.revyuk.githublist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Vitaly Revyuk on 20.11.2014.
 */
public class DBPreferences extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "githublist";
    private static final int DATABASE_VERSION = 1;
    public static final String PREFERENCES_TABLE_NAME = "preferences";
    public static final String FAVORITES_TABLE_NAME = "favorites";

    public static final String FAVORITES_LOGIN_COLUMN =  "login";
    public static final String FAVORITES_AVATAR_URL_COLUMN =  "avatar_url";
    public static final String FAVORITES_USER_URL_COLUMN =  "user_url";

    public static final String PREFERENCES_KEY_COLUMN =  "pkey";
    public static final String PREFERENCES_VALUE_COLUMN =  "pvalue";

    public DBPreferences(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DBPreferences(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_PREFERENCES = "create table " + PREFERENCES_TABLE_NAME + " (_id integer primary key autoincrement, " +
                PREFERENCES_KEY_COLUMN + " text not null, " +
                PREFERENCES_VALUE_COLUMN + " text not null);";
        String CREATE_TABLE_FAVORITES = "create table " + FAVORITES_TABLE_NAME + " (_id integer primary key autoincrement, " +
                FAVORITES_LOGIN_COLUMN + " text not null, " +
                FAVORITES_AVATAR_URL_COLUMN + " text not null, " +
                FAVORITES_USER_URL_COLUMN + " text not null);\n";

        db.execSQL(CREATE_TABLE_PREFERENCES);
        db.execSQL(CREATE_TABLE_FAVORITES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("XXX", "Update database.");
        String DROP_TABLES = "drop table if it exist " + PREFERENCES_TABLE_NAME + ";\n" +
                "drop table if it exist " + FAVORITES_TABLE_NAME + ";";
        db.execSQL(DROP_TABLES);
        onCreate(db);
    }


}
