package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;

import com.xtremelabs.imageutils.DefaultImageDiskCacher.FileFormatException;

class ImageCacher {
	@SuppressWarnings("unused")
	private static final String TAG = "ImageCacher";
	private static ImageCacher mImageCacher;

	private ImageDiskCacherInterface mDiskCache;
	private ImageMemoryCacherInterface mMemoryCache;
	private ImageNetworkInterface mNetworkInterface;

	private ImageCacher(Context appContext) {
		mMemoryCache = new DefaultImageMemoryLRUCacher();
		mDiskCache = new DefaultImageDiskCacher(appContext);
		mNetworkInterface = new DefaultImageDownloader(mDiskCache);
	}

	public static synchronized ImageCacher getInstance(Context appContext) {
		if (mImageCacher == null) {
			mImageCacher = new ImageCacher(appContext);
		}
		return mImageCacher;
	}

	/**
	 * Retrieves and caches the requested bitmap. The bitmap returned by this method is always the full sized image.
	 * 
	 * @param url
	 *            The location of the image on the web.
	 * @param listener
	 *            If the image is not synchronously available with this call, the bitmap will be returned at some time in the future to this listener, so long
	 *            as the request is not cancelled.
	 * @return Returns the bitmap if it is synchronously available, or null.
	 */
	public Bitmap getBitmap(String url, ImageCacherListener listener) {
		return getBitmapWithScale(url, listener, 1);
	}

	/**
	 * This method will return a (potentially) scaled down version of the requested image. If the image requested is smaller than at least one of the bounds
	 * provided, the full image will be returned. Otherwise, this method will return an image that is guaranteed to be larger than both bounds, and potentially
	 * scaled down from its original size in order to conserve memory. Enter "null" for either the width or height if you do not want the image to be scaled
	 * relative to that dimension.
	 * 
	 * Example 1:
	 * 
	 * Let the requested image be of size 640x480. The width specified is 120 pixels, and the height is null (and therefore ignored). The returned image will be
	 * 160x120 pixels.
	 * 
	 * Example 2:
	 * 
	 * Let the requested image be of size 300x300. The width specified is 50px, the height is specified at 140px. The returned image will be 150x150.
	 * 
	 * @param url
	 *            Location of the image on the web.
	 * @param imageCacherListener
	 *            If the image is not synchronously available, null will be returned, and the image will eventually be sent to this listener.
	 * @param width
	 *            The width (in pixels) of the display area for the image. This is usually the width of the ImageView or ImageButton that the image will be
	 *            loaded to.
	 * @param height
	 *            The height (in pixels) of the display area for the image. This is usually the height of the ImageView or ImageButton that the image will be
	 *            loaded to.
	 * @return Returns a reference to the bitmap if it is synchronously available. Otherwise null is returned, and the bitmap will eventually be returned to the
	 *         listener if the request is not cancelled.
	 */
	public synchronized Bitmap getBitmapWithBounds(final String url, final ImageCacherListener imageCacherListener, final Integer width, final Integer height) {
		validateUrl(url);

		SampleSizeFetcher fetcher = generateSampleSizeFetcher(url, width, height);
		NetworkImageRequestListener networkImageRequestListener = getNewNetworkImageRequestListener(url, imageCacherListener, fetcher);
		if (!mNetworkInterface.queueIfDownloadingFromNetwork(url, networkImageRequestListener)) {
			/*
			 * We only know the sample size if the image is currently cached on disk. Otherwise we need it from the network first.
			 */
			if (mDiskCache.isCached(url)) {
				try {
					return getBitmapFromMemoryOrDisk(url, imageCacherListener, mDiskCache.getSampleSize(url, width, height));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (FileFormatException e) {
					e.printStackTrace();
				}
			}

			loadImageFromNetwork(url, imageCacherListener, generateSampleSizeFetcher(url, width, height));
		}
		return null;
	}

	private SampleSizeFetcher generateSampleSizeFetcher(final String url, final Integer width, final Integer height) {
		return new SampleSizeFetcher() {
			@Override
			public int onSampleSizeRequired() throws FileNotFoundException {
				try {
					return mDiskCache.getSampleSize(url, width, height);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					throw e;
				}
			}
		};
	}

	/**
	 * Retrieves and caches the bitmap for the provided url. The bitmap returned will be scaled by the provided sample size.
	 * 
	 * @param url
	 *            Location of the image on the web.
	 * @param imageCacherListener
	 *            If the image is not synchronously available, null will be returned, and the image will eventually be sent to this listener.
	 * @param sampleSize
	 *            This parameter MUST BE A POWER OF 2 unless you have good reason to pick a non-power-of-two value. The width and height dimensions of the image
	 *            being returned will be divided by this sample size.
	 * @return Returns a reference to the bitmap if it is synchronously available. Otherwise null is returned, and the bitmap will eventually be returned to the
	 *         listener if the request is not cancelled.
	 */
	public synchronized Bitmap getBitmapWithScale(final String url, final ImageCacherListener imageCacherListener, final int sampleSize) {
		validateUrl(url);
		
		/*
		 * Logic for below:
		 * 
		 * 1. If currently requesting from the network, queue with that call.
		 * 
		 * 2. If currently requesting from the disk, queue with that call.
		 * 
		 * 3. If image is in memory, get the image from the memcache and return.
		 * 
		 * 4. If image is in disk:
		 * 
		 * a) Synchronously decode and return if synchronous disk cache is enabled
		 * 
		 * b) Queue for and start the asynchronous call.
		 * 
		 * 5. If we haven't done any of the above, queue for and start the network call.
		 */

		NetworkImageRequestListener networkImageRequestListener = getNewNetworkImageRequestListener(url, imageCacherListener, new SampleSizeFetcher() {
			@Override
			public int onSampleSizeRequired() throws FileNotFoundException {
				return sampleSize;
			}
		});

		if (!mNetworkInterface.queueIfDownloadingFromNetwork(url, networkImageRequestListener)) {
			Bitmap bitmap = null;
			try {
				bitmap = getBitmapFromMemoryOrDisk(url, imageCacherListener, sampleSize);
			} catch (FileNotFoundException e) {
				// We get to ignore this exception safely.
			} catch (FileFormatException e) {
				// We get to ignore this exception safely.
			}

			if (bitmap != null) {
				return bitmap;
			} else {
				loadImageFromNetwork(url, imageCacherListener, new SampleSizeFetcher() {
					@Override
					public int onSampleSizeRequired() {
						return sampleSize;
					}
				});
			}
		}
		return null;
	}

	// TODO: Trigger a network request for the image. We may want to move this into the database...
	// FIXME: What happens during the error condition?
	public Dimensions getImageDimensions(String url) throws FileNotFoundException {
		return mDiskCache.getImageDimensions(url);
	}

	private synchronized Bitmap getBitmapFromMemoryOrDisk(String url, ImageCacherListener imageCacherListener, int sampleSize) throws FileNotFoundException,
			FileFormatException {
		Bitmap bitmap;
		if ((bitmap = mMemoryCache.getBitmap(url, sampleSize)) != null) {
			mDiskCache.bump(url);
			return bitmap;
		} else if (mDiskCache.isCached(url)) {
			return loadImageFromDisk(url, imageCacherListener, sampleSize);
		} else {
			throw new FileNotFoundException();
		}
	}

	/**
	 * Cancels any asynchronous requests that are currently pending for the provided url that uses the provided listener.
	 * 
	 * @param url
	 * @param imageCacherListener
	 */
	public synchronized void cancelRequestForBitmap(String url, ImageCacherListener imageCacherListener) {
		validateUrl(url);

		if (imageCacherListener.networkRequestListener != null) {
			mNetworkInterface.cancelRequest(url, imageCacherListener.networkRequestListener);
		}
		// diskCache.cancelRequest(url, listener);
	}

	/**
	 * Caches the image at the provided url to disk. If the image is already on disk, it gets bumped on the eviction queue.
	 * 
	 * @param url
	 */
	public synchronized void precacheImage(String url) {
		validateUrl(url);

		// TODO: This is a race condition with this "isCached" call.
		if (!mDiskCache.isCached(url)) {
			// Request the image with a blank listener. We don't care what happens when it's done.
			mNetworkInterface.downloadImageToDisk(url, new NetworkImageRequestListener() {
				@Override
				public void onSuccess() {
				}

				@Override
				public void onFailure() {
				}
			});
		} else {
			mDiskCache.bump(url);
		}
	}

	private synchronized Bitmap loadImageFromDisk(final String url, final ImageCacherListener imageCacherListener, final int sampleSize)
			throws FileNotFoundException, FileFormatException {
		if (mDiskCache.synchronousDiskCacheEnabled()) {
			Bitmap bitmap = mDiskCache.getBitmapSynchronouslyFromDisk(url, sampleSize);
			mMemoryCache.cacheBitmap(bitmap, url, sampleSize);
			return bitmap;
		} else {
			mDiskCache.getBitmapAsynchronousFromDisk(url, sampleSize, new DiskCacherListener() {
				@Override
				public void onImageDecoded(Bitmap bitmap) {
					mMemoryCache.cacheBitmap(bitmap, url, sampleSize);
					imageCacherListener.onImageAvailable(bitmap);
				}
			});
			return null;
		}
	}

	private synchronized void loadImageFromNetwork(String url, ImageCacherListener imageCacherListener, SampleSizeFetcher sampleSizeFetcher) {
		NetworkImageRequestListener networkListener = getNewNetworkImageRequestListener(url, imageCacherListener, sampleSizeFetcher);
		imageCacherListener.networkRequestListener = networkListener;
		mNetworkInterface.downloadImageToDisk(url, networkListener);
	}

	private NetworkImageRequestListener getNewNetworkImageRequestListener(final String url, final ImageCacherListener imageCacherListener,
			final SampleSizeFetcher sampleSizeFetcher) {
		return new NetworkImageRequestListener() {
			@Override
			public void onSuccess() {
				onNetworkListenerSuccess(url, imageCacherListener, sampleSizeFetcher);
			}

			@Override
			public void onFailure() {
				imageCacherListener.onFailure("Failed to get image from the network!");
			}
		};
	}

	private synchronized void onNetworkListenerSuccess(final String url, final ImageCacherListener imageCacherListener,
			final SampleSizeFetcher sampleSizeFetcher) {
		Bitmap bitmap = null;
		try {
			bitmap = getBitmapFromMemoryOrDisk(url, imageCacherListener, sampleSizeFetcher.onSampleSizeRequired());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			imageCacherListener.onFailure("File not found after it was downloaded!");
		} catch (FileFormatException e) {
			e.printStackTrace();
			imageCacherListener.onFailure("File format exception after the file was downloaded!");
		}
		if (bitmap != null) {
			imageCacherListener.onImageAvailable(bitmap);
		}
	}

	private void validateUrl(String url) {
		if (url == null || url.length() == 0) {
			throw new IllegalArgumentException("Null URL passed into the image system.");
		}
	}

	private interface SampleSizeFetcher {
		public int onSampleSizeRequired() throws FileNotFoundException;
	}

	public void clearMemCache() {
		mMemoryCache.clearCache();
	}

	public void setMemCacheSize(int size) {
		mMemoryCache.setMaximumCacheSize(size);
	}

	public ImageMemoryCacherInterface getMemCacher() {
		return mMemoryCache;
	}

	public static abstract class ImageCacherListener {
		private NetworkImageRequestListener networkRequestListener;

		public abstract void onImageAvailable(Bitmap bitmap);

		public abstract void onFailure(String message);
	}
}
