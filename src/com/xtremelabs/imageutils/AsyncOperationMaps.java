package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

class AsyncOperationMaps {
	private HashMap<String, List<ImageCacherListener>> mUrlToListenersMapForNetwork = new HashMap<String, List<ImageCacherListener>>();
	private HashMap<ImageCacherListener, String> mListenerToUrlMapForNetwork = new HashMap<ImageCacher.ImageCacherListener, String>();
	
	private HashMap<DiskOperationParameters, List<ImageCacherListener>> mDiskParamsToListenersMapForDisk = new HashMap<AsyncOperationMaps.DiskOperationParameters, List<ImageCacherListener>>();
	private HashMap<ImageCacherListener, DiskOperationParameters> mListenerToDiskParamsMap = new HashMap<ImageCacher.ImageCacherListener, AsyncOperationMaps.DiskOperationParameters>();
	
	public synchronized void registerListenerForNetworkRequest(ImageCacherListener listener, String url) {
		List<ImageCacherListener> listeners = mUrlToListenersMapForNetwork.get(url);
		if (listeners == null) {
			listeners = new ArrayList<ImageCacher.ImageCacherListener>();
			mUrlToListenersMapForNetwork.put(url, listeners);
		}
		listeners.add(listener);
		
		mListenerToUrlMapForNetwork.put(listener, url);
	}
	
	private class DiskOperationParameters {
		private String url;
		private int sampleSize;
		
		@Override
		public int hashCode() {
			int hash;
			hash = 31 * sampleSize + 17;
			hash += url.hashCode();
			return hash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DiskOperationParameters)) {
				return false;
			}
			
			DiskOperationParameters otherObject = (DiskOperationParameters) o;
			if (otherObject.sampleSize == sampleSize && otherObject.url.equals(url)) {
				return true;
			}
			return false;
		}
	}
}
