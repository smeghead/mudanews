package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.starbug1.android.newsapp.FetchFeedService;
import com.starbug1.android.newsapp.data.NewsListItem;

public class AppFetchFeedService extends FetchFeedService {

	private final Pattern imageUrl_ = Pattern.compile(
			"<img.*?src=\"([^\"]*)\"", Pattern.MULTILINE);
	private final Pattern imageOriginalUrl_ = Pattern.compile(
			"<img.*?data-original=\"([^\"]*)\"", Pattern.MULTILINE);
	private final Pattern gigagineContent_ = Pattern.compile(
			"class=\"preface\"(.*)$", Pattern.DOTALL);
	private final Pattern itaContent_ = Pattern.compile(
			"class=\"main entry-content\"(.*)name=\"more\"", Pattern.DOTALL);

	@Override
	protected List<Feed> getFeeds() {
		List<Feed> feeds = new ArrayList<Feed>();
		feeds.add(new Feed("らばQ", "http://labaq.com/index.rdf") {

			@Override
			public String getImageUrl(String content, NewsListItem item) {
				return null;
			}

		});
		feeds.add(new Feed("痛いニュース",
				"http://blog.livedoor.jp/dqnplus/index.rdf") {
			@Override
			public String getImageUrl(String content, NewsListItem item) {
				Matcher m = itaContent_.matcher(content);
				if (!m.find()) {
					return null;
				}
				String mainPart = m.group(1);
				m = imageUrl_.matcher(mainPart);
				if (!m.find()) {
					return null;
				}
				String imageUrl = m.group(1);
				return imageUrl;
			}

		});
		feeds.add(new Feed("GIGAZINE", "http://gigazine.net/news/rss_2.0/") {

			@Override
			public String getImageUrl(String content, NewsListItem item) {
				Matcher m = gigagineContent_.matcher(content);
				if (!m.find()) {
					return null;
				}
				String mainPart = m.group(1);
				m = imageUrl_.matcher(mainPart);
				if (!m.find()) {
					return null;
				}
				String imageUrl = m.group(1);
				if (imageUrl.equals("http://gigazine.jp/images/1x3.png")) {
					// gigazine で、メイン画像の遅延ロードをするようになったので、元の画像を取得するようにして対応する。
					m = imageOriginalUrl_.matcher(mainPart);
					if (m.find()) {
						imageUrl = m.group(1);
					}
				}
				return imageUrl;
			}

		});

		return feeds;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}
}
