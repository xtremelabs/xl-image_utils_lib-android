package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

public class DefaultImageDownloader implements ImageNetworkInterface {
	private final HashMap<String, List<NetworkImageRequestListener>> urlToListenersMap = new HashMap<String, List<NetworkImageRequestListener>>();
	private final HashMap<String, ImageDownloadingRunnable> urlToNetworkDownloadRunnableMap = new HashMap<String, ImageDownloadingRunnable>();
	private ImageInputStreamLoader imageLoader;
	
	public DefaultImageDownloader(ImageInputStreamLoader imageLoader) {
		this.imageLoader = imageLoader;
	}

	@Override
	public synchronized void loadImageToDisk(final String url, final NetworkImageRequestListener onComplete) {
		if (isRequestingUrlFromNetwork(url)) {
			urlToListenersMap.get(url).add(onComplete);
			return;
		}

		List<NetworkImageRequestListener> imageRequestListenerList = new ArrayList<NetworkImageRequestListener>();
		imageRequestListenerList.add(onComplete);
		urlToListenersMap.put(url, imageRequestListenerList);

		getImageFromInternet(url);
	}

	@Override
	public synchronized void cancelRequest(String url, NetworkImageRequestListener listener) {
		List<NetworkImageRequestListener> listenerList = urlToListenersMap.get(url);
		if (listenerList != null) {
			if (listenerList.contains(listener)) {
				listenerList.remove(listener);
			}

			if (listenerList.size() == 0) {
				urlToListenersMap.remove(url);
				urlToNetworkDownloadRunnableMap.remove(url).cancel();
			}
		}
	}

	private void getImageFromInternet(final String url) {
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(url);
		urlToNetworkDownloadRunnableMap.put(url, runnable);
		ThreadPool.execute(runnable);
	}

	private synchronized void imageLoadComplete(String url) {
		List<NetworkImageRequestListener> listeners = urlToListenersMap.remove(url);
		if (listeners != null) {
			for (NetworkImageRequestListener listener : listeners) {
				listener.onSuccess();
			}
		}
	}

	// FIXME: Bad name or functionality.
	private synchronized void removeAllListenersForRequest(String url) {
		List<NetworkImageRequestListener> listeners = urlToListenersMap.remove(url);
		if (listeners != null) {
			for (NetworkImageRequestListener listener : listeners) {
				listener.onFailure();
			}
		}
		urlToNetworkDownloadRunnableMap.remove(url);
	}

	private synchronized boolean isRequestingUrlFromNetwork(String url) {
		return urlToListenersMap.containsKey(url);
	}
	
	public ImageDownloadingRunnable getImageDownloadingRunnable(String url) {
		return new ImageDownloadingRunnable(url);
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
				synchronized (this) {
					if (!cancelled) {
						imageLoadComplete(url);
					}
					removeAllListenersForRequest(url);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			synchronized (this) {
				cancelled = true;
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public void executeNetworkRequest() throws ClientProtocolException, IOException {
			synchronized (this) {
				if (cancelled) {
					removeAllListenersForRequest(url);
					return;
				}
				inputStream = new URL(url).openStream();
			}
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (inputStream != null) {
				imageLoader.loadImageFromInputStream(url, inputStream);
			}
		}
	}
}
