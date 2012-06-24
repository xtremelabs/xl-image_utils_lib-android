package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.xtremelabs.imageutils.LifecycleReferenceManager.ImageManagerCacheListener;

class LifecycleKeyListenerMapper {
	private HashMap<Object, List<ImageManagerListener>> mKeyToListenersMap = new HashMap<Object, List<ImageManagerListener>>();
	private HashMap<ImageManagerListener, ListenerInfo> mListenerToInfoMap = new HashMap<ImageManagerListener, ListenerInfo>();
	private HashMap<ImageManagerCacheListener, ImageManagerListener> mCacheListenerToImageReceivedListenerMap = new HashMap<ImageManagerCacheListener, ImageManagerListener>();

	public synchronized void registerNewListener(ImageManagerListener imageManagerListener, Object key, ImageManagerCacheListener customImageListener) {
		List<ImageManagerListener> imageManagerListenersList = mKeyToListenersMap.get(key);
		if (imageManagerListenersList == null) {
			imageManagerListenersList = new ArrayList<ImageManagerListener>();
			mKeyToListenersMap.put(key, imageManagerListenersList);
		}
		imageManagerListenersList.add(imageManagerListener);
		
		ListenerInfo info = new ListenerInfo(key, customImageListener);
		mListenerToInfoMap.put(imageManagerListener, info);
		
		mCacheListenerToImageReceivedListenerMap.put(customImageListener, imageManagerListener);
	}

	public synchronized ImageManagerCacheListener unregisterListener(ImageManagerListener imageManagerListener) {
		ListenerInfo info = mListenerToInfoMap.remove(imageManagerListener);
		if (info != null) {
			List<ImageManagerListener> listenerList = mKeyToListenersMap.get(info.mKey);
			if (listenerList != null) {
				listenerList.remove(imageManagerListener);
				if (listenerList.size() == 0) {
					mKeyToListenersMap.remove(info.mKey);
				}
			}
			mCacheListenerToImageReceivedListenerMap.remove(info.mCacheListener);
			return info.mCacheListener;
		} else {
			return null;
		}
	}
	
	public synchronized ImageManagerListener getAndRemoveListener(ImageManagerCacheListener cacheListener) {
		ImageManagerListener listener = mCacheListenerToImageReceivedListenerMap.get(cacheListener);
		if (listener != null) {
			unregisterListener(listener);
		}
		return listener;
	}

	public synchronized void removeAllEntriesForKey(Object key) {
		List<ImageManagerListener> listeners = mKeyToListenersMap.remove(key);
		if (listeners != null) {
			for (ImageManagerListener listener : listeners) {
				ListenerInfo info = mListenerToInfoMap.remove(listener);
				if (info != null) {
					mCacheListenerToImageReceivedListenerMap.remove(info.mCacheListener);
				}
			}
		}
	}

	public synchronized boolean isListenerRegistered(ImageManagerListener imageManagerListener) {
		return mListenerToInfoMap.containsKey(imageManagerListener);
	}
	
	private class ListenerInfo {
		Object mKey;
		ImageManagerCacheListener mCacheListener;
		
		ListenerInfo(Object key, ImageManagerCacheListener cacheListener) {
			this.mKey = key;
			this.mCacheListener = cacheListener;
		}
	}
}
