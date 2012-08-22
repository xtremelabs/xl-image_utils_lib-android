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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import android.graphics.Bitmap;

// TODO: Research into using the official Android LRU.
class SizeEstimatingMemoryLRUCacher implements ImageMemoryCacherInterface {
	private long mMaximumSizeInBytes = 6 * 1024 * 1024; // 6MB default
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
			try {
				EvictionQueueContainer container = mEvictionQueue.removeFirst();
				Bitmap bitmap = mCache.remove(new DecodeOperationParameters(container.getUrl(), container.getSampleSize()));
				mSize -= getBitmapSize(bitmap);
			} catch (NoSuchElementException e) {
				mSize = 0;
			}
		}
	}

	private long getBitmapSize(Bitmap bitmap) {
		return bitmap.getWidth() * bitmap.getHeight() * 4;
	}
}
