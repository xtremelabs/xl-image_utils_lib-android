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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

@SuppressLint("NewApi")
public class AdvancedMemoryLRUCacher implements ImageMemoryCacherInterface {
	private long mMaximumSizeInBytes = 20 * 1024 * 1024; // 20MB default
	private long mSize = 0;

	private final HashMap<DecodeOperationParameters, Bitmap> mCache = new HashMap<DecodeOperationParameters, Bitmap>();
	private final LinkedList<EvictionQueueContainer> mEvictionQueue = new LinkedList<EvictionQueueContainer>();

	@Override
	public synchronized Bitmap getBitmap(RequestIdentifier requestIdentifier, int sampleSize) {
		DecodeOperationParameters params = new DecodeOperationParameters(requestIdentifier, sampleSize);
		Bitmap bitmap = mCache.get(params);
		if (bitmap != null) {
			onEntryHit(requestIdentifier, sampleSize);
			return bitmap;
		}
		return null;
	}

	@Override
	public synchronized void cacheBitmap(Bitmap bitmap, RequestIdentifier requestIdentifier, int sampleSize) {
		DecodeOperationParameters params = new DecodeOperationParameters(requestIdentifier, sampleSize);
		mCache.put(params, bitmap);
		mSize += bitmap.getByteCount();
		onEntryHit(requestIdentifier, sampleSize);
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

	public int getNumImagesInCache() {
		return mCache.size();
	}

	public long getSize() {
		return mSize;
	}

	public long getCurrentActualSize() {
		long size = 0;
		Collection<Bitmap> bitmaps = mCache.values();
		for (Bitmap bitmap : bitmaps) {
			size += bitmap.getByteCount();
		}
		return size;
	}

	private synchronized void onEntryHit(RequestIdentifier requestIdentifier, int sampleSize) {
		EvictionQueueContainer container = new EvictionQueueContainer(requestIdentifier, sampleSize);

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
				Bitmap bitmap = mCache.remove(new DecodeOperationParameters(container.getRequestIdentifier(), container.getSampleSize()));
				mSize -= bitmap.getByteCount();
			} catch (NoSuchElementException e) {
				mSize = 0;
			}
		}
	}
}
