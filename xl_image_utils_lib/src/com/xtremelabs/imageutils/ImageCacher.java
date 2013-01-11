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
import android.graphics.Bitmap;
import android.os.Build;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;

/**
 * This class defensively handles requests from four locations: LifecycleReferenceManager, ImageMemoryCacherInterface, ImageDiskCacherInterface, ImageNetworkInterface and the AsyncOperationsMaps.
 * 
 * The job of this class is to "route" messages appropriately in order to ensure synchronized handling of image downloading and caching operations.
 */
public class ImageCacher implements ImageDownloadObserver, ImageDiskObserver, AsyncOperationsObserver {
	private static ImageCacher mImageCacher;

	private ImageDiskCacherInterface mDiskCache;
	private ImageMemoryCacherInterface mMemoryCache;
	private ImageNetworkInterface mNetworkInterface;

	private AsyncOperationsMaps mAsyncOperationsMap;

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

	public ImageResponse getBitmap(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		String uri = imageRequest.getUri();
		throwExceptionIfNeeded(imageRequest, imageCacherListener);

		AsyncOperationState state = mAsyncOperationsMap.queueListenerIfRequestPending(imageRequest, imageCacherListener);
		switch (state) {
		case QUEUED_FOR_NETWORK_REQUEST:
			mNetworkInterface.bump(uri);
			return generateQueuedResponse();
		case QUEUED_FOR_DECODE_REQUEST:
			mDiskCache.bumpInQueue(new DecodeSignature(uri, getSampleSize(imageRequest), imageRequest.getOptions().preferedConfig));
			return generateQueuedResponse();
		case QUEUED_FOR_DETAILS_REQUEST:
			mDiskCache.bumpInQueue(new DecodeSignature(uri, 0, imageRequest.getOptions().preferedConfig));
			return generateQueuedResponse();
		case NOT_QUEUED:
			break;
		default:
			break;
		}

		int sampleSize = getSampleSize(imageRequest);

		// TODO: Look into removing the sampleSize check.

		boolean isCached = mDiskCache.isCached(uri);

		if (isCached && sampleSize != -1) {
			DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, imageRequest.getOptions().preferedConfig);
			Bitmap bitmap;
			if ((bitmap = mMemoryCache.getBitmap(decodeSignature)) != null) {
				return new ImageResponse(bitmap, ImageReturnedFrom.MEMORY, ImageResponseStatus.SUCCESS);
			} else {
				decodeBitmapFromDisk(decodeSignature, imageCacherListener);
			}
		} else if (GeneralUtils.isFileSystemUri(uri)) {
			retrieveImageDetails(imageRequest, imageCacherListener);
		} else {
			downloadImageFromNetwork(imageRequest, imageCacherListener);
		}

		return generateQueuedResponse();
	}

	@Override
	public int getSampleSize(ImageRequest imageRequest) {
		ScalingInfo scalingInfo = imageRequest.getScalingInfo();

		int sampleSize;
		if (scalingInfo.sampleSize != null) {
			sampleSize = scalingInfo.sampleSize;
		} else {
			sampleSize = mDiskCache.getSampleSize(imageRequest);
		}
		return sampleSize;
	}

	/**
	 * Caches the image at the provided uri to disk. If the image is already on disk, it gets bumped on the eviction queue.
	 * 
	 * @param uri
	 */
	public void precacheImageToDisk(ImageRequest imageRequest) {
		String uri = imageRequest.getUri();
		validateUri(uri);

		if (GeneralUtils.isFileSystemUri(uri)) {
			return;
		}

		if (!mAsyncOperationsMap.isNetworkRequestPending(uri) && !mDiskCache.isCached(uri)) {
			mAsyncOperationsMap.registerListenerForNetworkRequest(imageRequest, new ImageCacherListener() {
				@Override
				public void onImageAvailable(ImageResponse imageResponse) {
					// Intentionally blank.
				}

				@Override
				public void onFailure(String message) {
					// Intentionally blank.
				}
			});
			mNetworkInterface.downloadImageToDisk(uri);
		} else {
			mDiskCache.bumpOnDisk(uri);
		}
	}

	public void clearMemCache() {
		mMemoryCache.clearCache();
	}

	public void setMaximumCacheSize(long size) {
		mMemoryCache.setMaximumCacheSize(size);
	}

	public void cancelRequestForBitmap(ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.cancelPendingRequest(imageCacherListener);
	}

	private void downloadImageFromNetwork(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.registerListenerForNetworkRequest(imageRequest, imageCacherListener);
		mNetworkInterface.downloadImageToDisk(imageRequest.getUri());
	}

	private void retrieveImageDetails(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.registerListenerForDetailsRequest(imageRequest, imageCacherListener);
		mDiskCache.retrieveImageDetails(imageRequest.getUri());
	}

	private void decodeBitmapFromDisk(DecodeSignature decodeSignature, ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.registerListenerForDecode(decodeSignature, imageCacherListener);
		mDiskCache.getBitmapAsynchronouslyFromDisk(decodeSignature, ImageReturnedFrom.DISK, true);
	}

	private void validateUri(String uri) {
		if (uri == null || uri.length() == 0)
			throw new IllegalArgumentException("Null URI passed into the image system.");
	}

	private void throwExceptionIfNeeded(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		ThreadChecker.throwErrorIfOffUiThread();

		validateUri(imageRequest.getUri());

		if (imageCacherListener == null) {
			throw new IllegalArgumentException("The ImageCacherListener must not be null.");
		}

		if (imageRequest.getScalingInfo() == null) {
			throw new IllegalArgumentException("The ScalingInfo must not be null.");
		}
	}

	public static abstract class ImageCacherListener {
		public abstract void onImageAvailable(ImageResponse imageResponse);

		public abstract void onFailure(String message);
	}

	@Override
	public void onImageDecoded(DecodeSignature decodeSignature, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
		mMemoryCache.cacheBitmap(bitmap, decodeSignature);
		mAsyncOperationsMap.onDecodeSuccess(bitmap, returnedFrom, decodeSignature);
	}

	@Override
	public void onImageDecodeFailed(DecodeSignature decodeSignature, String message) {
		mAsyncOperationsMap.onDecodeFailed(decodeSignature, message);
	}

	@Override
	public void onImageDownloaded(String uri) {
		mAsyncOperationsMap.onDownloadComplete(uri);
	}

	@Override
	public void onImageDownloadFailed(String uri, String message) {
		mAsyncOperationsMap.onDownloadFailed(uri, message);
	}

	@Override
	public void onImageDecodeRequired(DecodeSignature decodeSignature) {
		mDiskCache.getBitmapAsynchronouslyFromDisk(decodeSignature, ImageReturnedFrom.NETWORK, false);
	}

	@Override
	public boolean isNetworkRequestPending(String uri) {
		return mNetworkInterface.isNetworkRequestPendingForUrl(uri);
	}

	@Override
	public boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
		return mDiskCache.isDecodeRequestPending(decodeSignature);
	}

	public void setNetworkRequestCreator(NetworkRequestCreator networkRequestCreator) {
		mNetworkInterface.setNetworkRequestCreator(networkRequestCreator);
	}

	void stubMemCache(ImageMemoryCacherInterface imageMemoryCacherInterface) {
		mMemoryCache = imageMemoryCacherInterface;
	}

	void stubDiskCache(ImageDiskCacherInterface imageDiskCacherInterface) {
		mDiskCache = imageDiskCacherInterface;
	}

	void stubNetwork(ImageNetworkInterface imageNetworkInterface) {
		mNetworkInterface = imageNetworkInterface;
	}

	void stubAsynchOperationsMaps(AsyncOperationsMaps asyncOperationsMaps) {
		mAsyncOperationsMap = asyncOperationsMaps;
	}

	@Override
	public void onImageDetailsRetrieved(String uri) {
		mAsyncOperationsMap.onDetailsRequestComplete(uri);
	}

	@Override
	public void onImageDetailsRequestFailed(String uri, String message) {
		mAsyncOperationsMap.onDetailsRequestFailed(uri, message);
	}

	@Override
	public void onImageDetailsRequired(String uri) {
		mDiskCache.retrieveImageDetails(uri);
	}

	private ImageResponse generateQueuedResponse() {
		return new ImageResponse(null, null, ImageResponseStatus.REQUEST_QUEUED);
	}
}
