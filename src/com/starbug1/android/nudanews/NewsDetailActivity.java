/**
 * 
 */
package com.starbug1.android.nudanews;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * @author smeghead
 *
 */
public class NewsDetailActivity extends Activity {
	WebView view_ = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.news_detail);
		
		Intent intent = getIntent();
		String url = intent.getStringExtra("link");
		Log.d("NewsDetailActivity", "url: " + url);
		view_ = (WebView) findViewById(R.id.webView1);
		view_.setWebViewClient(new WebViewClient());
		view_.loadUrl(url);
	}

}
