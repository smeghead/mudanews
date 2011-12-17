package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.starbug1.android.mudanews.data.DatabaseHelper;
import com.starbug1.android.mudanews.data.NewsListItem;
import com.starbug1.android.mudanews.utils.UrlUtils;

public class MudanewsActivity extends Activity {
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	private int page_ = 0;
	private ViewFlipper flipper_;
	private Animation anim_left_in_, anim_left_out_, anim_right_in_,
			anim_right_out_;
	private ProgressDialog progressDialog_;
	DatabaseHelper dbHelper_ = null;

	private void setupAnim() {
		anim_left_in_ = AnimationUtils.loadAnimation(MudanewsActivity.this,
				R.animator.left_in);
		anim_left_out_ = AnimationUtils.loadAnimation(MudanewsActivity.this,
				R.animator.left_out);
		anim_right_in_ = AnimationUtils.loadAnimation(MudanewsActivity.this,
				R.animator.right_in);
		anim_right_out_ = AnimationUtils.loadAnimation(MudanewsActivity.this,
				R.animator.right_out);
	}

	private FetchFeedService fetchFeedService_;
	private boolean isBound_;
	final Handler handler_ = new Handler();
	private boolean isViewingEntry_ = false;

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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		dbHelper_ = new DatabaseHelper(MudanewsActivity.this);
		setupAnim();
		flipper_ = (ViewFlipper) this.findViewById(R.id.flipper);

		doBindService();

		page_ = 0;
		items_ = new ArrayList<NewsListItem>();
		adapter_ = new NewsListAdapter(this, items_);

		String versionName = getVersionName();
		TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);
		TextView version2 = (TextView) this.findViewById(R.id.version2);
		version2.setText(versionName);

		GridView view = (GridView) this.findViewById(R.id.grid);
		view.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				NewsListItem item = items_.get(position);

				final SQLiteDatabase db = dbHelper_.getWritableDatabase();
				db.execSQL(
						"insert into view_logs (feed_id, created_at) values (?, current_timestamp)",
						new String[] { String.valueOf(item.getId()) });
				db.close();

				flipper_.setInAnimation(anim_right_in_);
				flipper_.setOutAnimation(anim_left_out_);
				flipper_.showNext();
				isViewingEntry_ = true;
				item.setViewCount(item.getViewCount() + 1);
				ImageView newIcon = (ImageView) view
						.findViewById(R.id.newEntry);
				newIcon.setVisibility(ImageView.GONE);

				WebView entryView = (WebView) MudanewsActivity.this
						.findViewById(R.id.entryView);
				entryView.setWebViewClient(new WebViewClient() {

					@Override
					public void onPageStarted(WebView view, String url,
							Bitmap favicon) {
						final WebView v = view;
						Log.d("NewsDetailActivity", "onPageStarted url: " + url);
						final Timer timer = new Timer();
						timer.schedule(new TimerTask() {
							@Override
							public void run() {
								Log.i("MudanewsActivity", "timer taks");
								if (v.getContentHeight() > 0) {
									handler_.post(new Runnable() {
										public void run() {
											if (progressDialog_ != null) {
												progressDialog_.dismiss();
											}
										}
									});
									timer.cancel();
								}
							}
						}, 1000, 1000);
					}

					@Override
					public boolean shouldOverrideUrlLoading(WebView view,
							String url) {
						if (!UrlUtils.isSameDomain(view.getOriginalUrl(), url)) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(url));
							startActivity(intent);
							return true;
						}
						Log.d("NewsDetailActivity",
								"shouldOverrideUrlLoading url: " + url);
						return super.shouldOverrideUrlLoading(view, url);
					}
				});
				entryView.getSettings().setJavaScriptEnabled(true);
				entryView.loadUrl(UrlUtils.mobileUrl(item.getLink()));
				progressDialog_ = new ProgressDialog(MudanewsActivity.this);
				progressDialog_.setMessage("読み込み中...");
				progressDialog_.show();
			}
		});
		
		final Button more = (Button) this.findViewById(R.id.more);
		more.setVisibility(Button.GONE);
		more.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				page_++;
				updateList(page_);
			}
		});

		view.setOnScrollListener(new OnScrollListener() {
			private boolean stayBottom = false;

			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (!Boolean.parseBoolean(more.getTag().toString())) {
					return;
				}
				switch (scrollState) {
				// スクロールしていない
				case OnScrollListener.SCROLL_STATE_IDLE:
				case OnScrollListener.SCROLL_STATE_FLING:
					if (stayBottom) {
						more.setVisibility(Button.VISIBLE);
					} else {
						more.setVisibility(Button.GONE);
					}
					break;
				}
			}

			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				Log.d("MudanewsActivity", "onScrollState");
				stayBottom = (totalItemCount == firstVisibleItem
						+ visibleItemCount);
			}
		});

		// TODO 初回起動なら、feed取得 ボタンを表示する
		if (dbHelper_.entryIsEmpty()) {
			final Button fetchfeeds = (Button) this.findViewById(R.id.fetchfeeds);
			fetchfeeds.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					fetchFeeds();
					updateList(page_);
					fetchfeeds.setVisibility(Button.GONE);
				}
			});
			fetchfeeds.setVisibility(Button.VISIBLE);
		}

		Log.d("MudanewsActivity", "updateList start.");
		updateList(page_);
		Log.d("MudanewsActivity", "updateList end.");

		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancelAll();
	}

	private NewsParserTask task_ = null;

	private void setupGridColumns() {
		WindowManager w = getWindowManager();
		Display d = w.getDefaultDisplay();
		int width = d.getWidth();
		int column_count = width / 160;
		GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setNumColumns(column_count);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.update_feeds:
			fetchFeeds();
			break;
		case R.id.settings:
			settings();
			break;
		}
		return super.onOptionsItemSelected(item);
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
				fetchFeedService_.updateFeeds();
				handler_.post(new Runnable() {
					public void run() {
						progressDialog_.dismiss();
						page_ = 0;
						items_.clear();
						updateList(page_);
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

	private String getVersionName() {
		PackageInfo packageInfo = null;
		try {
			packageInfo = getPackageManager().getPackageInfo(
					this.getClass().getPackage().getName(),
					PackageManager.GET_META_DATA);
			return "Version " + packageInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e("NudaNewsActivity", "failed to retreive version info.");
		}
		return "";
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (e.getAction() == KeyEvent.ACTION_DOWN) {
				if (isViewingEntry_) {
					flipper_.setInAnimation(anim_left_in_);
					flipper_.setOutAnimation(anim_right_out_);
					isViewingEntry_ = false;
					flipper_.showPrevious();
					WebView entryView = (WebView) MudanewsActivity.this
							.findViewById(R.id.entryView);
					entryView.clearView();
					entryView.loadData("", "text/plain", "UTF-8");
					return false;
				}
			}
		}
		return super.dispatchKeyEvent(e);
	}
}