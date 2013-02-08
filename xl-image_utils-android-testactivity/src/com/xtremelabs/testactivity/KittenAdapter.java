/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.testactivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.xtreme.testactivity.R;
import com.xtremelabs.imageutils.Dimensions;
import com.xtremelabs.imageutils.ImageLoader;
import com.xtremelabs.imageutils.ImageLoaderListener;
import com.xtremelabs.imageutils.ImagePrecacheAssistant;
import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheInformationProvider;
import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheRequest;
import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheRequest.UnitType;
import com.xtremelabs.imageutils.ImageReturnedFrom;

@TargetApi(13)
public class KittenAdapter extends BaseAdapter {
	private static final String IMAGE_FILE_NAME = "kitteh.jpg";
	private static final String URL = "http://placekitten.com/500/250?a=";
	private final String KITTEN_URI;
	private final Activity mActivity;
	private final ImageLoader mImageLoader;
	private ImagePrecacheAssistant mImagePrecacheAssistant;
	private final Dimensions mBounds;

	public KittenAdapter(final Activity activity, ImageLoader imageLoader) {
		KITTEN_URI = "file://" + activity.getCacheDir() + File.separator + IMAGE_FILE_NAME;

		mActivity = activity;
		mImageLoader = imageLoader;

		loadKittenToFile();

		Point size = new Point();
		mActivity.getWindowManager().getDefaultDisplay().getSize(size);
		mBounds = new Dimensions(size.x / 2, (int) ((size.x / 800f) * 200f));

		mImagePrecacheAssistant = new ImagePrecacheAssistant(mImageLoader, new PrecacheInformationProvider() {
			@Override
			public int getCount() {
				return KittenAdapter.this.getCount();
			}

			@Override
			public List<PrecacheRequest> getImageRequestsForMemoryPrecache(int position) {
				List<PrecacheRequest> list = new ArrayList<PrecacheRequest>();
				if (position % 2 == 0) {
					list.add(PrecacheRequest.generatePrecacheRequest(activity, (String) getItem(position) + "1", mBounds, UnitType.PIXELS));
					list.add(PrecacheRequest.generatePrecacheRequest(activity, (String) getItem(position) + "2", mBounds, UnitType.PIXELS));
				} else {
					list.add(PrecacheRequest.generatePrecacheRequest(activity, (String) getItem(position), mBounds, UnitType.PIXELS));
				}
				return list;
			}

			@Override
			public List<String> getRequestsForDiskPrecache(int position) {
				List<String> list = new ArrayList<String>();
				if (position % 2 == 0) {
					list.add((String) getItem(position) + "1");
					list.add((String) getItem(position) + "2");
				} else {
					list.add((String) getItem(position));
				}
				return list;
			}
		});

		mImagePrecacheAssistant.setMemCacheRange(6);
		mImagePrecacheAssistant.setDiskCacheRange(5);
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
		mImagePrecacheAssistant.onPositionVisited(position);

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
			mImageLoader.loadImage(kittenViews.kitten1, (String) getItem(position) + "1", null, mListener);
			mImageLoader.loadImage(kittenViews.kitten2, (String) getItem(position) + "2", null, mListener);
		} else {
			mImageLoader.loadImage(kittenViews.kitten1, (String) getItem(position), null, mListener);
			mImageLoader.loadImage(kittenViews.kitten2, (String) getItem(position), null, mListener);
		}

		return convertView;
	}

	ImageLoaderListener mListener = new ImageLoaderListener() {
		@Override
		public void onImageLoadError(String error) {
			Log.i("ImageLoader", "Image load failed! Message: " + error);
		}

		@Override
		public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
			imageView.setImageBitmap(bitmap);
		}
	};

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
