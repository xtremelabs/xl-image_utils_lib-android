package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.xtremelabs.imageutils.DefaultImageDownloader.ImageDownloadingRunnable;

public class MappingManager {
	private final HashMap<String, List<NetworkImageRequestListener>> urlToListenersMap = new HashMap<String, List<NetworkImageRequestListener>>();
	private final HashMap<String, ImageDownloadingRunnable> urlToNetworkDownloadRunnableMap = new HashMap<String, ImageDownloadingRunnable>();

	public synchronized void addToListenerNewMap(String url, NetworkImageRequestListener onLoadComplete, ImageDownloadingRunnable runnable) {
		List<NetworkImageRequestListener> imageRequestListenerList = new ArrayList<NetworkImageRequestListener>();
		imageRequestListenerList.add(onLoadComplete);
		urlToListenersMap.put(url, imageRequestListenerList);
		urlToNetworkDownloadRunnableMap.put(url, runnable);
	}

	public synchronized boolean queueIfLoadingFromNetwork(String url, NetworkImageRequestListener onLoadComplete) {
		if (isRequestingUrlFromNetwork(url)) {
			urlToListenersMap.get(url).add(onLoadComplete);
			return true;
		}
		return false;
	}
	
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

	public synchronized List<NetworkImageRequestListener> removeListenersForUrl(String url) {
		urlToNetworkDownloadRunnableMap.remove(url);
		return urlToListenersMap.remove(url);
	}
	
	private boolean isRequestingUrlFromNetwork(String url) {
		return urlToListenersMap.containsKey(url);
	}
}
