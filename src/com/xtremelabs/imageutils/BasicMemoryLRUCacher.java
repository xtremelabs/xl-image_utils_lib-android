package com.xtremelabs.imageutils;

import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;

class BasicMemoryLRUCacher implements ImageMemoryCacherInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageMemoryLRUCacher";
	private int mMaximumCacheEntries = 10;

	private HashMap<String, HashMap<Integer, Bitmap>> mCache = new HashMap<String, HashMap<Integer, Bitmap>>();
	private LinkedList<EvictionQueueContainer> mEvictionQueue = new LinkedList<EvictionQueueContainer>();
	
	@Override
	public synchronized Bitmap getBitmap(String url, int sampleSize) {
		HashMap<Integer, Bitmap> map = mCache.get(url);
		if (map != null) {
			Bitmap bitmap = map.get(sampleSize);
			if (bitmap != null) {
				onEntryHit(url, sampleSize);
				return bitmap;
			}
		}
		return null;
	}

	@Override
	public synchronized void cacheBitmap(Bitmap bitmap, String url, int sampleSize) {
		HashMap<Integer, Bitmap> map = mCache.get(url);
		if (map == null) {
			map = new HashMap<Integer, Bitmap>();
			mCache.put(url, map);
		}
		map.put(sampleSize, bitmap);
		onEntryHit(url, sampleSize);
	}

	@Override
	public synchronized void clearCache() {
		mCache.clear();
		mEvictionQueue.clear();
	}
	
	@Override
	public synchronized void setMaximumCacheSize(long size) {
		mMaximumCacheEntries = (int) size;
		evictFromQueue(Math.max(mEvictionQueue.size() - mMaximumCacheEntries, 0));
	}

	private void onEntryHit(String url, int sampleSize) {
		EvictionQueueContainer container = new EvictionQueueContainer(url, sampleSize);

		if (mEvictionQueue.contains(container)) {
			mEvictionQueue.remove(container);
			mEvictionQueue.add(container);
		} else {
			evictFromQueue(Math.max(mEvictionQueue.size() - mMaximumCacheEntries + 1, 0));
			mEvictionQueue.add(container);
		}
	}

	private void evictFromQueue(int numEvictions) {
		for (int i = 0; i < numEvictions && !mEvictionQueue.isEmpty(); i++) {
			EvictionQueueContainer container = mEvictionQueue.removeFirst();
			HashMap<Integer, Bitmap> map = mCache.get(container.getUrl());
			if (map != null) {
				if (map.containsKey(container.getSampleSize())) {
					map.remove(container.getSampleSize());
					if (map.size() == 0) {
						mCache.remove(container.getUrl());
					}
				}
			}
		}
	}
}
