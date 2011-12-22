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
import android.widget.ImageView;
import android.widget.TextView;

import com.starbug1.android.mudanews.data.NewsListItem;

/**
 * @author smeghead
 * 
 */
public class NewsListAdapter extends ArrayAdapter<NewsListItem> {
	private LayoutInflater inflater_;
	private TextView title_;

	public NewsListAdapter(Context context, List<NewsListItem> objects) {
		super(context, 0, objects);
		inflater_ = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			view = inflater_.inflate(R.layout.item_row, null);
		}

		Log.d("NewsListAdapter", "position: " + position);
		if (this.getCount() < position + 1) {
			Log.w("NewsListAdapter", "position invalid!");
			return null;
		}
		NewsListItem item = this.getItem(position);
		if (item != null) {
			view.setTag(item);
			
			String title = item.getTitle().toString();
			title_ = (TextView) view.findViewById(R.id.item_title);
			title_.setText(title);
			ImageView newEntry = (ImageView) view.findViewById(R.id.newEntry);
			if (item.getViewCount() > 0) {
				newEntry.setVisibility(ImageView.GONE);
			} else {
				newEntry.setVisibility(ImageView.VISIBLE);
			}

			if (item.getImage() != null) {
				Bitmap b = item.getImageBitmap();
				if (b == null) {
					byte[] data = item.getImage();
					Log.d("NewsListAdapter", "data.length:" + data.length);
					b = BitmapFactory.decodeByteArray(data, 0, data.length);
					item.setImageBitmap(b);
				}
				ImageView image = (ImageView) view
						.findViewById(R.id.item_image);
				image.setImageDrawable(null);
				image.setImageBitmap(b);
				image.setVisibility(ImageView.VISIBLE);
			} else {
				Log.d("NewsListAdapter", "item more? id:" + item.getId());
				ImageView image = (ImageView) view
						.findViewById(R.id.item_image);
				image.setVisibility(ImageView.GONE);
			}
		}
		return view;
	}

	@Override
	public void remove(NewsListItem object) {
		Log.d("NewsListAdapter", "remove");
		super.remove(object);
	}

}
