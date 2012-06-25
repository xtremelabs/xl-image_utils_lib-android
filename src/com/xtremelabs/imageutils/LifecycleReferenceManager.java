package com.xtremelabs.imageutils;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

class LifecycleReferenceManager {
	@SuppressWarnings("unused")
	private static final String TAG = "LifecycleReferenceManager";
	
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
	
	public void getBitmap(Object key, String url, ImageManagerListener imageManagerListener, ScalingInfo scalingInfo) {
		if (GeneralUtils.isStringBlank(url)) {
			imageManagerListener.onLoadImageFailed();
			return;
		}
		ImageManagerCacheListener cacheListener = generateRegisteredListener(key, url, imageManagerListener);
		Bitmap bitmap = mImageCacher.getBitmap(url, cacheListener, scalingInfo);
		returnImageIfValid(imageManagerListener, bitmap);
	}

	public void removeListenersForKey(Object key) {
		mListenerHelper.removeAllEntriesForKey(key);
	}
	
	public void cancelRequest(ImageManagerListener imageManagerListener) {
		mListenerHelper.unregisterListener(imageManagerListener).cancelRequest();
	}
	
	private ImageManagerCacheListener generateRegisteredListener(Object key, String url, ImageManagerListener listener) {
		ImageManagerCacheListener cacheListener = new ImageManagerCacheListener();

		mListenerHelper.registerNewListener(listener, key, cacheListener);
		return cacheListener;
	}
	
	private void returnImageIfValid(ImageManagerListener listener, Bitmap bitmap) {
		if (bitmap != null && mListenerHelper.unregisterListener(listener) != null) {
			listener.onImageReceived(bitmap, true);
		}
	}

	class ImageManagerCacheListener extends ImageCacherListener {
		@Override
		public void onImageAvailable(final Bitmap bitmap) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					if (listener != null) {
						listener.onImageReceived(bitmap, false);
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
			mImageCacher.cancelRequestForBitmap(this);
		}
	}
}
