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

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Build;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;
import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;

/**
 * This class defensively handles requests from four locations: LifecycleReferenceManager, ImageMemoryCacherInterface, ImageDiskCacherInterface, ImageNetworkInterface and the AsyncOperationsMaps.
 * 
 * The job of this class is to "route" messages appropriately in order to ensure synchronized handling of image downloading and caching operations.
 */
public class ImageCacher implements ImageDownloadObserver, ImageDecodeObserver, AsyncOperationsObserver {
	private static enum RequestType {
		LOCAL_REQUEST, REMOTE_REQUEST
	}

	private static final String PREFIX = "IMAGE CACHER - ";

	private static final String FILE_SYSTEM_SCHEME = "file";

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

	public Bitmap getBitmapForWidget(String url, ImageCacherListener cacherListener, ScalingInfo scalingInfo) {
		int sampleSize = getSampleSize(url, scalingInfo);
		Bitmap bitmap;

		if (mDiskCache.isCached(url) && sampleSize != -1) {
			if ((bitmap = mMemoryCache.getBitmap(url, sampleSize)) != null) {
				return bitmap;
			} else {
				try {
					return mDiskCache.getBitmapSynchronouslyFromDisk(url, sampleSize);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			downloadImageFromNetwork(url, cacherListener, scalingInfo);
		}

		return null;
	}

	public Bitmap getBitmap(String uri, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		throwExceptionIfNeeded(uri, imageCacherListener, scalingInfo);

		RequestType requestType;
		try {
			requestType = getRequestType(uri, imageCacherListener);
		} catch (URISyntaxException e) {
			return null;
		}

		AsyncOperationState asyncOperationState = mAsyncOperationsMap.queueListenerIfRequestPending(imageCacherListener, uri, scalingInfo);

		switch (asyncOperationState) {
		case QUEUED_FOR_NETWORK_REQUEST:
			mNetworkInterface.bump(uri);
			return null;
		case QUEUED_FOR_DECODE_REQUEST:
			mDiskCache.bumpInQueue(uri, getSampleSize(uri, scalingInfo));
			return null;
		}

		int sampleSize = getSampleSize(uri, scalingInfo);

		// TODO: Look into removing the sampleSize check.

		if (mDiskCache.isCached(uri) && sampleSize != -1) {
			Bitmap bitmap;
			if ((bitmap = mMemoryCache.getBitmap(uri, sampleSize)) != null) {
				return bitmap;
			} else {
				decodeBitmapFromDisk(uri, imageCacherListener, sampleSize);
			}
		} else if (requestType == RequestType.REMOTE_REQUEST) {
			downloadImageFromNetwork(uri, imageCacherListener, scalingInfo);
		} else if (requestType == RequestType.LOCAL_REQUEST) {
			
			decodeBitmapForLocalRequest(uri, imageCacherListener, scalingInfo);
		}

		return null;
	}

	private RequestType getRequestType(String uri, ImageCacherListener imageCacherListener) throws URISyntaxException {
		RequestType requestType;
		try {
			URI convertedUri = new URI(uri);
			if (convertedUri.getScheme().equals(FILE_SYSTEM_SCHEME)) {
				requestType = RequestType.LOCAL_REQUEST;
			} else {
				requestType = RequestType.REMOTE_REQUEST;
			}
			return requestType;
		} catch (URISyntaxException e) {
			imageCacherListener.onFailure("An invalid URI was provided to the image system. URI provided: " + uri);
			// TODO Make this a more meaningful exception.
			throw e;
		}
	}

	@Override
	public int getSampleSize(String url, ScalingInfo scalingInfo) {
		int sampleSize;
		if (scalingInfo.sampleSize != null) {
			sampleSize = scalingInfo.sampleSize;
		} else {
			sampleSize = mDiskCache.getSampleSize(url, scalingInfo.width, scalingInfo.height);
		}
		return sampleSize;
	}

	/**
	 * Caches the image at the provided url to disk. If the image is already on disk, it gets bumped on the eviction queue.
	 * 
	 * @param url
	 */
	public synchronized void precacheImage(String url) {
		validateUrl(url);

		if (!mAsyncOperationsMap.isNetworkRequestPendingForUrl(url) && !mDiskCache.isCached(url)) {
			mNetworkInterface.downloadImageToDisk(url);
		} else {
			mDiskCache.bumpOnDisk(url);
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

	private void downloadImageFromNetwork(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		mAsyncOperationsMap.registerListenerForNetworkRequest(imageCacherListener, url, scalingInfo);
		mNetworkInterface.downloadImageToDisk(url);
	}

	private void decodeBitmapFromDisk(String url, ImageCacherListener imageCacherListener, int sampleSize) {
		mAsyncOperationsMap.registerListenerForDecode(imageCacherListener, url, sampleSize);
		mDiskCache.getBitmapAsynchronouslyFromDisk(url, sampleSize, ImageReturnedFrom.DISK, true);
	}

	private void decodeBitmapForLocalRequest(String uri, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		int sampleSize = 1;
		if (scalingInfo.sampleSize != null) {
			sampleSize = scalingInfo.sampleSize;
		} else if (scalingInfo.width != null || scalingInfo.height != null) {
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(uri, options);
			Dimensions dimensions = new Dimensions(options.outWidth, options.outHeight);
			sampleSize = mDiskCache.calculateSampleSize(scalingInfo.width, scalingInfo.height, dimensions);
		}

		mAsyncOperationsMap.registerListenerForDecode(imageCacherListener, uri, sampleSize);
		mDiskCache.getLocalBitmapAsynchronouslyFromDisk(uri, sampleSize, ImageReturnedFrom.DISK, true);
	}

	private void validateUrl(String url) {
		if (url == null || url.length() == 0)
			throw new IllegalArgumentException("Null URL passed into the image system.");
	}

	private void throwExceptionIfNeeded(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		ThreadChecker.throwErrorIfOffUiThread();

		validateUrl(url);

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
	public void onImageDecoded(Bitmap bitmap, String url, int sampleSize, ImageReturnedFrom returnedFrom) {
		mMemoryCache.cacheBitmap(bitmap, url, sampleSize);
		mAsyncOperationsMap.onDecodeSuccess(bitmap, url, sampleSize, returnedFrom);
	}

	@Override
	public void onImageDecodeFailed(String url, int sampleSize, String message) {
		mAsyncOperationsMap.onDecodeFailed(url, sampleSize, message);
	}

	@Override
	public void onImageDownloaded(String url) {
		mAsyncOperationsMap.onDownloadComplete(url);
	}

	@Override
	public void onImageDownloadFailed(String url, String message) {
		mAsyncOperationsMap.onDownloadFailed(url, message);
	}

	@Override
	public void onImageDecodeRequired(String url, int sampleSize) {
		mDiskCache.getBitmapAsynchronouslyFromDisk(url, sampleSize, ImageReturnedFrom.NETWORK, false);
	}

	@Override
	public boolean isNetworkRequestPendingForUrl(String url) {
		return mNetworkInterface.isNetworkRequestPendingForUrl(url);
	}

	@Override
	public boolean isDecodeRequestPending(DecodeOperationParameters decodeOperationParameters) {
		return mDiskCache.isDecodeRequestPending(decodeOperationParameters);
	}

	public void setNetworkRequestCreator(NetworkRequestCreator networkRequestCreator) {
		mNetworkInterface.setNetworkRequestCreator(networkRequestCreator);
	}
}
