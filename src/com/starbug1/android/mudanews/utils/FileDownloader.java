package com.starbug1.android.mudanews.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Environment;
import android.util.Log;

import com.starbug1.android.mudanews.AppException;

public class FileDownloader {
	public static String download(String uri) throws AppException {
		try {
			int responseCode = 0;
			int BUFFER_SIZE = 10240;

			URI url = new URI(uri);

			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet();

			client.getParams().setParameter("http.connection.timeout",
					new Integer(15000));
			get.setURI(url);
			HttpResponse res = client.execute(get);

			responseCode = res.getStatusLine().getStatusCode();

			String fileName = "";
			File SDDirectory = Environment.getExternalStorageDirectory();
			
			if (responseCode == HttpStatus.SC_OK) {
				fileName = SDDirectory.getAbsolutePath() + File.separator + String.valueOf(uri.hashCode());
				Log.d("FileDownloader", "filename:" + fileName);
				File file = new File(fileName);
				InputStream is = res.getEntity().getContent();
				BufferedInputStream in = new BufferedInputStream(is, BUFFER_SIZE);
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(file, false), BUFFER_SIZE);

				byte buf[] = new byte[BUFFER_SIZE];
				int size = -1;
				while ((size = in.read(buf)) != -1) {
					out.write(buf, 0, size);
				}
				out.flush();

				out.close();
				in.close();
			} else if (responseCode == HttpStatus.SC_NOT_FOUND) {
				throw new AppException("NOT FOUND");
			} else if (responseCode == HttpStatus.SC_REQUEST_TIMEOUT) {
				throw new AppException("TIMEOUT");
			}
			return fileName;
		} catch (Exception e) {
			Log.e("FileDownloader", "failed to download." + e.getMessage());
			throw new AppException("failed to download.", e);
		}
	}
}
