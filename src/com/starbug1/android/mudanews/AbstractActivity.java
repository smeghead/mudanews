package com.starbug1.android.mudanews;

import com.starbug1.android.mudanews.data.DatabaseHelper;

import android.app.Activity;

public abstract class AbstractActivity extends Activity {
	public abstract DatabaseHelper getDbHelper();
	public abstract int getGridColumnCount();
}
