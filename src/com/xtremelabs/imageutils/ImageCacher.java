package com.xtremelabs.imageutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;

/**
 * This class defensively handles requests from four locations: LifecycleReferenceManager, ImageMemoryCacherInterface, ImageDiskCacherInterface,
 * ImageNetworkInterface and the AsyncOperationsMaps.
 * 
 * The job of this class is to "route" messages appropriately in order to ensure synchronized handling of image downloading and caching operations.
 * 
 * @author Jamie Halpern
 */
class ImageCacher implements ImageDownloadObserver, ImageDecodeObserver, AsyncOperationsObserver {
	@SuppressWarnings("unused")
	private static final String TAG = "ImageCacher";
	private static ImageCacher mImageCacher;

	private DiskCacherInterface mDiskCache;
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

	public Bitmap getBitmap(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		throwExceptionIfNeeded(url, imageCacherListener, scalingInfo);

		AsyncOperationState asyncOperationState = mAsyncOperationsMap.queueListenerIfRequestPending(imageCacherListener, url, scalingInfo);
		switch (asyncOperationState) {
		case QUEUED_FOR_NETWORK_REQUEST:
			mNetworkInterface.bump(url);
			return null;
		case QUEUED_FOR_DECODE_REQUEST:
			mDiskCache.bumpInQueue(url, getSampleSize(url, scalingInfo));
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
		public abstract void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom);

		public abstract void onFailure(String message);
	}

	@Override
	public void onImageDecoded(Bitmap bitmap, String url, int sampleSize, ImageReturnedFrom returnedFrom) {
		mMemoryCache.cacheBitmap(bitmap, url, sampleSize);
		mAsyncOperationsMap.onDecodeSuccess(bitmap, url, sampleSize, returnedFrom);
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
		mDiskCache.getBitmapAsynchronouslyFromDisk(url, sampleSize, ImageReturnedFrom.NETWORK, false);
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
