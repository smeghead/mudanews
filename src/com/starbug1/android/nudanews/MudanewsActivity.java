package com.starbug1.android.nudanews;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.starbug1.android.nudanews.data.NewsListItem;

public class MudanewsActivity extends ListActivity {
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	
	private final ServiceReceiver receiver_ = new ServiceReceiver();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        items_ = new ArrayList<NewsListItem>();
        adapter_ = new NewsListAdapter(this, items_);
        
        NewsParserTask task = new NewsParserTask(this, adapter_);
        task.execute("http://labaq.com/index.rdf");
        
//        // サービスを開始
//        startService(new Intent(this, FetchFeedService.class));
//        IntentFilter filter = new IntentFilter(FetchFeedService.ACTION);
//        registerReceiver(receiver_, filter);
    }
}