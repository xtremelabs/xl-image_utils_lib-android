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

package com.xtremelabs.imageutils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageLoader.Options;

/**
 * This class should be instantiated from the method {@link ImageLoader#buildWidgetImageLoader(Object, Context)}.<br>
 * <br>
 * This class is designed to utilize the {@link ImageLoader} to load images specifically for widgets. Placing images in widgets is fairly different from loading images for an app, and therefore any image loads should be
 * going through this class instead of straight through the ImageLoader itself.<br>
 * <br>
 * This class should be instantiated from within any class that manages the images for the widget, and destroyed when that class is destroyed with the "destroy" method.
 */
public class WidgetImageLoader {
	private final ImageLoader mImageLoader;
	private volatile boolean mDestroyed = false;
	private final Context mContext;

	WidgetImageLoader(Object imageLoaderClass, Context context) {
		mImageLoader = new ImageLoader(imageLoaderClass, context);
		mContext = context;
	}

	/**
	 * This method will either immediately return a bitmap (if one is available), or report that a request for the image has been "queued". Once the requested image is available,
	 * {@link ImageDownloadedListener#onImageDownloaded()} will be called, allowing the widget to be updated with the now-available image.
	 * 
	 * @param uri
	 * @param options
	 * @param listener
	 * @return
	 */
	// TODO This does not handle bad URIs. The system just crashes.
	public ImageResponse loadImageSynchronouslyOrQueueNetworkRequest(String uri, Options options, ImageDownloadedListener listener) {
		if (!isDestroyed()) {
			if (options == null) {
				options = mImageLoader.getDefaultOptions();
			}

			ScalingInfo scalingInfo = mImageLoader.getScalingInfo(null, options);
			CacheRequest imageRequest = new CacheRequest(uri, scalingInfo, options);
			imageRequest.setImageRequestType(ImageRequestType.PRECACHE_TO_DISK);
			return ImageCacher.getInstance(mContext).getBitmapSynchronouslyFromDiskOrMemory(imageRequest, getImageCacherListener(listener));
		} else {
			Log.w(ImageLoader.TAG, "WARNING: loadImageSynchronouslyFromDiskOrMemory was called after the ImageLoader was destroyed.");
			return null;
		}
	}

	private static ImageCacherListener getImageCacherListener(final ImageDownloadedListener listener) {
		return new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
				listener.onImageDownloaded();
			}

			@Override
			public void onFailure(String message) {
				listener.onImageDownloadFailure();
			}
		};
	}

	/**
	 * This method must be called when your widget is destroyed.
	 */
	public synchronized void destroy() {
		mDestroyed = true;

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mImageLoader.destroy();
			}
		});
	}

	private synchronized boolean isDestroyed() {
		return mDestroyed;
	}

	public static interface ImageDownloadedListener {
		public void onImageDownloaded();

		public void onImageDownloadFailure();
	}
}
