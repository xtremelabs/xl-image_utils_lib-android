package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

public class DefaultImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final MappingManager mappingManager = new MappingManager();

	private Handler handler;
	private ImageInputStreamLoader inputStreamLoader;

	public DefaultImageDownloader(Context applicationContext, ImageInputStreamLoader inputStreamLoader) {
		handler = new Handler(applicationContext.getMainLooper());
		this.inputStreamLoader = inputStreamLoader;
	}

	@Override
	public synchronized void loadImageToDisk(final String url, final NetworkImageRequestListener onLoadComplete) {
		if (mappingManager.queueIfLoadingFromNetwork(url, onLoadComplete)) {
			return;
		}
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(url);
		mappingManager.addToListenerNewMap(url, onLoadComplete, runnable);
		ThreadPool.execute(runnable);
	}

	@Override
	public synchronized void cancelRequest(String url, NetworkImageRequestListener listener) {
		mappingManager.cancelRequest(url, listener);
	}

	private void imageLoadComplete(String url) {
		final List<NetworkImageRequestListener> listeners = mappingManager.retrieveListeners(url);
		if (listeners != null) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					for (NetworkImageRequestListener listener : listeners) {
						listener.onSuccess();
					}
				}
			});
		}
	}

	@Override
	public boolean queueIfLoadingFromNetwork(String url, NetworkImageRequestListener onLoadComplete) {
		return mappingManager.queueIfLoadingFromNetwork(url, onLoadComplete);
	}

	public ImageDownloadingRunnable getImageDownloadingRunnable(String url) {
		return new ImageDownloadingRunnable(url);
	}

	public void removeAllListenersForUrl(String url) {
		final List<NetworkImageRequestListener> listeners = mappingManager.removeListenersForUrl(url);
		if (listeners != null) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					for (NetworkImageRequestListener listener : listeners) {
						listener.onFailure();
					}
					return null;
				}
			}.execute();
		}
	}

	public class ImageDownloadingRunnable implements Runnable {
		private String url;
		private boolean cancelled = false;
		private InputStream inputStream = null;

		public ImageDownloadingRunnable(String url) {
			this.url = url;
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
			if (!cancelled) {
				imageLoadComplete(url);
			}
			removeAllListenersForUrl(url);
		}

		public synchronized void cancel() {
			cancelled = true;
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public synchronized void executeNetworkRequest() throws ClientProtocolException, IOException {
			if (cancelled) {
				removeAllListenersForUrl(url);
				return;
			}
			inputStream = new URL(url).openStream();
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (inputStream != null) {
				inputStreamLoader.loadImageFromInputStream(url, inputStream);
			}
		}
	}
}
