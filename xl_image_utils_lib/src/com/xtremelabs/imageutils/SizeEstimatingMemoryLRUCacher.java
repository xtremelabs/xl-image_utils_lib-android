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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import android.graphics.Bitmap;

class SizeEstimatingMemoryLRUCacher implements ImageMemoryCacherInterface {
	private long mMaximumSizeInBytes = 6 * 1024 * 1024; // 6MB default
	private long mSize = 0;

	private final HashMap<DecodeSignature, Bitmap> mCache = new HashMap<DecodeSignature, Bitmap>();
	private final Set<DecodeSignature> mEvictionSet = new LinkedHashSet<DecodeSignature>();

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
		mSize += getBitmapSize(bitmap, decodeSignature);
		onEntryHit(decodeSignature);
	}

	@Override
	public synchronized void clearCache() {
		mSize = 0;
		mCache.clear();
		mEvictionSet.clear();
	}

	@Override
	public synchronized void setMaximumCacheSize(long size) {
		mMaximumSizeInBytes = size;
		performEvictions();
	}

	@Override
	public synchronized void removeAllImagesForUri(String uri) {
		Set<DecodeSignature> set = mCache.keySet();
		List<DecodeSignature> listToRemove = new ArrayList<DecodeSignature>();

		for (DecodeSignature signature : set) {
			if (signature.uri.equals(uri)) {
				listToRemove.add(signature);
			}
		}

		for (DecodeSignature signature : listToRemove) {
			Bitmap bitmap = mCache.remove(signature);
			mSize -= getBitmapSize(bitmap, signature);
			mEvictionSet.remove(signature);
		}
	}

	private synchronized void onEntryHit(DecodeSignature decodeSignature) {
		if (mEvictionSet.contains(decodeSignature)) {
			mEvictionSet.remove(decodeSignature);
			mEvictionSet.add(decodeSignature);
		} else {
			mEvictionSet.add(decodeSignature);
			performEvictions();
		}
	}

	private synchronized void performEvictions() {
		while (mSize > mMaximumSizeInBytes) {
			try {
				DecodeSignature decodeSignature = getLRU();
				Bitmap bitmap = mCache.remove(decodeSignature);
				mSize -= getBitmapSize(bitmap, decodeSignature);
			} catch (NoSuchElementException e) {
				mSize = 0;
			}
		}
	}

	private static long getBitmapSize(Bitmap bitmap, DecodeSignature decodeSignature) {
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

	private DecodeSignature getLRU() {
		DecodeSignature signatureToRemove = null;
		for (DecodeSignature signature : mEvictionSet) {
			signatureToRemove = signature;
			break;
		}
		if (signatureToRemove != null) {
			mEvictionSet.remove(signatureToRemove);
		}
		return signatureToRemove;
	}
}
