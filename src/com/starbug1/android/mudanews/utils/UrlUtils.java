package com.starbug1.android.mudanews.utils;

public class UrlUtils {
	public static String mobileUrl(String url) {
		String ret = url;
		ret = ret.replaceAll("/dqnplus/", "/dqnplus/lite/");
		ret = ret.replaceAll("/labaq.com/", "/labaq.com/lite/");
		return ret;
	}

}
