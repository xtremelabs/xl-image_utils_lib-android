/*
 * Copyright 2012 Xtreme Labs
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

package com.xtremelabs.imageutils;

import java.util.List;

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
 */
// TODO: It may be worthwhile to use a WeakHashMap rather than actively forcing the user to call onDestroy.
// Look into using a ReferenceQueue
class LifecycleReferenceManager {
	private static final String PREFIX = "REFERENCE MANAGER - ";

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
			imageManagerListener.onLoadImageFailed("Blank url");
			return;
		}
		ImageManagerCacheListener cacheListener = generateRegisteredListener(key, url, imageManagerListener);
		ImageReturnValues imageValues = mImageCacher.getBitmapValues(url, cacheListener, scalingInfo);
		returnImageIfValid(imageManagerListener, imageValues);
	}

	public List<ImageManagerListener> removeListenersForKey(Object key) {
		return mListenerHelper.removeAllEntriesForKey(key);
	}

	public void cancelRequest(ImageManagerListener imageManagerListener) {
		if (Logger.logAll()) {
			Logger.d(PREFIX + "Cancelling a request.");
		}
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
	private void returnImageIfValid(ImageManagerListener listener, ImageReturnValues imageValues) {
		if (imageValues != null && imageValues.getBitmap() != null  && mListenerHelper.unregisterListener(listener) != null) {
			listener.onImageReceived(imageValues);
		}
	}

	class ImageManagerCacheListener extends ImageCacherListener {
		@Override
		public void onImageAvailable(final Bitmap bitmap, final ImageReturnedFrom returnedFrom) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					if (Logger.isProfiling()) {
						Profiler.init("End-of-call on UI thread - success");
						Profiler.init("End-of-call  --  Removing the listener.");
					}

					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					
					if (Logger.isProfiling()) {
						Profiler.report("End-of-call  --  Removing the listener.");
					}
					
					if (listener != null) {
						if (Logger.isProfiling()) {
							Profiler.init("End-of-call  --  On Image Received (success)");
						}
						listener.onImageReceived(bitmap, returnedFrom);
						if (Logger.isProfiling()) {
							Profiler.report("End-of-call  --  On Image Received (success)");
						}
					}

					if (Logger.isProfiling()) {
						Profiler.report("End-of-call on UI thread - success");
					}
				}
			});
		}

		@Override
		public void onFailure(final String message) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					if (Logger.isProfiling()) {
						Profiler.init("End-of-call on UI thread - failure");
					}
					
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					if (listener != null) {
						listener.onLoadImageFailed(message);
					}
					
					if (Logger.isProfiling()) {
						Profiler.init("End-of-call on UI thread - failure");
					}
				}
			});
		}

		public void cancelRequest() {
			if (Logger.logAll()) {
				Logger.d(PREFIX + "Cancelling request from within listener.");
			}
			mImageCacher.cancelRequestForBitmap(this);
		}

		@Override
		public void onImageAvailable(final ImageReturnValues imageValues) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					if (Logger.isProfiling()) {
						Profiler.init("End-of-call on UI thread - success");
						Profiler.init("End-of-call  --  Removing the listener.");
					}

					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					
					if (Logger.isProfiling()) {
						Profiler.report("End-of-call  --  Removing the listener.");
					}
					
					if (listener != null) {
						if (Logger.isProfiling()) {
							Profiler.init("End-of-call  --  On Image Received (success)");
						}
						listener.onImageReceived(imageValues);
						if (Logger.isProfiling()) {
							Profiler.report("End-of-call  --  On Image Received (success)");
						}
					}

					if (Logger.isProfiling()) {
						Profiler.report("End-of-call on UI thread - success");
					}
				}
			});
		}
	}
}
