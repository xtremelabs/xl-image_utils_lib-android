package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.xtremelabs.imageutils.ImageManager.CacheListener;

class ImageListenerMapper {
	private HashMap<Object, List<ImageReceivedListener>> keyToListenersMap = new HashMap<Object, List<ImageReceivedListener>>();
	private HashMap<ImageReceivedListener, ListenerInfo> listenerToInfoMap = new HashMap<ImageReceivedListener, ListenerInfo>();
	private HashMap<CacheListener, ImageReceivedListener> cacheListenerToImageReceivedListenerMap = new HashMap<ImageManager.CacheListener, ImageReceivedListener>();

	public synchronized void registerNewListener(ImageReceivedListener imageReceivedListener, Object key, CacheListener customImageListener) {
		List<ImageReceivedListener> list = keyToListenersMap.get(key);
		if (list == null) {
			list = new ArrayList<ImageReceivedListener>();
			keyToListenersMap.put(key, list);
		}
		list.add(imageReceivedListener);
		
		ListenerInfo info = new ListenerInfo(key, customImageListener);
		listenerToInfoMap.put(imageReceivedListener, info);
		
		cacheListenerToImageReceivedListenerMap.put(customImageListener, imageReceivedListener);
	}

	public synchronized boolean unregisterListener(ImageReceivedListener listener) {
		ListenerInfo info = listenerToInfoMap.remove(listener);
		if (info != null) {
			List<ImageReceivedListener> listenerList = keyToListenersMap.get(info.key);
			if (listenerList != null) {
				listenerList.remove(listener);
				if (listenerList.size() == 0) {
					keyToListenersMap.remove(info.key);
				}
			}
			
			cacheListenerToImageReceivedListenerMap.remove(info.cacheListener);
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized ImageReceivedListener getAndRemoveListener(CacheListener cacheListener) {
		ImageReceivedListener listener = cacheListenerToImageReceivedListenerMap.get(cacheListener);
		if (listener != null) {
			unregisterListener(listener);
		}
		return listener;
	}

	public synchronized void removeAllEntriesForKey(Object key) {
		List<ImageReceivedListener> listeners = keyToListenersMap.remove(key);
		if (listeners != null) {
			for (ImageReceivedListener listener : listeners) {
				ListenerInfo info = listenerToInfoMap.remove(listener);
				if (info != null) {
					cacheListenerToImageReceivedListenerMap.remove(info.cacheListener);
				}
			}
		}
	}

	public synchronized boolean isListenerRegistered(ImageReceivedListener listener) {
		return listenerToInfoMap.containsKey(listener);
	}
	
	private class ListenerInfo {
		Object key;
		CacheListener cacheListener;
		
		ListenerInfo(Object key, CacheListener cacheListener) {
			this.key = key;
			this.cacheListener = cacheListener;
		}
	}
}
