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
import android.os.Handler;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;

/**
 * This class is responsible for maintaining a clear separation between the cacher and the lifecycle classes that originally made image requests (ie. Activities and Fragments).
 * 
 * HashMaps are used to maintain mappings between the different requests. When an Activity or Fragment is being destroyed, this class will sever all references back to the Activity or Fragment, allowing the class to
 * become garbage collected.
 * 
 * Finally, this class is responsible for ensuring that all calls back to listeners in the ImageLoader occur on the UI thread. This prevents race conditions in the ImageLoader and simplifies loading the bitmaps back to
 * image views.
 */
// TODO: It may be worthwhile to use a WeakHashMap rather than actively forcing the user to call onDestroy.
// Look into using a ReferenceQueue
class LifecycleReferenceManager implements ReferenceManager {
	private static LifecycleReferenceManager referenceManager;

	private final LifecycleKeyListenerMapper mListenerHelper = new LifecycleKeyListenerMapper();
	private final Handler mUiThreadHandler;
	private final ImageCacher mImageCacher;

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
	@Override
	public void getBitmap(Object key, ImageRequest imageRequest, ImageManagerListener imageManagerListener) {
		String uri = imageRequest.getUri();

		if (GeneralUtils.isStringBlank(uri)) {
			imageManagerListener.onLoadImageFailed("Blank url");
			return;
		}
		ImageManagerCacheListener cacheListener = generateRegisteredListener(key, uri, imageManagerListener);
		ImageResponse imageResponse = mImageCacher.getBitmap(imageRequest, cacheListener);
		returnImageIfValid(imageManagerListener, imageResponse);
	}

	@Override
	public List<ImageManagerListener> removeListenersForKey(Object key) {
		return mListenerHelper.removeAllEntriesForKey(key);
	}

	@Override
	public void cancelRequest(ImageManagerListener imageManagerListener) {
		mListenerHelper.unregisterListener(imageManagerListener).cancelRequest();
	}

	private ImageManagerCacheListener generateRegisteredListener(Object key, String url, ImageManagerListener listener) {
		ImageManagerCacheListener cacheListener = new ImageManagerCacheListener();

		mListenerHelper.registerNewListener(listener, key, cacheListener);
		return cacheListener;
	}

	private void returnImageIfValid(ImageManagerListener listener, ImageResponse imageResponse) {
		if (imageResponse.getImageResponseStatus() == ImageResponseStatus.SUCCESS && mListenerHelper.unregisterListener(listener) != null) {
			listener.onImageReceived(imageResponse);
		}
	}

	class ImageManagerCacheListener extends ImageCacherListener {
		@Override
		public void onImageAvailable(final ImageResponse imageResponse) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);

					if (listener != null) {
						listener.onImageReceived(imageResponse);
					}
				}
			});
		}

		@Override
		public void onFailure(final String message) {
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageManagerListener listener = mListenerHelper.getAndRemoveListener(ImageManagerCacheListener.this);
					if (listener != null) {
						listener.onLoadImageFailed(message);
					}
				}
			});
		}

		public void cancelRequest() {
			mImageCacher.cancelRequestForBitmap(this);
		}
	}
}
