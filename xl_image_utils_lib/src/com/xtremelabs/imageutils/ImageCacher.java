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

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;
import com.xtremelabs.imageutils.AsyncOperationsMaps.OperationsObserver;
import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;

/**
 * This class defensively handles requests from four locations:
 * LifecycleReferenceManager, ImageMemoryCacherInterface,
 * ImageDiskCacherInterface, ImageNetworkInterface and the AsyncOperationsMaps.
 * 
 * The job of this class is to "route" messages appropriately in order to ensure
 * synchronized handling of image downloading and caching operations.
 */
class ImageCacher implements ImageDownloadObserver, ImageDiskObserver, OperationsObserver {
	private static ImageCacher mImageCacher;

	private ImageDiskCacherInterface mDiskCache;
	private ImageMemoryCacherInterface mMemoryCache;
	private ImageNetworkInterface mNetworkInterface;

	private AsyncOperationsMaps mAsyncOperationsMap;

	private ImageCacher(Context appContext) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
			mMemoryCache = new SizeEstimatingMemoryLRUCacher();
		} else {
			mMemoryCache = new AdvancedMemoryLRUCacher();
		}

		mDiskCache = new DiskLRUCacher(appContext, this);
		mNetworkInterface = new ImageDownloader(mDiskCache, this);
		mAsyncOperationsMap = new AsyncOperationsMaps(this);
	}

	public static synchronized ImageCacher getInstance(Context context) {
		if (mImageCacher == null) {
			mImageCacher = new ImageCacher(context.getApplicationContext());
		}
		return mImageCacher;
	}

	public Bitmap getBitmapFromMemoryCache(CacheRequest cacheRequest) {
		Bitmap bitmap = null;
		String uri = cacheRequest.getUri();
		int sampleSize = getSampleSize(cacheRequest);
		boolean isCached = mDiskCache.isCached(cacheRequest);
		if (isCached && sampleSize != -1) {
			DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, cacheRequest.getOptions().preferedConfig);
			bitmap = mMemoryCache.getBitmap(decodeSignature);
		}
		return bitmap;
	}

	public void queueCacheRequest(CacheRequest cacheRequest, ImageCacherListener imageCacherListener) {
		ImageUtilsHandler.getInstance().post(new BackgroundRequest(cacheRequest, imageCacherListener));
	}

	public ImageResponse getBitmap(CacheRequest cacheRequest, ImageCacherListener imageCacherListener) {
		throwExceptionIfNeeded(cacheRequest, imageCacherListener);

		if (!cacheRequest.isPrecacheRequest()) {
			Bitmap bitmap = getBitmapFromMemoryCache(cacheRequest);
			if (bitmap != null) {
				return new ImageResponse(bitmap, ImageReturnedFrom.MEMORY, ImageResponseStatus.SUCCESS);
			}
		}

		queueRequestOnHandlerThread(cacheRequest, imageCacherListener);

		return generateQueuedResponse();
	}

	public ImageResponse getBitmapSynchronouslyFromDiskOrMemory(CacheRequest cacheRequest, ImageCacherListener imageCacherListener) {
		String uri = cacheRequest.getUri();

		synchronized (mAsyncOperationsMap) {
			AsyncOperationState state = mAsyncOperationsMap.queueListenerIfRequestPending(cacheRequest, imageCacherListener);
			switch (state) {
			case QUEUED_FOR_NETWORK_REQUEST:
				return generateQueuedResponse();
			case QUEUED_FOR_DETAILS_REQUEST:
			case QUEUED_FOR_DECODE_REQUEST:
				// FIXME I do not believe the "Cancel Pending Request" method does what it should be doing. Needs investigation.
				// mAsyncOperationsMap.cancelPendingRequest(imageCacherListener);
				break;
			case NOT_QUEUED:
				break;
			}
		}

		int sampleSize = getSampleSize(cacheRequest);
		boolean isCached = mDiskCache.isCached(cacheRequest);

		try {
			if (isCached && sampleSize != -1) {
				DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, cacheRequest.getOptions().preferedConfig);
				Bitmap bitmap;
				if ((bitmap = mMemoryCache.getBitmap(decodeSignature)) != null) {
					return new ImageResponse(bitmap, ImageReturnedFrom.MEMORY, ImageResponseStatus.SUCCESS);
				} else {
					return getBitmapSynchronouslyFromDisk(cacheRequest, decodeSignature);
				}
			} else if (cacheRequest.isFileSystemRequest()) {
				mDiskCache.calculateAndSaveImageDetails(cacheRequest);
				sampleSize = getSampleSize(cacheRequest);
				if (sampleSize != -1) {
					DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, cacheRequest.getOptions().preferedConfig);
					return getBitmapSynchronouslyFromDisk(cacheRequest, decodeSignature);
				}
			}
		} catch (FileNotFoundException e) {
			Log.w(ImageLoader.TAG, "Unable to load bitmap synchronously. File not found.");
		} catch (FileFormatException e) {
			Log.w(ImageLoader.TAG, "Unable to load bitmap synchronously. File format exception.");
		} catch (URISyntaxException e) {
			Log.w(ImageLoader.TAG, "Unable to load bitmap synchronously. URISyntaxException. URI: " + uri);
		}

		if (!cacheRequest.isFileSystemRequest()) {
			downloadImageFromNetwork(cacheRequest, imageCacherListener);
		}

		// FIXME We should be returning an ImageResponse with the result "Request Queued"
		return null;
	}

	private ImageResponse getBitmapSynchronouslyFromDisk(CacheRequest cacheRequest, DecodeSignature decodeSignature) throws FileNotFoundException, FileFormatException {
		Bitmap bitmap;
		bitmap = mDiskCache.getBitmapSynchronouslyFromDisk(cacheRequest, decodeSignature);
		return new ImageResponse(bitmap, ImageReturnedFrom.DISK, ImageResponseStatus.SUCCESS);
	}

	// TODO This method is VERY slow. Find ways to improve performance.
	@Override
	public int getSampleSize(CacheRequest imageRequest) {
		ScalingInfo scalingInfo = imageRequest.getScalingInfo();

		int sampleSize;
		if (scalingInfo.sampleSize != null) {
			sampleSize = scalingInfo.sampleSize;
		} else {
			sampleSize = mDiskCache.getSampleSize(imageRequest);
		}
		return sampleSize;
	}

	public void clearMemCache() {
		mMemoryCache.clearCache();
	}

	public void setMaximumMemCacheSize(long size) {
		mMemoryCache.setMaximumCacheSize(size);
	}

	public void setMaximumDiskCacheSize(long maxSizeInBytes) {
		mDiskCache.setDiskCacheSize(maxSizeInBytes);
	}

	public void cancelRequestForBitmap(final ImageCacherListener imageCacherListener) {
		ImageUtilsHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				mAsyncOperationsMap.cancelPendingRequest(imageCacherListener);
			}
		});
	}

	private void downloadImageFromNetwork(CacheRequest imageRequest, ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.registerNetworkRequest(imageRequest, imageCacherListener);
	}

	private void retrieveImageDetails(CacheRequest imageRequest, ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.registerDetailsRequest(imageRequest, imageCacherListener, ImageReturnedFrom.DISK);
	}

	private void decodeBitmapFromDisk(CacheRequest cacheRequest, DecodeSignature decodeSignature, ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.registerDecodeRequest(cacheRequest, decodeSignature, imageCacherListener, ImageReturnedFrom.DISK);
	}

	private static void throwExceptionIfNeeded(CacheRequest cacheRequest, ImageCacherListener imageCacherListener) {
		String uri = cacheRequest.getUri();
		if (uri == null || uri.length() == 0) {
			throw new IllegalArgumentException("Null URI passed into the image system.");
		}

		if (imageCacherListener == null) {
			throw new IllegalArgumentException("The ImageCacherListener must not be null.");
		}

		if (cacheRequest.getScalingInfo() == null) {
			throw new IllegalArgumentException("The ScalingInfo must not be null.");
		}
	}

	public interface ImageCacherListener {
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
	public void onImageDetailsRetrieved(String uri) {
		mAsyncOperationsMap.onDetailsRequestComplete(uri);
	}

	@Override
	public void onImageDetailsRequestFailed(String uri, String message) {
		mAsyncOperationsMap.onDetailsRequestFailed(uri, message);
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
	public Prioritizable getNetworkRunnable(CacheRequest cacheRequest) {
		return mNetworkInterface.getNetworkPrioritizable(cacheRequest);
	}

	@Override
	public Prioritizable getDetailsRunnable(CacheRequest cacheRequest) {
		return mDiskCache.getDetailsPrioritizable(cacheRequest);
	}

	@Override
	public Prioritizable getDecodeRunnable(CacheRequest cacheRequest, DecodeSignature decodeSignature, ImageReturnedFrom imageReturnedFrom) {
		return mDiskCache.getDecodePrioritizable(cacheRequest, decodeSignature, imageReturnedFrom);
	}

	public void setNetworkRequestCreator(NetworkRequestCreator networkRequestCreator) {
		mNetworkInterface.setNetworkRequestCreator(networkRequestCreator);
	}

	void stubMemCache(ImageMemoryCacherInterface memoryCache) {
		mMemoryCache = memoryCache;
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

	private static ImageResponse generateQueuedResponse() {
		return new ImageResponse(null, null, ImageResponseStatus.REQUEST_QUEUED);
	}

	public void invalidateFileSystemUri(String uri) {
		mDiskCache.invalidateFileSystemUri(uri);
		mMemoryCache.removeAllImagesForUri(uri);
	}

	public void notifyDirectionSwapped(CacheKey cacheKey) {
		mAsyncOperationsMap.notifyDirectionSwapped(cacheKey);
	}

	private void queueRequestOnHandlerThread(final CacheRequest cacheRequest, final ImageCacherListener imageCacherListener) {
		ImageUtilsHandler.getInstance().post(new BackgroundRequest(cacheRequest, imageCacherListener));
	}

	private class BackgroundRequest implements Runnable {

		private final CacheRequest mCacheRequest;
		private final ImageCacherListener mImageCacherListener;

		BackgroundRequest(final CacheRequest cacheRequest, final ImageCacherListener imageCacherListener) {
			mCacheRequest = cacheRequest;
			mImageCacherListener = imageCacherListener;
		}

		@Override
		public void run() {
			AsyncOperationState state = mAsyncOperationsMap.queueListenerIfRequestPending(mCacheRequest, mImageCacherListener);
			switch (state) {
			case QUEUED_FOR_NETWORK_REQUEST:
			case QUEUED_FOR_DECODE_REQUEST:
			case QUEUED_FOR_DETAILS_REQUEST:
				return;
			case NOT_QUEUED:
			default:
				break;
			}

			// TODO: Look into removing the sampleSize check.
			int sampleSize = getSampleSize(mCacheRequest);
			boolean isCached = mDiskCache.isCached(mCacheRequest);

			if (isCached && sampleSize != -1) {
				if (mCacheRequest.getImageRequestType() == ImageRequestType.PRECACHE_TO_DISK) {
					mImageCacherListener.onImageAvailable(new ImageResponse(null, null, ImageResponseStatus.CACHED_ON_DISK));
					return;
				}

				DecodeSignature decodeSignature = new DecodeSignature(mCacheRequest.getUri(), sampleSize, mCacheRequest.getOptions().preferedConfig);
				Bitmap bitmap;
				if ((bitmap = mMemoryCache.getBitmap(decodeSignature)) != null) {
					mImageCacherListener.onImageAvailable(new ImageResponse(bitmap, ImageReturnedFrom.DISK, ImageResponseStatus.SUCCESS));
				} else {
					decodeBitmapFromDisk(mCacheRequest, decodeSignature, mImageCacherListener);
				}
			} else if (mCacheRequest.isFileSystemRequest()) {
				retrieveImageDetails(mCacheRequest, mImageCacherListener);
			} else {
				downloadImageFromNetwork(mCacheRequest, mImageCacherListener);
			}
		}
	}
}
