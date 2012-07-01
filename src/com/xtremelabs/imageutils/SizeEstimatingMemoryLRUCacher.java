package com.xtremelabs.imageutils;

import java.util.HashMap;
import java.util.LinkedList;

import android.graphics.Bitmap;

class SizeEstimatingMemoryLRUCacher implements ImageMemoryCacherInterface {
	private long mMaximumSizeInBytes = 18 * 1024 * 1024; // 12MB default
	private long mSize = 0;

	private HashMap<DecodeOperationParameters, Bitmap> mCache = new HashMap<DecodeOperationParameters, Bitmap>();
	private LinkedList<EvictionQueueContainer> mEvictionQueue = new LinkedList<EvictionQueueContainer>();

	@Override
	public synchronized Bitmap getBitmap(String url, int sampleSize) {
		DecodeOperationParameters params = new DecodeOperationParameters(url, sampleSize);
		Bitmap bitmap = mCache.get(params);
		if (bitmap != null) {
			onEntryHit(url, sampleSize);
			return bitmap;
		}
		return null;
	}

	@Override
	public synchronized void cacheBitmap(Bitmap bitmap, String url, int sampleSize) {
		DecodeOperationParameters params = new DecodeOperationParameters(url, sampleSize);
		mCache.put(params, bitmap);
		mSize += getBitmapSize(bitmap);
		onEntryHit(url, sampleSize);
	}

	@Override
	public synchronized void clearCache() {
		mSize = 0;
		mCache.clear();
		mEvictionQueue.clear();
	}

	@Override
	public synchronized void setMaximumCacheSize(long size) {
		mMaximumSizeInBytes = size;
		performEvictions();
	}
	
	private synchronized void onEntryHit(String url, int sampleSize) {
		EvictionQueueContainer container = new EvictionQueueContainer(url, sampleSize);

		if (mEvictionQueue.contains(container)) {
			mEvictionQueue.remove(container);
			mEvictionQueue.add(container);
		} else {
			mEvictionQueue.add(container);
			performEvictions();
		}
	}
	
	private synchronized void performEvictions() {
		while (mSize > mMaximumSizeInBytes) {
			EvictionQueueContainer container = mEvictionQueue.removeFirst();
			Bitmap bitmap = mCache.remove(new DecodeOperationParameters(container.getUrl(), container.getSampleSize()));
			mSize -= getBitmapSize(bitmap);
		}
	}
	
	private long getBitmapSize(Bitmap bitmap) {
		return bitmap.getWidth() * bitmap.getHeight() * 4;
	}
}
