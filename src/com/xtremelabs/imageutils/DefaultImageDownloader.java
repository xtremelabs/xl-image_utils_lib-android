package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

public class DefaultImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final MappingManager mMappingManager = new MappingManager();

	private NetworkToDiskInterface mNetworkToDiskInterface;

	public DefaultImageDownloader(NetworkToDiskInterface networkToDiskInterface) {
		mNetworkToDiskInterface = networkToDiskInterface;
	}

	@Override
	public synchronized boolean queueIfDownloadingFromNetwork(String url, NetworkImageRequestListener onLoadComplete) {
		return mMappingManager.queueIfDownloadingFromNetwork(url, onLoadComplete);
	}

	@Override
	public synchronized void downloadImageToDisk(final String url, final NetworkImageRequestListener onLoadComplete) {
		if (mMappingManager.queueIfDownloadingFromNetwork(url, onLoadComplete)) {
			return;
		}
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(url);
		mMappingManager.addToListenerNewMap(url, onLoadComplete, runnable);
		ThreadPool.execute(runnable);
	}

	@Override
	public void cancelRequest(String url, NetworkImageRequestListener listener) {
		synchronized (listener) {
			mMappingManager.cancelRequest(url, listener);
		}
	}

	private void onDownloadSucceeded(String url) {
		final List<NetworkImageRequestListener> listeners = mMappingManager.getListenersForUrl(url);
		if (listeners != null) {
			int size;
			NetworkImageRequestListener listener;
			while ((size = listeners.size()) != 0) {
				listener
				synchronized (listeners) {
					listeners.remove(location)
				}
			}
		}
	}

	private void onDownloadFailed(String url) {
		final List<NetworkImageRequestListener> listeners = mMappingManager.removeListenersForUrl(url);
		if (listeners != null) {
			ThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					for (NetworkImageRequestListener listener : listeners) {
						listener.onFailure();
					}
				}
			});
		}
	}

	private synchronized void onDownloadCancelled(String url) {
		mMappingManager.removeListenersForUrl(url);
	}

	public class ImageDownloadingRunnable implements Runnable {
		private String mUrl;
		private boolean mCancelled = false;
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
				e.printStackTrace();
			}
			checkLoadCompleteAndRemoveListeners();
		}

		private synchronized void checkLoadCompleteAndRemoveListeners() {
			if (!mCancelled) {
				onDownloadSucceeded(mUrl);
			}
			onDownloadFailed(mUrl);
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
			if (mCancelled) {
				onDownloadFailed(mUrl);
				return;
			}
			mInputStream = new URL(mUrl).openStream();
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (mInputStream != null) {
				mNetworkToDiskInterface.downloadImageFromInputStream(mUrl, mInputStream);
			}
		}
	}
}
