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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;

/**
 * This class defensively handles requests from four locations: LifecycleReferenceManager, ImageMemoryCacherInterface, ImageDiskCacherInterface, ImageNetworkInterface and the AsyncOperationsMaps.
 * 
 * The job of this class is to "route" messages appropriately in order to ensure synchronized handling of image downloading and caching operations.
 */
public class ImageCacher implements ImageDownloadObserver, ImageDecodeObserver, AsyncOperationsObserver {
	private static final String PREFIX = "IMAGE CACHER - ";

	private static ImageCacher mImageCacher;

	private final ImageDiskCacherInterface mDiskCache;
	private ImageMemoryCacherInterface mMemoryCache;
	private final ImageNetworkInterface mNetworkInterface;

	private final AsyncOperationsMaps mAsyncOperationsMap;

	private ImageCacher(Context appContext) {
		if (Build.VERSION.SDK_INT <= 11) {
			mMemoryCache = new SizeEstimatingMemoryLRUCacher();
		} else {
			mMemoryCache = new AdvancedMemoryLRUCacher();
		}

		mDiskCache = new DiskLRUCacher(appContext, this);
		mNetworkInterface = new ImageDownloader(mDiskCache, this);
		mAsyncOperationsMap = new AsyncOperationsMaps(this);
	}

	public static synchronized ImageCacher getInstance(Context appContext) {
		if (mImageCacher == null) {
			mImageCacher = new ImageCacher(appContext);
		}
		return mImageCacher;
	}

	public Bitmap getBitmap(RequestIdentifier requestIdentifier, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		throwExceptionIfNeeded(requestIdentifier, imageCacherListener, scalingInfo);

		AsyncOperationState asyncOperationState = mAsyncOperationsMap.queueListenerIfRequestPending(requestIdentifier, imageCacherListener, scalingInfo);

		switch (asyncOperationState) {
		case QUEUED_FOR_NETWORK_REQUEST:
			mNetworkInterface.bump(requestIdentifier);
			return null;
		case QUEUED_FOR_DECODE_REQUEST:
			mDiskCache.bumpInStack(requestIdentifier, getSampleSize(requestIdentifier, scalingInfo));
			return null;
		}

		int sampleSize = getSampleSize(requestIdentifier, scalingInfo);
		if (mDiskCache.isCached(requestIdentifier) && sampleSize != -1) {
			Bitmap bitmap;
			if ((bitmap = mMemoryCache.getBitmap(requestIdentifier, sampleSize)) != null) {
				return bitmap;
			} else {
				decodeBitmapFromDisk(requestIdentifier, imageCacherListener, sampleSize);
			}
		} else {
			downloadImageFromNetwork(requestIdentifier, imageCacherListener, scalingInfo);
		}
		return null;
	}

	@Override
	public int getSampleSize(RequestIdentifier requestIdentifier, ScalingInfo scalingInfo) {
		int sampleSize;
		if (scalingInfo.sampleSize != null) {
			sampleSize = scalingInfo.sampleSize;
		} else {
			sampleSize = mDiskCache.getSampleSize(requestIdentifier, scalingInfo.width, scalingInfo.height);
		}
		return sampleSize;
	}

	/**
	 * Caches the image at the provided url to disk. If the image is already on disk, it gets bumped on the eviction queue.
	 * 
	 * @param url
	 */
	public synchronized void precacheImage(RequestIdentifier requestIdentifier) {
		validateUrl(requestIdentifier);

		if (!mAsyncOperationsMap.isNetworkRequestPendingForUrl(requestIdentifier) && !mDiskCache.isCached(requestIdentifier)) {
			mNetworkInterface.downloadImageToDisk(requestIdentifier);
		} else {
			mDiskCache.bumpOnDisk(requestIdentifier);
		}
	}

	public void clearMemCache() {
		mMemoryCache.clearCache();
	}

	public void setMaximumCacheSize(long size) {
		mMemoryCache.setMaximumCacheSize(size);
	}

	public void cancelRequestForBitmap(ImageCacherListener imageCacherListener) {
		if (Logger.logAll()) {
			Logger.d(PREFIX + "Cancelling a request.");
		}
		mAsyncOperationsMap.cancelPendingRequest(imageCacherListener);
	}

	private void downloadImageFromNetwork(RequestIdentifier requestIdentifier, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		mAsyncOperationsMap.registerListenerForNetworkRequest(imageCacherListener, requestIdentifier, scalingInfo);
		mNetworkInterface.downloadImageToDisk(requestIdentifier);
	}

	private void decodeBitmapFromDisk(RequestIdentifier requestIdentifier, ImageCacherListener imageCacherListener, int sampleSize) {
		mAsyncOperationsMap.registerListenerForDecode(imageCacherListener, requestIdentifier, sampleSize);
		mDiskCache.getBitmapAsynchronouslyFromDisk(requestIdentifier, sampleSize, ImageReturnedFrom.DISK, true);
	}

	private void validateUrl(RequestIdentifier requestIdentifier) {
		if (requestIdentifier == null || requestIdentifier.getUrlOrFilename() == null || requestIdentifier.getUrlOrFilename().length() == 0) {
			throw new IllegalArgumentException("Null URL passed into the image system.");
		}
	}

	private void throwExceptionIfNeeded(RequestIdentifier requestIdentifier, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		ThreadChecker.throwErrorIfOffUiThread();

		validateUrl(requestIdentifier);

		if (imageCacherListener == null) {
			throw new IllegalArgumentException("The ImageCacherListener must not be null.");
		}

		if (scalingInfo == null) {
			throw new IllegalArgumentException("The ScalingInfo must not be null.");
		}
	}

	public static abstract class ImageCacherListener {
		public abstract void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom);

		public abstract void onFailure(String message);
	}

	@Override
	public void onImageDecoded(Bitmap bitmap, RequestIdentifier requestIdentifier, int sampleSize, ImageReturnedFrom returnedFrom) {
		mMemoryCache.cacheBitmap(bitmap, requestIdentifier, sampleSize);
		mAsyncOperationsMap.onDecodeSuccess(bitmap, requestIdentifier, sampleSize, returnedFrom);
	}

	@Override
	public void onImageDecodeFailed(RequestIdentifier requestIdentifier, int sampleSize, String message) {
		mAsyncOperationsMap.onDecodeFailed(requestIdentifier, sampleSize, message);
	}

	@Override
	public void onImageDownloaded(RequestIdentifier requestIdentifier) {
		mAsyncOperationsMap.onDownloadComplete(requestIdentifier);
	}

	@Override
	public void onImageDownloadFailed(RequestIdentifier requestIdentifier, String message) {
		mAsyncOperationsMap.onDownloadFailed(requestIdentifier, message);
	}

	@Override
	public void onImageDecodeRequired(RequestIdentifier requestIdentifier, int sampleSize) {
		mDiskCache.getBitmapAsynchronouslyFromDisk(requestIdentifier, sampleSize, ImageReturnedFrom.NETWORK, false);
	}

	@Override
	public boolean isNetworkRequestPendingForUrl(RequestIdentifier requestIdentifier) {
		return mNetworkInterface.isNetworkRequestPendingForUrl(requestIdentifier);
	}

	@Override
	public boolean isDecodeRequestPending(DecodeOperationParameters decodeOperationParameters) {
		return mDiskCache.isDecodeRequestPending(decodeOperationParameters);
	}
}
