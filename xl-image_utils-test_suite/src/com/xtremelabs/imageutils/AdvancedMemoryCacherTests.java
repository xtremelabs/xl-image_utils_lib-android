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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.test.AndroidTestCase;

public class AdvancedMemoryCacherTests extends AndroidTestCase {
	private static final boolean SHOULD_RUN = Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT;
	private AdvancedMemoryLRUCacher mMemCache;
	private Bitmap.Config mBitmapConfig;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		if (SHOULD_RUN) {
			mMemCache = new AdvancedMemoryLRUCacher();
		}
	}

	public void testClearingCache() {
		if (!SHOULD_RUN) {
			return;
		}

		assertEquals(0, mMemCache.getNumImagesInCache());
		mMemCache.cacheBitmap(getBitmap(), new DecodeSignature("url1", 1, mBitmapConfig));
		assertEquals(1, mMemCache.getNumImagesInCache());
		mMemCache.cacheBitmap(getBitmap(), new DecodeSignature("url2", 1, mBitmapConfig));
		assertEquals(2, mMemCache.getNumImagesInCache());
		mMemCache.clearCache();
		assertEquals(0, mMemCache.getNumImagesInCache());
	}

	@SuppressLint("NewApi")
	public void testGetBitmapAndLru() {
		if (!SHOULD_RUN) {
			return;
		}

		Bitmap bitmap = getBitmap();
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 2, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url2", 1, mBitmapConfig));

		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(bitmap.getByteCount() * 3, mMemCache.getSize());

		mMemCache.setMaximumCacheSize(bitmap.getByteCount() * 2 + 1);
		assertNull(mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(bitmap.getByteCount() * 2, mMemCache.getSize());

		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertNull(mMemCache.getBitmap(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url2", 1, mBitmapConfig)));
	}

	private Bitmap getBitmap() {
		return ((BitmapDrawable) getContext().getResources().getDrawable(android.R.drawable.ic_input_add)).getBitmap();
	}
}
