package com.artech.android.notification;
 
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
 
public class LocalNotificationsSQLiteHelper extends SQLiteOpenHelper {
 
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "notificationsManager";
 
    // Notifications table name
    private static final String TABLE_NOTIFICATIONS = "notifications";
 
    // Notifications Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_DATE_TIME = "datetime";
    private static final String KEY_TEXT = "text";
 
    public LocalNotificationsSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_NOTIFICATIONS_TABLE = "CREATE TABLE " + TABLE_NOTIFICATIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_DATE_TIME + " TEXT,"
                + KEY_TEXT + " TEXT" + ")";
        db.execSQL(CREATE_NOTIFICATIONS_TABLE);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
 
        // Create tables again
        onCreate(db);
    }
 
    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */
 
    // Adding new Notification
    public long addNotification(Notification notification) {
    	SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        //values.put(KEY_ID, notification.getId()); // Notification Id
        values.put(KEY_DATE_TIME, notification.getDateTime()); // Notification DateTime
        values.put(KEY_TEXT, notification.getText()); // Notification Text
 
        // Inserting Row
        long row = db.insert(TABLE_NOTIFICATIONS, null, values);
        db.close(); // Closing database connection
        return row;
    }
 
    // Getting All Notifications
    public List<Notification> getAllNotifications() {
        List<Notification> notificationList = new ArrayList<Notification>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_NOTIFICATIONS;
 
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
            	Notification notification = new Notification();
                notification.setDateTime(cursor.getString(1));
                notification.setText(cursor.getString(2));
                // Adding contact to list
                notificationList.add(notification);
            } while (cursor.moveToNext());
        }
 
        // return Notification list
        db.close(); // Closing database connection
        return notificationList;
    }
 
    // Deleting single Notification
    public void deleteNotification(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTIFICATIONS, KEY_ID + " = ?", new String[] { String.valueOf(id) });
        db.close(); // Closing database connection
    }
 
    
    public void deleteAllNotifications() {
    	SQLiteDatabase db = this.getWritableDatabase();
    	db.delete(TABLE_NOTIFICATIONS,null, null);
		db.close(); // Closing database connection
    }
    
    
    public int getId(Notification notification){
    	int id = 0;
        SQLiteDatabase db = this.getWritableDatabase();
    
        String[] select = new String[] {KEY_ID};
        String[] args = new String[] {notification.getDateTime(),notification.getText()};
         
        Cursor cursor = db.query(TABLE_NOTIFICATIONS, select ,KEY_DATE_TIME +" =? AND " + KEY_TEXT + " =?", args, null, null, null);
           // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
            	id = cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        db.close(); // Closing database connection
		return id;
    }
 
}