package com.xtremelabs.imageutils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final NetworkToDiskInterface mNetworkToDiskInterface;
	private final ImageDownloadObserver mImageDownloadObserver;
	private final HashMap<String, ImageDownloadingRunnable> mUrlToRunnableMap = new HashMap<String, ImageDownloadingRunnable>();

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

	private synchronized void removeUrlFromMap(String url) {
		mUrlToRunnableMap.remove(url);
	}

	class ImageDownloadingRunnable implements Runnable {
		private final String mUrl;
		private boolean mFailed = false;
		private InputStream mInputStream = null;
		private HttpEntity mEntity;

		public ImageDownloadingRunnable(String url) {
			mUrl = url;
		}

		@Override
		public void run() {
			String errorMessage = null;
			try {
				executeNetworkRequest();
				passInputStreamToImageLoader();
			} catch (IOException e) {
				mFailed = true;
				errorMessage = "Failed to download image with error message: " + e.getMessage();
			} catch (IllegalArgumentException e) {
				mFailed = true;
				errorMessage = "Failed to download image with error message: " + e.getMessage();
			} catch (IllegalStateException e) {
				/*
				 * NOTE: If a bad URL is passed in (for example, mUrl = "N/A", the client.execute() call will throw an IllegalStateException. We do not want this exception to crash the app. Rather, we want to log the
				 * error and report a failure.
				 */
				Log.w(AbstractImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URL: " + mUrl);
				e.printStackTrace();
				errorMessage = "Failed to download image. A stack trace has been output to the logs. Message: " + e.getMessage();
				mFailed = true;
			} finally {
				try {
					if (mEntity != null) {
						mEntity.consumeContent();
					}
				} catch (IOException e) {
				}
				try {
					if (mInputStream != null) {
						mInputStream.close();
					}
				} catch (IOException e) {
				}
			}
			checkLoadCompleteAndRemoveListeners(errorMessage);
		}

		private synchronized void checkLoadCompleteAndRemoveListeners(String errorMessage) {
			removeUrlFromMap(mUrl);
			if (mFailed) {
				assert (errorMessage != null);
				mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
			} else {
				mImageDownloadObserver.onImageDownloaded(mUrl);
			}
		}

		public synchronized void executeNetworkRequest() throws ClientProtocolException, IOException {
			HttpClient client = new DefaultHttpClient();
			client.getConnectionManager().closeExpiredConnections();
			HttpUriRequest request = new HttpGet(mUrl);
			HttpResponse response = client.execute(request);
			;

			mEntity = response.getEntity();
			if (mEntity != null) {
				mInputStream = new BufferedInputStream(mEntity.getContent());
			}
			if (mEntity == null || mInputStream == null) {
				mFailed = true;
			}
			client.getConnectionManager().closeExpiredConnections();
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (mInputStream != null) {
				mNetworkToDiskInterface.downloadImageFromInputStream(mUrl, mInputStream);
			}
		}
	}

	@Override
	public synchronized boolean isNetworkRequestPendingForUrl(String url) {
		return mUrlToRunnableMap.containsKey(url);
	}
}
