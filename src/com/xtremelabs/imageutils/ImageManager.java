package com.xtremelabs.imageutils;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.xtremelabs.imageutils.ImageCacher.ImageRequestListener;

class ImageManager {
	private ImageListenerMapper listenerHelper = new ImageListenerMapper();
	private Handler handler;
	private static ImageManager imageManager;
	private ImageCacher imageCacher;

	private ImageManager(Context applicationContext) {
		imageCacher = ImageCacher.getInstance(applicationContext);
		handler = new Handler(applicationContext.getMainLooper());
	}

	public synchronized static ImageManager getInstance(Context applicationContext) {
		if (!(applicationContext instanceof Application)) {
			throw new IllegalArgumentException("The context passed in must be an application context!");
		}

		if (imageManager == null) {
			imageManager = new ImageManager(applicationContext);
		}

		return imageManager;
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

		CacheListener cacheListener = generateRegisteredListener(key, url, listener);
		Bitmap bitmap = imageCacher.getBitmap(url, cacheListener);
		returnImageIfValid(listener, bitmap);
	}

	public void getBitmap(Object key, String url, ImageReceivedListener listener, Integer width, Integer height) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		CacheListener cacheListener = generateRegisteredListener(key, url, listener);
		Bitmap bitmap = imageCacher.getBitmapWithBounds(url, cacheListener, width, height);
		returnImageIfValid(listener, bitmap);
	}

	public void getBitmap(Object key, String url, ImageReceivedListener listener, int overrideSampleSize) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		CacheListener cacheListener = generateRegisteredListener(key, url, listener);
		Bitmap bitmap = imageCacher.getBitmapWithScale(url, cacheListener, overrideSampleSize);
		returnImageIfValid(listener, bitmap);
	}

	public void removeListenersForKey(Object key) {
		listenerHelper.removeAllEntriesForKey(key);
	}
	
	public void cancelRequest(ImageReceivedListener listener) {
		// listenerHelper.cancelRequestForListener(listener);
	}
	
	private CacheListener generateRegisteredListener(Object key, String url, ImageReceivedListener listener) {
		CacheListener cacheListener = new CacheListener(url);

		listenerHelper.registerNewListener(listener, key, cacheListener);
		return cacheListener;
	}
	
	private void returnImageIfValid(ImageReceivedListener listener, Bitmap bitmap) {
		if (bitmap != null && listenerHelper.unregisterListener(listener) != null) {
			listener.onImageReceived(bitmap);
		}
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

		public void cancelRequest() {
			imageCacher.cancelRequestForBitmap(url, this);
		}
	}
}
