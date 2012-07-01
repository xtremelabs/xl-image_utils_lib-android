package com.xtremelabs.imageutils;

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

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private NetworkToDiskInterface mNetworkToDiskInterface;
	private ImageDownloadObserver mImageDownloadObserver;
	private HashMap<String, ImageDownloadingRunnable> mUrlToRunnableMap = new HashMap<String, ImageDownloadingRunnable>();

	/*
	 * TODO: Research into lowering the number of available threads for the network
	 */
	private LifoThreadPool mThreadPool = new LifoThreadPool(8);

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
		mUrlToRunnableMap.put(url, runnable);
		mThreadPool.execute(runnable);
	}

	@Override
	public synchronized void cancelRequest(String url) {
		ImageDownloadingRunnable runnable = mUrlToRunnableMap.remove(url);
		runnable.cancel();
	}

	class ImageDownloadingRunnable implements Runnable {
		private String mUrl;
		private boolean mCancelled = false;
		private boolean failed = false;
		private InputStream mInputStream = null;

		public ImageDownloadingRunnable(String url) {
			mUrl = url;
		}

		@Override
		public void run() {
			try {
				executeNetworkRequest();
				passInputStreamToImageLoader();
			} catch (IOException e) {
				failed = true;
			}
			checkLoadCompleteAndRemoveListeners();
		}

		private synchronized void checkLoadCompleteAndRemoveListeners() {
			if (!mCancelled) {
				if (failed) {
					mImageDownloadObserver.onImageDownloadFailed(mUrl);
				} else {
					mImageDownloadObserver.onImageDownloaded(mUrl);
				}
			}
		}

		public synchronized void cancel() {
			mCancelled = true;
			if (mInputStream != null) {
				try {
					mInputStream.close();
				} catch (IOException e) {
				}
			}
		}

		public synchronized void executeNetworkRequest() throws ClientProtocolException, IOException {
			HttpClient client = new DefaultHttpClient();
			HttpUriRequest request = new HttpGet(mUrl);
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				mInputStream = entity.getContent();
			}
			if (entity == null || mInputStream == null) {
				failed = true;
			}
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (mInputStream != null) {
				mNetworkToDiskInterface.downloadImageFromInputStream(mUrl, mInputStream);
			}
		}
	}
}
