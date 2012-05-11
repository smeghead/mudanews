package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;

import me.parappa.sdk.PaRappa;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.starbug1.android.mudanews.data.DatabaseHelper;
import com.starbug1.android.mudanews.data.NewsListItem;
import com.starbug1.android.mudanews.utils.AppUtils;

public class MudanewsActivity extends Activity {
	private static final String TAG = "MudanewsActivity";
	
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	private int page_ = 0;
	private ProgressDialog progressDialog_;
	private DatabaseHelper dbHelper_ = null;
	public boolean hasNextPage = true;
	public boolean gridUpdating = false;
	private PaRappa parappa_;

	private FetchFeedService fetchFeedService_;
	private boolean isBound_;
	final Handler handler_ = new Handler();

	private ServiceConnection connection_ = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
			fetchFeedService_ = ((FetchFeedService.FetchFeedServiceLocalBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			fetchFeedService_ = null;
		}
	};

	void doBindService() {
		bindService(new Intent(MudanewsActivity.this, FetchFeedService.class),
				connection_, Context.BIND_AUTO_CREATE);
		isBound_ = true;
	}

	void doUnbindService() {
		if (isBound_) {
			unbindService(connection_);
			isBound_ = false;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d(TAG, "setContentView");
		
		dbHelper_ = new DatabaseHelper(this);

		doBindService();
		Log.d(TAG, "bindService");

		page_ = 0; hasNextPage = true;
		items_ = new ArrayList<NewsListItem>();
		adapter_ = new NewsListAdapter(this, items_);

		String versionName = AppUtils.getVersionName(this);
		TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);

		final GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				NewsListItem item = items_.get(position);

				final SQLiteDatabase db = dbHelper_.getWritableDatabase();
				db.execSQL(
						"insert into view_logs (feed_id, created_at) values (?, current_timestamp)",
						new String[] { String.valueOf(item.getId()) });
				db.close();

				item.setViewCount(item.getViewCount() + 1);
				ImageView newIcon = (ImageView) view
						.findViewById(R.id.newEntry);
				newIcon.setVisibility(ImageView.GONE);

				Intent entryIntent = new Intent(MudanewsActivity.this, EntryActivity.class);
				entryIntent.putExtra("item", item);
				MudanewsActivity.this.startActivity(entryIntent);
				
			}
		});

		grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			
			public boolean onItemLongClick(AdapterView<?> arg0, View viewArg,
					int position, long arg3) {
				final View v = viewArg;
				final NewsListItem item = items_.get(position);
//			    Integer item_index = (Integer)v.getTag() - 1;
				AlertDialog.Builder ad = new AlertDialog.Builder(MudanewsActivity.this);
				ad.setTitle("記事のアクション");
				ad.setItems(R.array.arrays_entry_actions, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Log.d("NewsListAdapter", "longclickmenu selected id:" + item.getId());
						String processName = MudanewsActivity.this.getResources().getStringArray(R.array.arrays_entry_action_values)[which];
						final SQLiteDatabase db = dbHelper_.getWritableDatabase();
						try {
							if ("share".equals(processName)) {
								//共有
								parappa_.shareString(item.getTitle() + " " + item.getLink() + " #" + getResources().getString(R.string.app_name), "共有");
							} else if ("make_favorite".equals(processName)) {
								//お気に入り
								db.execSQL(
										"insert into favorites (feed_id, created_at) values (?, current_timestamp)",
										new String[] { String.valueOf(item.getId()) });
								item.setFavorite(true);
								ImageView favorite = (ImageView) v
										.findViewById(R.id.favorite);
								favorite.setImageResource(android.R.drawable.btn_star_big_on);
								Toast.makeText(MudanewsActivity.this, item.getTitle() + "をお気に入りにしました", Toast.LENGTH_LONG).show();
							} else if ("make_read".equals(processName)) {
								//既読にする
								db.execSQL(
										"insert into view_logs (feed_id, created_at) values (?, current_timestamp)",
										new String[] { String.valueOf(item.getId()) });
								item.setViewCount(item.getViewCount() + 1);
								ImageView newIcon = (ImageView) v
										.findViewById(R.id.newEntry);
								newIcon.setVisibility(ImageView.GONE);
								Toast.makeText(MudanewsActivity.this, item.getTitle() + "を既読にしました", Toast.LENGTH_LONG).show();
							} else if ("delete".equals(processName)) {
								//削除
								db.execSQL(
										"update feeds set deleted = 1 where id = ?",
										new String[] { String.valueOf(item.getId()) });
								items_.remove(item);
								page_ = 0; hasNextPage = true;
								Toast.makeText(MudanewsActivity.this, item.getTitle() + "を削除しました", Toast.LENGTH_LONG).show();
								updateList(page_);
							}
						} catch (Exception e) {
							Log.e(TAG, "failed to upate entry action.");
						} finally {
							db.close();
						}
					}
				});
				AlertDialog alert = ad.create();
				alert.show();
				return true;
			}
		});
		Log.d(TAG, "grid setup");

		grid.setOnScrollListener(new OnScrollListener() {
			private boolean stayBottom = false;

			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
				// スクロールしていない
				case OnScrollListener.SCROLL_STATE_IDLE:
				case OnScrollListener.SCROLL_STATE_FLING:
					if (stayBottom) {
						Log.d(TAG, "scrollY: " + grid.getHeight());
						// load more.
						
						if (!MudanewsActivity.this.gridUpdating && MudanewsActivity.this.hasNextPage) {
							updateList(++page_);
						}
					}
					break;
				}
			}

			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

				stayBottom = (totalItemCount == firstVisibleItem
						+ visibleItemCount);
			}
		});
		Log.d(TAG, "scroll");

		// 初回起動なら、feed取得 ボタンを表示する
		if (dbHelper_.entryIsEmpty()) {
			final TextView initialMessage = (TextView) this.findViewById(R.id.initialMessage);
			initialMessage.setVisibility(Button.VISIBLE);
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 10; i++) {
						try {
							Thread.sleep(500);
						} catch (Exception e) {}
						Log.d(TAG, "service:" + isBound_);
						if (isBound_) break;
					}
					handler_.post(new Runnable() {
						@Override
						public void run() {
							fetchFeeds();
						}
					});
				}
			}).start();
		}

		Log.d(TAG, "updateList start.");
		updateList(page_);
		Log.d(TAG, "updateList end.");

		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancelAll();
		parappa_ = new PaRappa(this);
	}

	private NewsParserTask task_ = null;

	public int column_count_ = 1;
	private void setupGridColumns() {
		WindowManager w = getWindowManager();
		Display d = w.getDefaultDisplay();
		int width = d.getWidth();
		column_count_ = width / 160;
		GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setNumColumns(column_count_);
	}

	private void updateList(int page) {
		setupGridColumns();

		task_ = new NewsParserTask(this, adapter_);
		task_.execute(String.valueOf(page));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_update_feeds:
			fetchFeeds();
			break;
		case R.id.menu_settings:
			settings();
			break;
		case R.id.menu_notify_all:
			shareAll();
			break;
		case R.id.menu_review:
			parappa_.gotoMarket();
			break;
		case R.id.menu_support:
			parappa_.startSupportActivity();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void shareAll() {
		parappa_.shareString(getResources().getString(R.string.shareDescription) + " #" + getResources().getString(R.string.app_name), "紹介");
	}
		
	private void settings() {
		Intent intent = new Intent(this, AppPrefActivity.class);
		startActivity(intent);
	}

	private void fetchFeeds() {
		items_.clear();
		progressDialog_ = new ProgressDialog(MudanewsActivity.this);
		progressDialog_.setMessage("読み込み中...");
		progressDialog_.show();
		new Thread() {
			@Override
			public void run() {
				final int count = fetchFeedService_.updateFeeds();
				handler_.post(new Runnable() {
					public void run() {
						TextView initialMessage = (TextView) findViewById(R.id.initialMessage);
						initialMessage.setVisibility(TextView.GONE);

						progressDialog_.dismiss();
						page_ = 0; hasNextPage = true;
						items_.clear();
						updateList(page_);
						if (count == 0) {
							Toast.makeText(MudanewsActivity.this, "新しい記事はありませんでした", Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(MudanewsActivity.this, count + "件の記事を追加しました", Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		}.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	}

	@Override
	protected void onPause() {
		if (task_ != null) {
			task_.progresCancel();
		}
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setupGridColumns();
	}

	public DatabaseHelper getDbHelper() {
		return dbHelper_;
	}
}
