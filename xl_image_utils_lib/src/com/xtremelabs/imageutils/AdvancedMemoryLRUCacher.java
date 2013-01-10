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

	private final HashMap<DecodeSignature, Bitmap> mCache = new HashMap<DecodeSignature, Bitmap>();
	private final LinkedList<DecodeSignature> mEvictionQueue = new LinkedList<DecodeSignature>();

	@Override
	public synchronized Bitmap getBitmap(DecodeSignature decodeSignature) {
		Bitmap bitmap = mCache.get(decodeSignature);
		if (bitmap != null) {
			onEntryHit(decodeSignature);
			return bitmap;
		}
		return null;
	}

	@Override
	public synchronized void cacheBitmap(Bitmap bitmap, DecodeSignature decodeSignature) {
		mCache.put(decodeSignature, bitmap);
		mSize += bitmap.getByteCount();
		onEntryHit(decodeSignature);
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

	private synchronized void onEntryHit(DecodeSignature decodeSignature) {
		if (mEvictionQueue.contains(decodeSignature)) {
			mEvictionQueue.remove(decodeSignature);
			mEvictionQueue.add(decodeSignature);
		} else {
			mEvictionQueue.add(decodeSignature);
			performEvictions();
		}
	}

	private synchronized void performEvictions() {
		while (mSize > mMaximumSizeInBytes) {
			try {
				DecodeSignature decodeSignature = mEvictionQueue.removeFirst();
				Bitmap bitmap = mCache.remove(decodeSignature);
				mSize -= bitmap.getByteCount();
			} catch (NoSuchElementException e) {
				mSize = 0;
			}
		}
	}
}
