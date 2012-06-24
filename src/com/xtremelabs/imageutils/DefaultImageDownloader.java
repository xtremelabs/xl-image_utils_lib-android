package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;

import android.util.Log;

public class DefaultImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

//	private final MappingManager mMappingManager = new MappingManager();

	private NetworkToDiskInterface mNetworkToDiskInterface;
	private ImageDownloadObserver mImageDownloadObserver;
	
	private ThreadPool mThreadPool = new ThreadPool(6);

	public DefaultImageDownloader(NetworkToDiskInterface networkToDiskInterface, ImageDownloadObserver imageDownloadObserver) {
		mNetworkToDiskInterface = networkToDiskInterface;
		mImageDownloadObserver = imageDownloadObserver;
	}

	@Override
	public void downloadImageToDisk(final String url) {
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(url);
//		mMappingManager.addToListenerNewMap(url, onLoadComplete, runnable);
		mThreadPool.execute(runnable);
	}

	@Override
	public void cancelRequest(String url) {
	}

	public class ImageDownloadingRunnable implements Runnable {
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
				e.printStackTrace();
				Log.d(TAG, "Failed to download the image! Error message: " + e.getMessage());
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
					e.printStackTrace();
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
