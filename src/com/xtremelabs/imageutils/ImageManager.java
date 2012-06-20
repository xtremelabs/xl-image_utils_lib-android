package com.xtremelabs.imageutils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.app.Fragment;

import com.xtremelabs.imageutils.ImageCacher.ImageRequestListener;

class ImageManager {
	private ImageListenerMapper listenerHelper = new ImageListenerMapper();
	private Handler handler;
	private static ImageManager imageManager;
	private ImageCacher imageCacher;

	private ImageManager(Context context) {
		imageCacher = ImageCacher.getInstance(context);
		handler = new Handler(context.getMainLooper());
	}

	// TODO: Change from the "context" being passed in to the "mainLooper" being passed in.
	public synchronized static ImageManager getInstance(Context context) {
		if (!(context instanceof Application)) {
			throw new IllegalArgumentException("The context passed in must be an application context!");
		}
		
		if (imageManager == null) {
			imageManager = new ImageManager(context);
		}
		
		return imageManager;
	}

	public void getBitmap(Activity activity, final String url, ImageReceivedListener listener) {
		getBitmap((Object) activity, url, listener);
	}

	public void getBitmap(Fragment fragment, final String url, ImageReceivedListener listener) {
		getBitmap((Object) fragment, url, listener);
	}

	/**
	 * This call must be made from the UI thread.
	 * 
	 * @param key
	 * @param url
	 * @param listener
	 */
	public void getBitmap(Object key, final String url, ImageReceivedListener listener) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		CacheListener cacheListener = new CacheListener(url);

		listenerHelper.registerNewListener(listener, key, cacheListener);
		
		Bitmap bitmap = imageCacher.getBitmap(url, cacheListener);
		if (bitmap != null) {
			if (listenerHelper.unregisterListener(listener)) {
				listener.onImageReceived(bitmap);
			}
		}
	}
	
	public void getBitmap(Object key, String url, ImageReceivedListener listener, Integer width, Integer height) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		CacheListener cacheListener = new CacheListener(url);

		listenerHelper.registerNewListener(listener, key, cacheListener);
		
		Bitmap bitmap = imageCacher.getBitmapWithBounds(url, cacheListener, width, height);
		if (bitmap != null) {
			if (listenerHelper.unregisterListener(listener)) {
				listener.onImageReceived(bitmap);
			}
		}
	}
	
	public void getBitmap(Object key, String url, ImageReceivedListener listener, int overrideSampleSize) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		CacheListener cacheListener = new CacheListener(url);

		listenerHelper.registerNewListener(listener, key, cacheListener);
		
		Bitmap bitmap = imageCacher.getBitmapWithScale(url, cacheListener, overrideSampleSize);
		if (bitmap != null) {
			if (listenerHelper.unregisterListener(listener)) {
				listener.onImageReceived(bitmap);
			}
		}
	}

	public void removeListenersForKey(Object key) {
		listenerHelper.removeAllEntriesForKey(key);
	}

	class CacheListener extends ImageRequestListener {
		private String url;

		public CacheListener(String url) {
			this.url = url;
		}

		@Override
		public void onImageAvailable(final Bitmap bitmap) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					ImageReceivedListener listener = listenerHelper.getAndRemoveListener(CacheListener.this);
					if (listener != null) {
						listener.onImageReceived(bitmap);
					}
				}
			});
		}

		@Override
		public void onFailure(String message) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					ImageReceivedListener listener = listenerHelper.getAndRemoveListener(CacheListener.this);
					if (listener != null) {
						listener.onLoadImageFailed();
					}
				}
			});
		}

		public void setCancelled(boolean cancelled) {
			if (cancelled) {
				imageCacher.cancelRequestForBitmap(url, this);
			}
		}
	}
}
