/**
 * 
 */
package com.starbug1.android.mudanews;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.starbug1.android.mudanews.data.NewsListItem;
import com.starbug1.android.mudanews.R;

/**
 * @author smeghead
 *
 */
public class NewsListAdapter extends ArrayAdapter<NewsListItem> {
	private LayoutInflater inflater_;
	private TextView title_;
	private TextView description_;

	public NewsListAdapter(Context context, List<NewsListItem> objects) {
		super(context, 0, objects);
		
		inflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if (view == null) {
			view = inflater_.inflate(R.layout.item_row, null);
		}
		
		Log.d("NewsListAdapter", "position: " + position);
		NewsListItem item = this.getItem(position);
		if (item != null) {
			String title = item.getTitle().toString();
			title_ = (TextView)view.findViewById(R.id.item_title);
			title_.setText(title);
			view.setTag(item);

			if (item.getImage() != null) {
				byte[] data = item.getImage();
				Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
				ImageView image = (ImageView) view.findViewById(R.id.item_image);
				image.setImageBitmap(b);
			} else {
				ImageView image = (ImageView) view.findViewById(R.id.item_image);
				image.setVisibility(ImageView.GONE);
			}
		}
		
		return view;
	}

}
