package com.starbug1.android.nudanews.data;

/**
 * 記事一覧に表示するリストビュー
 * 
 * @author smeghead
 */
public class NewsListItem {
	private CharSequence title_;
	private CharSequence description_;
	
	public NewsListItem() {
		title_ = "";
		description_ = "";
	}

	public CharSequence getTitle() {
		return title_;
	}

	public void setTitle(CharSequence title) {
		title_ = title;
	}

	public CharSequence getDescription() {
		return description_;
	}

	public void setDescription(CharSequence description) {
		description_ = description;
	}
	
}
