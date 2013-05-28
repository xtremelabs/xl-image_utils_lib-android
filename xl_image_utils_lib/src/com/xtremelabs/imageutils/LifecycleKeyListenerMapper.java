/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.LifecycleReferenceManager.ImageManagerCacheListener;

// TODO The locking in this class (and the fact that it uses three hashmaps) makes it very slow. Find a way to improve the performance of this class.
/**
 * This class maintains three sets of mappings which ensure that any Activity that is being destroyed has all references to it released.
 * 
 * The {@link Object} "key" in this class refers to either an Activity or a Fragment.
 */
class LifecycleKeyListenerMapper {
	private final Map<Object, Set<ImageManagerListener>> mKeyToListenersMap = new HashMap<Object, Set<ImageManagerListener>>();
	private final Map<ImageManagerListener, ListenerInfo> mListenerToInfoMap = new HashMap<ImageManagerListener, ListenerInfo>();
	private final Map<ImageManagerCacheListener, ImageManagerListener> mCacheListenerToImageReceivedListenerMap = new HashMap<ImageManagerCacheListener, ImageManagerListener>();

	public synchronized void registerNewListener(ImageManagerListener imageManagerListener, Object key, ImageManagerCacheListener customImageListener) {
		Set<ImageManagerListener> imageManagerListenersList = mKeyToListenersMap.get(key);
		if (imageManagerListenersList == null) {
			imageManagerListenersList = new HashSet<ImageManagerListener>();
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
			Set<ImageManagerListener> listenerSet = mKeyToListenersMap.get(info.mKey);
			if (listenerSet != null) {
				listenerSet.remove(imageManagerListener);
				if (listenerSet.size() == 0) {
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

	public synchronized List<ImageManagerListener> removeAndCancelAllRequestsByKey(ImageCacher imageCacher, Object key) {
		Set<ImageManagerListener> listeners = mKeyToListenersMap.remove(key);
		List<ImageManagerListener> listOfCancelledListeners = null;
		if (listeners != null) {
			listOfCancelledListeners = new ArrayList<ImageManagerListener>(listeners.size());
			for (ImageManagerListener listener : listeners) {
				listOfCancelledListeners.add(listener);
				ListenerInfo info = mListenerToInfoMap.remove(listener);
				if (info != null) {
					ImageCacherListener imageCacherListener = info.mCacheListener;
					mCacheListenerToImageReceivedListenerMap.remove(imageCacherListener);
					imageCacher.cancelRequestForBitmap(imageCacherListener);
				}
			}
		}
		return listOfCancelledListeners;
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
