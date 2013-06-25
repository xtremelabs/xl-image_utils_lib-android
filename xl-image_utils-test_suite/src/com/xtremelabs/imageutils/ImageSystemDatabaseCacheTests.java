package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class ImageSystemDatabaseCacheTests extends AndroidTestCase {
	private static final String TEST_URI = "this is a URI";
	private static final long TEST_LAST_ACCESS_TIME = 500l;
	private ImageSystemDatabaseCache mImageSystemDatabaseCache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mImageSystemDatabaseCache = new ImageSystemDatabaseCache();
	}

	public void testGetEntry() {
		ImageEntry entry = new ImageEntry();
		entry.uri = TEST_URI;
		entry.lastAccessedTime = TEST_LAST_ACCESS_TIME;
		mImageSystemDatabaseCache.putEntry(entry);

		entry = mImageSystemDatabaseCache.getEntry(TEST_URI);
		assertNotNull(entry);
		assertEquals(TEST_LAST_ACCESS_TIME, entry.lastAccessedTime);
	}
}
