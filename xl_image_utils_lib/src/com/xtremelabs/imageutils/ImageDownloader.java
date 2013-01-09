package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import android.util.Log;

import com.xtremelabs.imageutils.NetworkRequestCreator.InputStreamListener;

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final NetworkToDiskInterface mNetworkToDiskInterface;
	private final ImageDownloadObserver mImageDownloadObserver;
	private final HashMap<String, ImageDownloadingRunnable> mUrlToRunnableMap = new HashMap<String, ImageDownloadingRunnable>();
	private NetworkRequestCreator mNetworkRequestCreator = new DefaultNetworkRequestCreator();

	/*
	 * TODO: Research into lowering the number of available threads for the network
	 */
	private final LifoThreadPool mThreadPool = new LifoThreadPool(3);

	public ImageDownloader(NetworkToDiskInterface networkToDiskInterface, ImageDownloadObserver imageDownloadObserver) {
		mNetworkToDiskInterface = networkToDiskInterface;
		mImageDownloadObserver = imageDownloadObserver;
	}

	@Override
	public synchronized void bump(String url) {
		ImageDownloadingRunnable runnable = mUrlToRunnableMap.get(url);
		if (runnable != null) {
			mThreadPool.bump(runnable);
		}
	}

	@Override
	public synchronized void downloadImageToDisk(final String url) {
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(url);
		if (!mUrlToRunnableMap.containsKey(url)) {
			mUrlToRunnableMap.put(url, runnable);
			mThreadPool.execute(runnable);
		}
	}

	@Override
	public synchronized void setNetworkRequestCreator(NetworkRequestCreator networkRequestCreator) {
		if (networkRequestCreator == null) {
			mNetworkRequestCreator = new DefaultNetworkRequestCreator();
		} else {
			mNetworkRequestCreator = networkRequestCreator;
		}
	}

	private synchronized void removeUrlFromMap(String url) {
		mUrlToRunnableMap.remove(url);
	}

	class ImageDownloadingRunnable implements Runnable {
		private final String mUrl;

		public ImageDownloadingRunnable(String url) {
			mUrl = url;
		}

		@Override
		public void run() {
			try {
				mNetworkRequestCreator.getInputStream(mUrl, new InputStreamListener() {
					@Override
					public void onInputStreamReady(InputStream inputStream) {
						String errorMessage = loadInputStreamToDisk(inputStream);
						removeUrlFromMap(mUrl);
						if (errorMessage != null) {
							mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
						} else {
							mImageDownloadObserver.onImageDownloaded(mUrl);
						}
					}

					@Override
					public void onFailure(String errorMessage) {
						removeUrlFromMap(mUrl);
						mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
					}
				});
			} catch (IllegalStateException e) {
				reportIllegalStateExceptionLoadFailure(e);
			}
		}

		private void reportIllegalStateExceptionLoadFailure(IllegalStateException e) {
			/*
			 * NOTE: If a bad URL is passed in (for example, mUrl = "N/A", the client.execute() call will throw an IllegalStateException. We do not want this exception to crash the app. Rather, we want to log the error
			 * and report a failure.
			 */
			Log.w(AbstractImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URL: " + mUrl);
			e.printStackTrace();
			String errorMessage = "Failed to download image. A stack trace has been output to the logs. Message: " + e.getMessage();
			mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
		}

		private String loadInputStreamToDisk(InputStream inputStream) {
			String errorMessage = null;
			if (inputStream != null) {
				try {
					mNetworkToDiskInterface.downloadImageFromInputStream(mUrl, inputStream);
				} catch (IOException e) {
					errorMessage = "IOException when downloading image: " + mUrl + ", Exception type: " + e.getClass().getName() + ", Exception message: " + e.getMessage();
				} catch (IllegalArgumentException e) {
					errorMessage = "Failed to download image with error message: " + e.getMessage();
				} catch (IllegalStateException e) {
					/*
					 * NOTE: If a bad URL is passed in (for example, mUrl = "N/A", the client.execute() call will throw an IllegalStateException. We do not want this exception to crash the app. Rather, we want to log the
					 * error and report a failure.
					 */
					Log.w(AbstractImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URL: " + mUrl);
					e.printStackTrace();
					errorMessage = "Failed to download image. A stack trace has been output to the logs. Message: " + e.getMessage();
				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (IOException e) {
					}
				}
			}
			return errorMessage;
		}
	}

	@Override
	public synchronized boolean isNetworkRequestPendingForUrl(String url) {
		return mUrlToRunnableMap.containsKey(url);
	}
}
