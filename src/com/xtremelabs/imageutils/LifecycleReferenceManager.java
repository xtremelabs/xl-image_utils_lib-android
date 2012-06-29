package com.xtremelabs.imageutils;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

/**
 * This class is responsible for maintaining a clear separation between the cacher and the lifecycle classes that originally made image requests (ie. Activities
 * and Fragments).
 * 
 * HashMaps are used to maintain mappings between the different requests. When an Activity or Fragment is being destroyed, this class will sever all references
 * back to the Activity or Fragment, allowing the class to become garbage collected.
 * 
 * Finally, this class is responsible for ensuring that all calls back to listeners in the ImageLoader occur on the UI thread. This prevents race conditions in
 * the ImageLoader and simplifies loading the bitmaps back to image views.
 * 
 * @author Jamie Halpern
 */
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

	/**
	 * Maps the key (usually an Activity or Fragment to the Bitmap request.
	 * 
	 * @param key
	 * @param url
	 * @param imageManagerListener
	 * @param scalingInfo
	 */
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
			listener.onImageReceived(bitmap, ImageReturnedFrom.MEMORY);
		}
	}

	class ImageManagerCacheListener extends ImageCacherListener {
		@Override
		public void onImageAvailable(final Bitmap bitmap, final ImageReturnedFrom returnedFrom) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					if (listener != null) {
						listener.onImageReceived(bitmap, returnedFrom);
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
