package com.xtremelabs.imageutils;

import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;

public class DefaultImageMemoryLRUCacher implements ImageMemoryCacherInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageMemoryLRUCacher";
	private int maximumCacheEntries = 3;

	private HashMap<String, HashMap<Integer, Bitmap>> memcache = new HashMap<String, HashMap<Integer, Bitmap>>();
	private LinkedList<EvictionQueueContainer> evictionQueue = new LinkedList<EvictionQueueContainer>();
	
	@Override
	public synchronized boolean isCached(String url, int sampleSize) {
		HashMap<Integer, Bitmap> images = memcache.get(url);
		if (images != null) {
			return images.containsKey(sampleSize);
		} else {
			return false;
		}
	}

	@Override
	public synchronized Bitmap getBitmap(String url, int sampleSize) {
		HashMap<Integer, Bitmap> map = memcache.get(url);
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
		HashMap<Integer, Bitmap> map = memcache.get(url);
		if (map == null) {
			map = new HashMap<Integer, Bitmap>();
			memcache.put(url, map);
		}
		map.put(sampleSize, bitmap);
		onEntryHit(url, sampleSize);
	}

	@Override
	public synchronized void clearCache() {
		memcache.clear();
		evictionQueue.clear();
	}

	@Override
	public synchronized void setMaximumCacheSize(int numImages) {
		maximumCacheEntries = numImages;
		evictFromQueue(Math.max(evictionQueue.size() - maximumCacheEntries, 0));
	}

	private void onEntryHit(String url, int sampleSize) {
		EvictionQueueContainer container = new EvictionQueueContainer(url, sampleSize);

		if (evictionQueue.contains(container)) {
			evictionQueue.remove(container);
			evictionQueue.add(container);
		} else {
			evictFromQueue(Math.max(evictionQueue.size() - maximumCacheEntries + 1, 0));
			evictionQueue.add(container);
		}
	}

	private void evictFromQueue(int numEvictions) {
		for (int i = 0; i < numEvictions && !evictionQueue.isEmpty(); i++) {
			EvictionQueueContainer container = evictionQueue.removeFirst();
			HashMap<Integer, Bitmap> map = memcache.get(container.getUrl());
			if (map != null) {
				if (map.containsKey(container.getSampleSize())) {
					map.remove(container.getSampleSize());
					if (map.size() == 0) {
						memcache.remove(container.getUrl());
					}
				}
			}
		}
	}
}
