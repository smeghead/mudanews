/**
 * 
 */
package com.starbug1.android.nudanews;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
	private ProgressDialog progresDialog_;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.news_detail);
		
		Intent intent = getIntent();
		String url = intent.getStringExtra("link");
		Log.d("NewsDetailActivity", "url: " + url);
		view_ = (WebView) findViewById(R.id.webView1);
		view_.setWebViewClient(new WebViewClient(){

			@Override
			public void onPageFinished(WebView view, String url) {
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (progresDialog_ != null) {
					progresDialog_.dismiss();
				}
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.d("NewsDetailActivity", "url: " + url);
				return super.shouldOverrideUrlLoading(view, url);
			}
			
		});
		view_.getSettings().setJavaScriptEnabled(true);
		view_.loadUrl(mobileUrl(url));

		progresDialog_ = new ProgressDialog(this);
		progresDialog_.setMessage("読み込み中...");
		progresDialog_.show();

	}

	private String mobileUrl(String url) {
		String ret = url;
		ret = ret.replaceAll("/dqnplus/", "/dqnplus/lite/");
		ret = ret.replaceAll("/labaq.com/", "/labaq.com/lite/");
		return ret;
	}
}
