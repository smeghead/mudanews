package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
import android.webkit.WebSettings;
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
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.starbug1.android.mudanews.data.DatabaseHelper;
import com.starbug1.android.mudanews.data.NewsListItem;
import com.starbug1.android.mudanews.utils.AppUtils;
import com.starbug1.android.mudanews.utils.UrlUtils;

public class MudanewsActivity extends Activity {
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	private int page_ = 0;
	private ViewFlipper flipper_;
	private Animation anim_left_in_, anim_left_out_, anim_right_in_,
			anim_right_out_;
	private ProgressDialog progressDialog_;
	private DatabaseHelper dbHelper_ = null;
	private NewsListItem currentItem_ = null;


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
		Log.d("MudanewsActivity", "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d("MudanewsActivity", "setContentView");
		
		dbHelper_ = new DatabaseHelper(MudanewsActivity.this);
		setupAnim();
		flipper_ = (ViewFlipper) this.findViewById(R.id.flipper);
		Log.d("MudanewsActivity", "setupAnim");

		doBindService();
		Log.d("MudanewsActivity", "bindService");

		page_ = 0;
		items_ = new ArrayList<NewsListItem>();
		adapter_ = new NewsListAdapter(this, items_);

		String versionName = AppUtils.getVersionName(this);
		TextView version = (TextView) this.findViewById(R.id.version);
		version.setText(versionName);
		TextView version2 = (TextView) this.findViewById(R.id.version2);
		version2.setText(versionName);

		GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				NewsListItem item = items_.get(position);
				currentItem_ = item;

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
						if (!url.startsWith("file") && !UrlUtils.isSameDomain(view.getOriginalUrl(), url)) {
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
				WebSettings ws = entryView.getSettings();
				ws.setBuiltInZoomControls(true);
				ws.setLoadWithOverviewMode(true);
				ws.setPluginsEnabled(true);
				ws.setUseWideViewPort(true);
				ws.setJavaScriptEnabled(true);
				ws.setAppCacheMaxSize(1024 * 1024 * 64); //64MB
				ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
				ws.setDomStorageEnabled(true);
				ws.setAppCacheEnabled(true);
				entryView.setVerticalScrollbarOverlay(true);
				entryView.loadUrl(UrlUtils.mobileUrl(item.getLink()));
				progressDialog_ = new ProgressDialog(MudanewsActivity.this);
				progressDialog_.setMessage("読み込み中...");
				progressDialog_.show();
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
							if ("make_favorite".equals(processName)) {
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
								page_ = 0;
								Toast.makeText(MudanewsActivity.this, item.getTitle() + "を削除しました", Toast.LENGTH_LONG).show();
								updateList(page_);
							}
						} catch (Exception e) {
							Log.e("MudanewsActivity", "failed to upate entry action.");
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
		Log.d("MudanewsActivity", "grid setup");

		final Button more = (Button) this.findViewById(R.id.more);
		more.setVisibility(Button.GONE);
		more.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				page_++;
				updateList(page_);
			}
		});
		Log.d("MudanewsActivity", "more button");

		grid.setOnScrollListener(new OnScrollListener() {
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
		Log.d("MudanewsActivity", "scroll");

		// 初回起動なら、feed取得 ボタンを表示する
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
		if (isViewingEntry_) {
			menu.findItem(R.id.menu_update_feeds).setVisible(false);
			menu.findItem(R.id.menu_share).setVisible(true);
			menu.findItem(R.id.menu_settings).setVisible(false);
			menu.findItem(R.id.menu_favorite).setVisible(true);
		} else {
			menu.findItem(R.id.menu_share).setVisible(false);
			menu.findItem(R.id.menu_update_feeds).setVisible(true);
			menu.findItem(R.id.menu_settings).setVisible(true);
			menu.findItem(R.id.menu_favorite).setVisible(false);
		}
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
		case R.id.menu_share:
			share();
			break;
		case R.id.menu_notify_all:
			shareAll();
			break;
		case R.id.menu_review:
			gotoMarket();
			break;
		case R.id.menu_favorite:
			favorite();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void favorite() {
		if (currentItem_ == null) return;
		Log.d("MudanewsActivity", "favorite id:" + currentItem_.getId());
		
		final SQLiteDatabase db = dbHelper_.getWritableDatabase();
		try {
			// お気に入り
			db.execSQL(
					"insert into favorites (feed_id, created_at) values (?, current_timestamp)",
					new String[] { String.valueOf(currentItem_.getId()) });
			currentItem_.setFavorite(true);
			Toast.makeText(MudanewsActivity.this, currentItem_.getTitle() + "をお気に入りにしました", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e("MudanewsActivity", "failed to favorite.");
		} finally {
			if (db != null && db.isOpen())
				db.close();
		}
	}
	
	private void share() {
		if (currentItem_ == null) {
			return;
		}
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
	    intent.setType("text/plain");
	    intent.putExtra(Intent.EXTRA_TEXT, currentItem_.getTitle() + " " + currentItem_.getLink() + " #無駄新聞");
	    startActivity(Intent.createChooser(intent, "共有"));
	}
	
	private void shareAll() {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
	    intent.setType("text/plain");
	    intent.putExtra(Intent.EXTRA_TEXT, "一番カジュアルなAndroid用 新聞アプリ\n http://goo.gl/c0j2H #無駄新聞");
	    startActivity(Intent.createChooser(intent, "紹介"));
	}
	
	private void gotoMarket() {
		Intent intent = new Intent(
				android.content.Intent.ACTION_VIEW,
				Uri.parse("market://details?id=com.starbug1.android.mudanews"));
	    startActivity(Intent.createChooser(intent, "紹介"));
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
						progressDialog_.dismiss();
						page_ = 0;
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

	public DatabaseHelper getDbHelper() {
		return dbHelper_;
	}
}