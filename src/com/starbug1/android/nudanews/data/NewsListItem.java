package com.starbug1.android.nudanews.data;

/**
 * 記事一覧に表示するリストビュー
 * 
 * @author smeghead
 */
public class NewsListItem {
	private String title_;
	private String description_;
	private String category_;
	private String publishedAt_;
	private String link_;
	private String source_;
	
	public NewsListItem() {
		title_ = "";
		description_ = "";
		category_ = "";
		publishedAt_ = "";
		link_ = "";
	}

	public String getTitle() {
		return title_;
	}

	public void setTitle(String title) {
		title_ = title;
	}

	public String getDescription() {
		return description_;
	}

	public void setDescription(String description) {
		description_ = description;
	}

	public String getCategory() {
		return category_;
	}

	public void setCategory(String category) {
		category_ = category;
	}

	public String getPublishedAt() {
		return publishedAt_;
	}

	public void setPublishedAt(String publishedAt) {
		publishedAt_ = publishedAt;
	}

	public String getLink() {
		return link_;
	}

	public void setLink(String link) {
		link_ = link;
	}

	public String getSource() {
		return source_;
	}

	public void setSource(String source) {
		source_ = source;
	}

	
}
