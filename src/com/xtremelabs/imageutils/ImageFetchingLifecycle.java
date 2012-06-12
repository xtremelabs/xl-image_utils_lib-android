package com.xtremelabs.imageutils;

import android.app.Activity;
import android.widget.ImageView;

import com.functionx.viggle.interfaces.LifecycleListener;

public class ImageFetchingLifecycle extends LifecycleListener {

	private ImageFetcherUtility imageFetchingUtility;
	private Activity activity;
	
	public ImageFetchingLifecycle(Activity activity) {
		this.activity = activity;
	}
	
	@Override
	public void onCreate() {
		imageFetchingUtility = new ImageFetcherUtility(activity);
	}

	@Override
	public void onDestroy() {
		ImageManager.getInstance(activity).removeListenersForActivity(activity);
	}
	
	public void loadImage(ImageView imageView, String url) {
		imageFetchingUtility.loadImage(imageView, url);
	}
	
	public void loadImage(ImageView imageView, String url, int placeholderImageResourceId) {
		imageFetchingUtility.loadImage(imageView, url, placeholderImageResourceId);
	}
	
	public void loadImage(ImageView imageView, String url, final ImageLoadingListener listener) {
		imageFetchingUtility.loadImage(imageView, url, listener);
	}
	
	public void loadImage(ImageView imageView, String url, int placeholderImageResourceId, final ImageLoadingListener listener) {
		imageFetchingUtility.loadImage(imageView, url, placeholderImageResourceId, listener);
	}

}
