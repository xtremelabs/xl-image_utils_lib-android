package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;

class ImageCacher implements ImageDownloadObserver, ImageDecodeObserver, AsyncOperationsObserver {
	@SuppressWarnings("unused")
	private static final String TAG = "ImageCacher";
	private static ImageCacher mImageCacher;

	private ImageDiskCacherInterface mDiskCache;
	private ImageMemoryCacherInterface mMemoryCache;
	private ImageNetworkInterface mNetworkInterface;

	private AsyncOperationsMaps mAsyncOperationsMap;

	private ImageCacher(Context appContext) {
		mMemoryCache = new DefaultImageMemoryLRUCacher();
		mDiskCache = new DefaultImageDiskCacher(appContext, this);
		mNetworkInterface = new DefaultImageDownloader(mDiskCache, this);
		mAsyncOperationsMap = new AsyncOperationsMaps(this);
	}

	public static synchronized ImageCacher getInstance(Context appContext) {
		if (mImageCacher == null) {
			mImageCacher = new ImageCacher(appContext);
		}
		return mImageCacher;
	}

	public Bitmap getBitmap(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		throwExceptionIfNeeded(url, imageCacherListener, scalingInfo);

		AsyncOperationState asyncOperationState = mAsyncOperationsMap.queueListenerIfRequestPending(imageCacherListener, url, scalingInfo);
		if (asyncOperationState != AsyncOperationState.NOT_QUEUED) {
			return null;
		}

		int sampleSize = getSampleSize(url, scalingInfo);
		if (mDiskCache.isCached(url) && sampleSize != -1) {
			Bitmap bitmap;
			if ((bitmap = mMemoryCache.getBitmap(url, sampleSize)) != null) {
				return bitmap;
			} else {
				decodeBitmapFromDisk(url, imageCacherListener, sampleSize);
			}
		} else {
			downloadImageFromNetwork(url, imageCacherListener, scalingInfo);
		}
		return null;
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

	// TODO: Trigger a network request for the image. We may want to move this into the database...
	// FIXME: What happens during the error condition?
	public Dimensions getImageDimensions(String url) throws FileNotFoundException {
		return mDiskCache.getImageDimensions(url);
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
			mDiskCache.bump(url);
		}
	}

	public void clearMemCache() {
		mMemoryCache.clearCache();
	}

	public void setMemCacheSize(int size) {
		mMemoryCache.setMaximumCacheSize(size);
	}

	public void cancelRequestForBitmap(ImageCacherListener imageCacherListener) {
		mAsyncOperationsMap.cancelPendingRequest(imageCacherListener);
	}

	private void downloadImageFromNetwork(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		mAsyncOperationsMap.registerListenerForNetworkRequest(imageCacherListener, url, scalingInfo);
		mNetworkInterface.downloadImageToDisk(url);
	}

	private void decodeBitmapFromDisk(String url, ImageCacherListener imageCacherListener, int sampleSize) {
		mAsyncOperationsMap.registerListenerForDecode(imageCacherListener, url, sampleSize);
		mDiskCache.getBitmapAsynchronouslyFromDisk(url, sampleSize);
	}

	private void validateUrl(String url) {
		if (url == null || url.length() == 0) {
			throw new IllegalArgumentException("Null URL passed into the image system.");
		}
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
		public abstract void onImageAvailable(Bitmap bitmap);

		public abstract void onFailure(String message);
	}

	@Override
	public void onImageDecoded(Bitmap bitmap, String url, int sampleSize) {
		mMemoryCache.cacheBitmap(bitmap, url, sampleSize);
		mAsyncOperationsMap.onDecodeSuccess(bitmap, url, sampleSize);
	}

	@Override
	public void onImageDecodeFailed(String url, int sampleSize) {
		mAsyncOperationsMap.onDecodeFailed(url, sampleSize);
	}

	@Override
	public void onImageDownloaded(String url) {
		mAsyncOperationsMap.onDownloadComplete(url);
	}

	@Override
	public void onImageDownloadFailed(String url) {
		mAsyncOperationsMap.onDownloadFailed(url);
	}

	@Override
	public void onImageDecodeRequired(String url, int sampleSize) {
		mDiskCache.getBitmapAsynchronouslyFromDisk(url, sampleSize);
	}

	@Override
	public void cancelNetworkRequest(String url) {
		mNetworkInterface.cancelRequest(url);
	}

	@Override
	public void cancelDecodeRequest(String url, int sampleSize) {
		mDiskCache.cancelRequest(url, sampleSize);
	}
}
