package com.xtremelabs.imageutils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.AndroidTestCase;

public class ImageMemoryCacherTests extends AndroidTestCase {
	private ImageMemoryLURCacher mMemCache;
	private Bitmap.Config mBitmapConfig;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mMemCache = new ImageMemoryLURCacher();
	}

	public void testClearingCache() {
		assertEquals(0, mMemCache.getNumImagesInCache());
		assertSizeCorrect();
		mMemCache.cacheBitmap(getBitmap(), new DecodeSignature("url1", 1, mBitmapConfig));
		assertEquals(1, mMemCache.getNumImagesInCache());
		assertSizeCorrect();
		mMemCache.cacheBitmap(getBitmap(), new DecodeSignature("url2", 1, mBitmapConfig));
		assertEquals(2, mMemCache.getNumImagesInCache());
		assertSizeCorrect();
		mMemCache.clearCache();
		assertEquals(0, mMemCache.getNumImagesInCache());
		assertSizeCorrect();
	}

	public void testSetMaximumCacheSize() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 3, mMemCache.getSize());
		assertSizeCorrect();

		mMemCache.setMaximumCacheSize(getBitmapSize(bitmap) * 2 + 1);
		assertNull(mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertNotNull(mMemCache.getBitmap(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 2, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testLRURemove() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);
		assertInitialOrder();
		assertSizeCorrect();

		mMemCache.removeBitmap(new DecodeSignature("url1", 1, mBitmapConfig));
		assertTrue(mMemCache.getLRUKey().equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertSizeCorrect();
	}

	public void testLRUAccess() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);
		assertInitialOrder();
		assertSizeCorrect();

		// [ url1-2, url2-1, url1-1 ]
		mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig));
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		
		// [ url1-2, url2-1, url1-1 ]
		mMemCache.getBitmap(new DecodeSignature("url2", 1, mBitmapConfig));
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		
		// [ url1-2, url2-1, url1-1 ]
		mMemCache.getBitmap(new DecodeSignature("url1", 1, mBitmapConfig));
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
	}

	public void testLRUReinsertSameBitmap() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);
		assertEquals(getBitmapSize(bitmap) * 3, mMemCache.getSize());
		assertInitialOrder();
		assertSizeCorrect();

		// [ url1-2, url2-1, url1-1 ]
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 3, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testLRUReinsertDifferentBitmap() {
		Bitmap bitmap = getBitmap();
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 2, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url2", 1, mBitmapConfig));
		assertTrue(mMemCache.getLRUKey().equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 3, mMemCache.getSize());
		assertSizeCorrect();

		Bitmap otherBitmap = getOtherBitmap();
		mMemCache.cacheBitmap(otherBitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 2 + getBitmapSize(otherBitmap), mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimBytes1() {
		Bitmap bitmap = getBitmap();
		long sizeOfBitmap = getBitmapSize(bitmap);
		setUpBasicMemCache(bitmap);

		mMemCache.trimCache(sizeOfBitmap - 1);

		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 2, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimBytes2() {
		Bitmap bitmap = getBitmap();
		long sizeOfBitmap = getBitmapSize(bitmap);
		setUpBasicMemCache(bitmap);

		mMemCache.trimCache(sizeOfBitmap + 1);

		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap), mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimBytesBiggerThanMax() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		long maxSize = mMemCache.getMaximumCacheSize();

		mMemCache.trimCache(maxSize + 1);
		assertEquals(0, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimBytesBiggerThanCurrentSize() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		long currentSize = mMemCache.getCurrentActualSize();

		mMemCache.trimCache(currentSize + 1);
		assertEquals(0, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimPercentage0() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		mMemCache.trimCache(0.0);
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 3, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimPercentage1() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		mMemCache.trimCache(0.3);
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap) * 2, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimPercentage2() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		mMemCache.trimCache(0.5);
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
		assertEquals(getBitmapSize(bitmap), mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimPercentageAll() {
		Bitmap bitmap = getBitmap();
		setUpBasicMemCache(bitmap);

		mMemCache.trimCache(1.0);
		assertEquals(0, mMemCache.getSize());
		assertSizeCorrect();
	}
	
	public void testTrimToPercentage1() {
		Bitmap bitmap = getBitmap();
		setUpTrimMemCache(bitmap);

		mMemCache.trimCacheToPercentageOfMaximum(0.75);
		assertEquals(getBitmapSize(bitmap) * 7, mMemCache.getSize());
		assertSizeCorrect();
	}
	
	public void testTrimToPercentage2() {
		Bitmap bitmap = getBitmap();
		setUpTrimMemCache(bitmap);

		mMemCache.trimCacheToPercentageOfMaximum(0.5);
		assertEquals(getBitmapSize(bitmap) * 5, mMemCache.getSize());
		assertSizeCorrect();
	}
	
	public void testTrimToPercentageAll() {
		Bitmap bitmap = getBitmap();
		setUpTrimMemCache(bitmap);

		mMemCache.trimCacheToPercentageOfMaximum(1);
		assertEquals(0, mMemCache.getSize());
		assertSizeCorrect();
	}

	public void testTrimToSize1() {
		Bitmap bitmap = getBitmap();
		setUpTrimMemCache(bitmap);

		int size = getBitmapSize(bitmap) * 7 + 1;
		mMemCache.trimCacheToSize(size);
		assertEquals(getBitmapSize(bitmap) * 7, mMemCache.getSize());
		assertSizeCorrect();
	}
	
	public void testTrimToSize2() {
		Bitmap bitmap = getBitmap();
		setUpTrimMemCache(bitmap);

		int size = getBitmapSize(bitmap) * 7 - 1;
		mMemCache.trimCacheToSize(size);
		assertEquals(getBitmapSize(bitmap) * 6, mMemCache.getSize());
		assertSizeCorrect();
	}
	
	public void testTrimToSizeAll() {
		Bitmap bitmap = getBitmap();
		setUpTrimMemCache(bitmap);

		mMemCache.trimCacheToSize(0);
		assertEquals(0, mMemCache.getSize());
		assertSizeCorrect();
	}
	
	// Helpers

	// [ url1-1, url1-2, url2-1 ]
	private void setUpBasicMemCache(Bitmap bitmap) {
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 2, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url2", 1, mBitmapConfig));
	}

	private void setUpTrimMemCache(Bitmap bitmap) {
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url0", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url1", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url2", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url3", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url4", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url5", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url6", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url7", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url8", 1, mBitmapConfig));
		mMemCache.cacheBitmap(bitmap, new DecodeSignature("url9", 1, mBitmapConfig));

		mMemCache.setMaximumCacheSize(getBitmapSize(bitmap) * 10);
	}

	private void assertSizeCorrect() {
		assertEquals(mMemCache.getCurrentActualSize(), mMemCache.getSize());
	}

	// [ url1-1, url1-2, url2-1 ]
	private void assertInitialOrder() {
		assertTrue(mMemCache.getKeyForIndex(0).equals(new DecodeSignature("url1", 1, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(1).equals(new DecodeSignature("url1", 2, mBitmapConfig)));
		assertTrue(mMemCache.getKeyForIndex(2).equals(new DecodeSignature("url2", 1, mBitmapConfig)));
	}

	private Bitmap getBitmap() {
		return ((BitmapDrawable) getContext().getResources().getDrawable(android.R.drawable.ic_input_add)).getBitmap();
	}

	@SuppressLint("NewApi")
	private int getBitmapSize(Bitmap bitmap) {
		return bitmap.getByteCount();
	}

	private Bitmap getOtherBitmap() {
		return ((BitmapDrawable) getContext().getResources().getDrawable(android.R.drawable.btn_star_big_on)).getBitmap();
	}
}
