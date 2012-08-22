package com.xtremelabs.imageutils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.AdvancedMemoryLRUCacher;
import com.xtremelabs.testactivity.MainActivity;

public class AdvancedMemoryCacherTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private AdvancedMemoryLRUCacher mMemCache;
	
	public AdvancedMemoryCacherTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mMemCache = new AdvancedMemoryLRUCacher();
	}
	
	public void testClearingCache() {
		assertEquals(0, mMemCache.getNumImagesInCache());
		mMemCache.cacheBitmap(getBitmap(), "url1", 1);
		assertEquals(1, mMemCache.getNumImagesInCache());
		mMemCache.cacheBitmap(getBitmap(), "url2", 1);
		assertEquals(2, mMemCache.getNumImagesInCache());
		mMemCache.clearCache();
		assertEquals(0, mMemCache.getNumImagesInCache());
	}
	
	@SuppressLint("NewApi")
	public void testGetBitmapAndLru() {
		Bitmap bitmap = getBitmap();
		mMemCache.cacheBitmap(bitmap, "url1", 1);
		mMemCache.cacheBitmap(bitmap, "url1", 2);
		mMemCache.cacheBitmap(bitmap, "url2", 1);
		
		assertNotNull(mMemCache.getBitmap("url1", 1));
		assertNotNull(mMemCache.getBitmap("url1", 2));
		assertNotNull(mMemCache.getBitmap("url2", 1));
		assertEquals(bitmap.getByteCount() * 3, mMemCache.getSize());
		
		mMemCache.setMaximumCacheSize(bitmap.getByteCount() * 2 + 1);
		assertNull(mMemCache.getBitmap("url1", 1));
		assertNotNull(mMemCache.getBitmap("url1", 2));
		assertNotNull(mMemCache.getBitmap("url2", 1));
		assertEquals(bitmap.getByteCount() * 2, mMemCache.getSize());
		
		mMemCache.cacheBitmap(bitmap, "url1", 1);
		assertNotNull(mMemCache.getBitmap("url1", 1));
		assertNull(mMemCache.getBitmap("url1", 2));
		assertNotNull(mMemCache.getBitmap("url2", 1));
	}
	
	private Bitmap getBitmap() {
		return ((BitmapDrawable) getActivity().getResources().getDrawable(android.R.drawable.ic_input_add)).getBitmap();
	}
}
