package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.starbug1.android.mudanews.data.More;
import com.starbug1.android.mudanews.data.NewsListItem;
import com.starbug1.android.mudanews.R;

public class MudanewsActivity extends ListActivity {
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	private int page_ = 0;
	
//	private final ServiceReceiver receiver_ = new ServiceReceiver();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        page_ = 0;
        items_ = new ArrayList<NewsListItem>();
        adapter_ = new NewsListAdapter(this, items_);
        
        NewsParserTask task = new NewsParserTask(this, adapter_);
        task.execute(String.valueOf(page_));
        
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancelAll();
		
//        // サービスを開始
//        startService(new Intent(this, FetchFeedService.class));
//        IntentFilter filter = new IntentFilter(FetchFeedService.ACTION);
//        registerReceiver(receiver_, filter);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        NewsListItem item = items_.get(position);
        if (item instanceof More) {
        	//read more
        	items_.remove(position);
        	page_++;
        	
        	int y = l.getScrollY();
            NewsParserTask task = new NewsParserTask(this, adapter_);
            task.execute(String.valueOf(page_), String.valueOf(y));
        } else {
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra("link", item.getLink());
            startActivity(intent);
        }
    }
}