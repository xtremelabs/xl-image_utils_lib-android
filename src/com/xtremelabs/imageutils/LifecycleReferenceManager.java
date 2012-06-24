package com.xtremelabs.imageutils;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

class LifecycleReferenceManager {
	private static LifecycleReferenceManager referenceManager;

	private LifecycleKeyListenerMapper mListenerHelper = new LifecycleKeyListenerMapper();
	private Handler mUiThreadHandler;
	private ImageCacher mImageCacher;

	private LifecycleReferenceManager(Context applicationContext) {
		mImageCacher = ImageCacher.getInstance(applicationContext);
		mUiThreadHandler = new Handler(applicationContext.getMainLooper());
	}

	public synchronized static LifecycleReferenceManager getInstance(Context applicationContext) {
		if (!(applicationContext instanceof Application)) {
			throw new IllegalArgumentException("The context passed in must be an application context!");
		}

		if (referenceManager == null) {
			referenceManager = new LifecycleReferenceManager(applicationContext);
		}

		return referenceManager;
	}

	/**
	 * This call must be made from the UI thread.
	 * 
	 * @param key
	 * @param url
	 * @param listener
	 */
	public void getBitmap(Object key, final String url, ImageManagerListener listener) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		ImageManagerCacheListener cacheListener = generateRegisteredListener(key, url, listener);
		Bitmap bitmap = mImageCacher.getBitmap(url, cacheListener);
		returnImageIfValid(listener, bitmap);
	}

	public void getBitmap(Object key, String url, ImageManagerListener listener, Integer width, Integer height) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		ImageManagerCacheListener cacheListener = generateRegisteredListener(key, url, listener);
		Bitmap bitmap = mImageCacher.getBitmapWithBounds(url, cacheListener, width, height);
		returnImageIfValid(listener, bitmap);
	}

	public void getBitmap(Object key, String url, ImageManagerListener listener, int overrideSampleSize) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		ImageManagerCacheListener cacheListener = generateRegisteredListener(key, url, listener);
		Bitmap bitmap = mImageCacher.getBitmapWithScale(url, cacheListener, overrideSampleSize);
		returnImageIfValid(listener, bitmap);
	}

	public void removeListenersForKey(Object key) {
		mListenerHelper.removeAllEntriesForKey(key);
	}
	
	public void cancelRequest(ImageManagerListener listener) {
		// TODO: Do not forget to remove the listeners from THIS class!
		// listenerHelper.cancelRequestForListener(listener);
	}
	
	private ImageManagerCacheListener generateRegisteredListener(Object key, String url, ImageManagerListener listener) {
		ImageManagerCacheListener cacheListener = new ImageManagerCacheListener(url);

		mListenerHelper.registerNewListener(listener, key, cacheListener);
		return cacheListener;
	}
	
	private void returnImageIfValid(ImageManagerListener listener, Bitmap bitmap) {
		if (bitmap != null && mListenerHelper.unregisterListener(listener) != null) {
			listener.onImageReceived(bitmap);
		}
	}

	class ImageManagerCacheListener extends ImageCacherListener {
		private String mUrl;

		public ImageManagerCacheListener(String url) {
			mUrl = url;
		}

		@Override
		public void onImageAvailable(final Bitmap bitmap) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					if (listener != null) {
						listener.onImageReceived(bitmap);
					}
				}
			});
		}

		@Override
		public void onFailure(String message) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					if (listener != null) {
						listener.onLoadImageFailed();
					}
				}
			});
		}

		public void cancelRequest() {
			mImageCacher.cancelRequestForBitmap(mUrl, this);
		}
	}
}
