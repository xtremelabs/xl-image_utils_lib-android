package com.xtremelabs.testactivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.xtreme.testactivity.R;
import com.xtremelabs.imageutils.ImageLoader;

@TargetApi(13)
public class KittenAdapter extends BaseAdapter {
	private static final String IMAGE_FILE_NAME = "kitteh.jpg";
	private static final String URL = "http://placekitten.com/500/250?a=";
	private final String KITTEN_URI;
	private final Activity mActivity;
	private final ImageLoader mImageLoader;

	public KittenAdapter(Activity activity, ImageLoader imageLoader) {
		KITTEN_URI = "file://" + activity.getCacheDir() + File.separator + IMAGE_FILE_NAME;

		mActivity = activity;
		mImageLoader = imageLoader;

		loadKittenToFile();
	}

	@Override
	public int getCount() {
		return 10000;
	}

	@Override
	public Object getItem(int position) {
		if (position % 2 == 0) {
			return URL + position;
		} else {
			return KITTEN_URI;
		}
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

		if (position % 2 == 0) {
			mImageLoader.loadImage(kittenViews.kitten1, (String) getItem(position) + "1");
			mImageLoader.loadImage(kittenViews.kitten2, (String) getItem(position) + "2");
		} else {
			mImageLoader.loadImage(kittenViews.kitten1, (String) getItem(position));
			mImageLoader.loadImage(kittenViews.kitten2, (String) getItem(position));
		}

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

	private void loadKittenToFile() {
		StrictMode.setThreadPolicy(ThreadPolicy.LAX);
		try {
			URI uri = new URI(KITTEN_URI);
			final File imageFile = new File(uri.getPath());
			final FileOutputStream fos = new FileOutputStream(imageFile);
			Bitmap bitmap = ((BitmapDrawable) mActivity.getResources().getDrawable(R.drawable.kitteh)).getBitmap();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException("Could not find kitteh.");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Poorly named kitteh.");
		}
	}
}
