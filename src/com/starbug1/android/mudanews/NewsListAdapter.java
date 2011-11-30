/**
 * 
 */
package com.starbug1.android.mudanews;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
		
		NewsListItem item = this.getItem(position);
		if (item != null) {
			String title = item.getTitle().toString();
			title_ = (TextView)view.findViewById(R.id.item_title);
			title_.setText(title);
			
//			String description = item.getDescription().toString();
//			description_ = (TextView)view.findViewById(R.id.item_descr);
//			description_.setText(description);
		}
		return view;
	}

}
