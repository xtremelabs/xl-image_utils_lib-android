package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.xtremelabs.imageutils.ImageManager.CustomImageListener;

public class ImageListenerMapper {
	private HashMap<String, List<ImageReceivedListener>> urlToListenersMap = new HashMap<String, List<ImageReceivedListener>>();
	private HashMap<Object, List<ImageReceivedListener>> objectToListenersMap = new HashMap<Object, List<ImageReceivedListener>>();
	private HashMap<ImageReceivedListener, ListenerInfo> listenerToInfoMap = new HashMap<ImageReceivedListener, ListenerInfo>();

	public synchronized void registerNewListener(ImageReceivedListener listener, Object object) {
		List<ImageReceivedListener> list = objectToListenersMap.get(object);
		if (list == null) {
			list = new ArrayList<ImageReceivedListener>();
			objectToListenersMap.put(object, list);
		}

		ListenerInfo info = new ListenerInfo();
		info.urlsToCustomImageListenersMap = new HashMap<String, HashSet<CustomImageListener>>();
		info.object = object;
		listenerToInfoMap.put(listener, info);
	}

	public synchronized void linkUrlToListener(String url, CustomImageListener imageListener, ImageReceivedListener listener) {
		List<ImageReceivedListener> listeners = urlToListenersMap.get(url);
		if (listeners == null) {
			listeners = new ArrayList<ImageReceivedListener>();
			urlToListenersMap.put(url, listeners);
		}
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}

		ListenerInfo info = listenerToInfoMap.get(listener);
		if (info.urlsToCustomImageListenersMap.containsKey(url)) {
			info.urlsToCustomImageListenersMap.get(url).add(imageListener);
		} else {
			HashSet<CustomImageListener> listenerSet = new HashSet<CustomImageListener>();
			listenerSet.add(imageListener);
			info.urlsToCustomImageListenersMap.put(url, listenerSet);
		}
	}

	public synchronized List<ImageReceivedListener> getAndRemoveListenersForUrl(String url) {
		List<ImageReceivedListener> listenersToRemove = new ArrayList<ImageReceivedListener>();

		List<ImageReceivedListener> list = urlToListenersMap.remove(url);
		if (list != null) {
			for (ImageReceivedListener listener : list) {
				ListenerInfo info = listenerToInfoMap.get(listener);
				if (info != null && info.urlsToCustomImageListenersMap != null) {
					info.urlsToCustomImageListenersMap.remove(url);
					if (info.urlsToCustomImageListenersMap.isEmpty()) {
						listenersToRemove.add(listener);
					}
				}
			}
		}

		for (ImageReceivedListener listener : listenersToRemove) {
			ListenerInfo info = listenerToInfoMap.get(listener);
			Object object = info.object;
			List<ImageReceivedListener> objectListeners = objectToListenersMap.get(object);

			if (objectListeners != null) {
				objectListeners.remove(listener);
				if (objectListeners.isEmpty()) {
					objectToListenersMap.remove(object);
				}

			}
			listenerToInfoMap.remove(listener);
		}

		return list;
	}

	public synchronized void removeAllEntriesForObject(Object object) {
		List<ImageReceivedListener> listeners = objectToListenersMap.remove(object);
		if (listeners != null) {
			for (ImageReceivedListener listener : listeners) {
				removeReferencesForListener(listener);
			}
		}
	}

	private void removeReferencesForListener(ImageReceivedListener listener) {
		ListenerInfo info = listenerToInfoMap.remove(listener);
		if (info != null && info.urlsToCustomImageListenersMap != null) {
			for (String url : info.urlsToCustomImageListenersMap.keySet()) {
				cancelAllRequestsForListenerInfoAndUrl(info, url);

				List<ImageReceivedListener> urlListeners = urlToListenersMap.get(url);
				urlListeners.remove(listener);
				if (urlListeners.size() == 0) {
					urlToListenersMap.remove(url);
				}
			}
		}
	}

	private void cancelAllRequestsForListenerInfoAndUrl(ListenerInfo info, String url) {
		HashSet<CustomImageListener> customImageListenerSet = info.urlsToCustomImageListenersMap.get(url);
		for (CustomImageListener customImageListener : customImageListenerSet) {
			customImageListener.setCancelled(true);
		}
	}

	public synchronized boolean isListenerRegistered(ImageReceivedListener listener) {
		return listenerToInfoMap.containsKey(listener);
	}

	private class ListenerInfo {
		private Object object;
		private HashMap<String, HashSet<CustomImageListener>> urlsToCustomImageListenersMap;
	}
}
