package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;

import com.starbug1.android.mudanews.data.More;
import com.starbug1.android.mudanews.data.NewsListItem;
import com.starbug1.android.mudanews.utils.FileDownloader;

public class MudanewsActivity extends Activity {
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	private int page_ = 0;
	
//	private final ServiceReceiver receiver_ = new ServiceReceiver();
	private FetchFeedService fetchFeedService_;
	private boolean isBound_;
	final Handler handler_ = new Handler();
	private ProgressDialog progresDialog_;

	private ServiceConnection connection_ = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
	        fetchFeedService_ = ((FetchFeedService.FetchFeedServiceLocalBinder)service).getService();
	    }
	 
	    public void onServiceDisconnected(ComponentName className) {
	        fetchFeedService_ = null;
//	        Toast.makeText(MudanewsActivity.this, "Activity:onServiceDisconnected",
//	                Toast.LENGTH_SHORT).show();
	    }
	};
	void doBindService() {
	    bindService(new Intent(MudanewsActivity.this,
	            FetchFeedService.class), connection_, Context.BIND_AUTO_CREATE);
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

		if (items_ != null) {
			updateList();
			return;
		}

        doBindService();
        
        page_ = 0;
        items_ = new ArrayList<NewsListItem>();
        adapter_ = new NewsListAdapter(this, items_);
        GridView view = (GridView) this.findViewById(R.id.grid);
        view.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapter, View view, int position,
					long id) {
		        NewsListItem item = items_.get(position);
		        if (item instanceof More) {
		        	//read more
		        	items_.remove(position);
		        	page_++;
		        	
		            updateList();
		        } else {
		            Intent intent = new Intent(MudanewsActivity.this, NewsDetailActivity.class);
		            intent.putExtra("link", item.getLink());
		            startActivity(intent);
		        }
			}
        });
        updateList();
        
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancelAll();		
    }

    private void updateList() {
        NewsParserTask task = new NewsParserTask(this, adapter_);
        task.execute(String.valueOf(page_));
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
			items_.clear();
			handler_.post(new Runnable() {
				public void run() {
					progresDialog_ = new ProgressDialog(MudanewsActivity.this);
					progresDialog_.setMessage("読み込み中...");
					progresDialog_.show();
				}
			});
			new Thread() {
				@Override
				public void run() {
					handler_.post(new Runnable() {
						public void run() {
							fetchFeedService_.updateFeeds();
							progresDialog_.dismiss();
							updateList();
						}
					});
				}
			}.start();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	}

}