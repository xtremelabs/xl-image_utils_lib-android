package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private NetworkToDiskInterface mNetworkToDiskInterface;
	private ImageDownloadObserver mImageDownloadObserver;
	private HashMap<String, ImageDownloadingRunnable> mUrlToRunnableMap = new HashMap<String, ImageDownloadingRunnable>();
	
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
		mUrlToRunnableMap.put(url,  runnable);
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
			mInputStream = new URL(mUrl).openStream();
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (mInputStream != null) {
				mNetworkToDiskInterface.downloadImageFromInputStream(mUrl, mInputStream);
			}
		}
	}
}
