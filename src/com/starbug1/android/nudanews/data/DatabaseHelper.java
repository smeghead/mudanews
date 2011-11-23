package com.starbug1.android.nudanews.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(Context context) {
		super(context, "mudanews.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			db.execSQL(
				"create table feeds ( " + 
				"  id integer primary key," +
				"  source text not null," +
				"  title text not null," +
				"  link text not null," +
				"  description text not null," +
				"  category text," +
				"  published_at datetime," +
				"  created_at datetime not null" +
				")"
			);
						
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int currentVersion, int newVersion) {
		//debug
	}

}
