package com.xtremelabs.testactivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.xtreme.testactivity.R;
import com.xtremelabs.imageutils.ImageLoader;

@TargetApi(13)
public class KittenAdapter extends BaseAdapter {
	private static final String URL = "http://placekitten.com/500/250?a=";
	private Activity mActivity;
	private ImageLoader mImageLoader;

	public KittenAdapter(Activity activity, ImageLoader imageLoader) {
		mActivity = activity;
		mImageLoader = imageLoader;
	}

	@Override
	public int getCount() {
		return 10000;
	}

	@Override
	public Object getItem(int position) {
		return URL + position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@TargetApi(13)
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		KittenViews kittenViews = null;
		if (convertView == null) {
			convertView = View.inflate(mActivity, R.layout.image_row, null);

			kittenViews = new KittenViews();
			kittenViews.kitten1 = (ImageView) convertView.findViewById(R.id.kitten1);
			kittenViews.kitten2 = (ImageView) convertView.findViewById(R.id.kitten2);
			convertView.setTag(kittenViews);

			setParams(kittenViews);
		}

		if (kittenViews == null)
			kittenViews = (KittenViews) convertView.getTag();

		mImageLoader.loadImage(kittenViews.kitten1, (String) getItem(position) + "1");
		mImageLoader.loadImage(kittenViews.kitten2, (String) getItem(position) + "2");

		return convertView;
	}

	private class KittenViews {
		ImageView kitten1;
		ImageView kitten2;
	}

	private void setParams(KittenViews kittenViews) {
		ViewGroup.LayoutParams params1 = kittenViews.kitten1.getLayoutParams();
		ViewGroup.LayoutParams params2 = kittenViews.kitten2.getLayoutParams();

		Point size = new Point();
		mActivity.getWindowManager().getDefaultDisplay().getSize(size);

		params1.width = params2.width = size.x / 2;
		params1.height = params2.height = (int) ((size.x / 800f) * 200f);
	}
}
