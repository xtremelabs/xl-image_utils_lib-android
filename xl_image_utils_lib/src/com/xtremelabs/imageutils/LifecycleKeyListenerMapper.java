/*
 * Copyright 2012 Xtreme Labs
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
import java.util.List;

import com.xtremelabs.imageutils.LifecycleReferenceManager.ImageManagerCacheListener;

/**
 * This class maintains three sets of mappings which ensure that any Activity that is being destroyed has all references to it released.
 *
 * The {@link Object} "key" in this class refers to either an Activity or a Fragment.
 */
class LifecycleKeyListenerMapper {
	private HashMap<Object, List<ReferenceManagerListener>> mKeyToListenersMap = new HashMap<Object, List<ReferenceManagerListener>>();
	private HashMap<ReferenceManagerListener, ListenerInfo> mListenerToInfoMap = new HashMap<ReferenceManagerListener, ListenerInfo>();
	private HashMap<ImageManagerCacheListener, ReferenceManagerListener> mCacheListenerToImageReceivedListenerMap = new HashMap<ImageManagerCacheListener, ReferenceManagerListener>();

	public synchronized void registerNewListener(ReferenceManagerListener imageManagerListener, Object key, ImageManagerCacheListener customImageListener) {
		List<ReferenceManagerListener> imageManagerListenersList = mKeyToListenersMap.get(key);
		if (imageManagerListenersList == null) {
			imageManagerListenersList = new ArrayList<ReferenceManagerListener>();
			mKeyToListenersMap.put(key, imageManagerListenersList);
		}
		imageManagerListenersList.add(imageManagerListener);

		ListenerInfo info = new ListenerInfo(key, customImageListener);
		mListenerToInfoMap.put(imageManagerListener, info);

		mCacheListenerToImageReceivedListenerMap.put(customImageListener, imageManagerListener);
	}

	public synchronized ImageManagerCacheListener unregisterListener(ReferenceManagerListener imageManagerListener) {
		ListenerInfo info = mListenerToInfoMap.remove(imageManagerListener);
		if (info != null) {
			List<ReferenceManagerListener> listenerList = mKeyToListenersMap.get(info.mKey);
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

	public synchronized ReferenceManagerListener getAndRemoveListener(ImageManagerCacheListener cacheListener) {
		ReferenceManagerListener listener = mCacheListenerToImageReceivedListenerMap.get(cacheListener);
		if (listener != null) {
			unregisterListener(listener);
		}
		return listener;
	}

	public synchronized List<ReferenceManagerListener> removeAllEntriesForKey(Object key) {
		List<ReferenceManagerListener> listeners = mKeyToListenersMap.remove(key);
		if (listeners != null) {
			for (ReferenceManagerListener listener : listeners) {
				ListenerInfo info = mListenerToInfoMap.remove(listener);
				if (info != null) {
					mCacheListenerToImageReceivedListenerMap.remove(info.mCacheListener);
				}
			}
		}
		return listeners;
	}

	public synchronized boolean isListenerRegistered(ReferenceManagerListener imageManagerListener) {
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
