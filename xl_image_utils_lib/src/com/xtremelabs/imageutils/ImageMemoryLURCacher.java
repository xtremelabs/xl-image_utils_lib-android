/*
 * Copyright 2013 Les Fletcher
 * 
 * Based off of com.xtremelabs.imageutils.AdvancedMemoryLRUCacher
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;

public class ImageMemoryLURCacher implements ImageMemoryCacherInterface {

	private long mMaximumSizeInBytes = 20 * 1024 * 1024; // 20MB default
	private volatile long mSize = 0;

	private final Map<DecodeSignature, Bitmap> mCache = new LinkedHashMap<DecodeSignature, Bitmap>(16, 0.75f, true);

	@Override
	public Bitmap getBitmap(DecodeSignature decodeSignature) {
		return mCache.get(decodeSignature);
	}

	@Override
	public void cacheBitmap(Bitmap bitmap, DecodeSignature decodeSignature) {
		Bitmap cachedBitmap = mCache.get(decodeSignature);
		// Evict first, then add
		mSize += getBitmapSize(bitmap, decodeSignature);
		if (cachedBitmap == null) {
			performEvictions(mSize);
		} else {
			removeBitmap(decodeSignature);
		}
		mCache.put(decodeSignature, bitmap);
	}

	@Override
	public void setMaximumCacheSize(long size) {
		mMaximumSizeInBytes = size;
		performEvictions(mMaximumSizeInBytes);
	}

	public long getMaximumCacheSize() {
		return mMaximumSizeInBytes;
	}

	public synchronized int getNumImagesInCache() {
		return mCache.size();
	}

	public synchronized long getSize() {
		return mSize;
	}

	public synchronized long getCurrentActualSize() {
		long size = 0;

		for (Entry<DecodeSignature, Bitmap> entry : mCache.entrySet()) {
			DecodeSignature decodeSignature = entry.getKey();
			Bitmap bitmap = entry.getValue();
			size += getBitmapSize(bitmap, decodeSignature);
		}

		return size;
	}

	/*
	 * Removers.
	 */

	@Override
	public void clearCache() {
		mSize = 0;
		mCache.clear();
	}

	@Override
	public void trimCache(double percetangeToRemove) {
		if (percetangeToRemove > 1 || percetangeToRemove < 0) {
			throw new IllegalStateException("trimPercentage takes a double greater than or equal to 0 and less than or equal to 1, i.e. 0 <= x <= 1");
		}

		if (percetangeToRemove == 1) {
			clearCache();
		} else if (percetangeToRemove > 0) {
			long bytesToRemove = (long) (mSize * percetangeToRemove);
			trimCache(bytesToRemove);
		}
	}

	/*
	 * Will remove at least numBytes from cache or clear the cache if greater
	 * the max size.
	 */
	@Override
	public void trimCache(long numBytes) {
		if (numBytes < 0) {
			throw new IllegalStateException("numBytes cannot be less than 0");
		}

		if (numBytes >= mMaximumSizeInBytes || numBytes >= mSize) {
			clearCache();
		} else {
			performEvictions(mSize - numBytes);
		}
	}

	@Override
	public void trimCacheToPercentageOfMaximum(double percentage) {
		if (percentage > 1 || percentage < 0) {
			throw new IllegalStateException("trimCacheToPercentageOfMaximum takes a double greater than or equal to 0 and less than or equal to 1, i.e. 0 <= x <= 1");
		}

		if (percentage >= 1) {
			clearCache();
		} else if (percentage > 0) {
			long evictToSize = (long) (mMaximumSizeInBytes * percentage);
			performEvictions(evictToSize);
		}
	}
	
	public void trimCacheToSize(long numBytes) {
		if (numBytes < 0) {
			throw new IllegalStateException("numBytes cannot be less than 0");
		}
		
		if (numBytes == 0) {
			clearCache();
		} else {
			performEvictions(numBytes);
		}
	}

	@Override
	public void removeAllImagesForUri(String uri) {
		Set<DecodeSignature> set = mCache.keySet();
		List<DecodeSignature> listToRemove = new ArrayList<DecodeSignature>();

		for (DecodeSignature decodeSignature : set) {
			if (decodeSignature.uri.equals(uri)) {
				listToRemove.add(decodeSignature);
			}
		}

		for (DecodeSignature decodeSignature : listToRemove) {
			Bitmap bitmap = mCache.remove(decodeSignature);
			mSize -= getBitmapSize(bitmap, decodeSignature);
		}
	}

	public void removeLRUBitmap() {
		removeBitmap(getLRUKey());
	}

	public void removeBitmaps(List<DecodeSignature> decodeSignatures) {
		for (DecodeSignature signature : decodeSignatures) {
			Bitmap bitmap = mCache.remove(signature);
			mSize -= getBitmapSize(bitmap, signature);
		}
	}

	public void removeBitmap(DecodeSignature decodeSignature) {
		Bitmap bitmap = mCache.remove(decodeSignature);
		mSize -= getBitmapSize(bitmap, decodeSignature);
	}

	/*
	 * Eviction Helpers.
	 */

	private synchronized void performEvictions(long evictToSize) {
		while (mSize > evictToSize) {
			try {
				DecodeSignature decodeSignature = getLRUKey();
				Bitmap bitmap = mCache.remove(decodeSignature);
				mSize -= getBitmapSize(bitmap, decodeSignature);
			} catch (NoSuchElementException e) {
				mSize = 0;
			}
		}
	}

	public DecodeSignature getLRUKey() {
		return getKeyForIndex(0);
	}
	
	public DecodeSignature getKeyForIndex(int n) {
		int i = 0;
		for (Entry<DecodeSignature, Bitmap> entry : mCache.entrySet()) {
			if (i == n) {
				return entry.getKey();
			}
			i++;
		}
		return null;
	}

	/*
	 * Bitmap Size Helpers.
	 */

	private static long getBitmapSize(Bitmap bitmap, DecodeSignature decodeSignature) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
			return getEstimatedBitmapSize(bitmap, decodeSignature);
		} else {
			return getActualBitmapSize(bitmap);
		}
	}

	@SuppressLint("NewApi")
	private static long getActualBitmapSize(Bitmap bitmap) {
		return bitmap.getByteCount();
	}

	private static long getEstimatedBitmapSize(Bitmap bitmap, DecodeSignature decodeSignature) {
		int bytesPerPixel;
		Bitmap.Config config = decodeSignature.bitmapConfig;

		if (config != null) {
			switch (config) {
			case ALPHA_8:
				bytesPerPixel = 1;
				break;
			case ARGB_4444:
			case RGB_565:
				bytesPerPixel = 2;
				break;
			case ARGB_8888:
			default:
				bytesPerPixel = 4;
				break;
			}
		} else {
			bytesPerPixel = 4;
		}

		return bitmap.getWidth() * bitmap.getHeight() * bytesPerPixel;
	}

}
