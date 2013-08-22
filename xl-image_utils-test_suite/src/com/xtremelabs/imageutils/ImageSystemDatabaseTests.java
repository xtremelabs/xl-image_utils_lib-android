package com.xtremelabs.imageutils;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import com.xtremelabs.imageutils.ImageSystemDatabase.ImageSystemDatabaseObserver;

public class ImageSystemDatabaseTests extends AndroidTestCase {
	private ImageSystemDatabase mDatabase;
	private static final String TEST_URI_1 = "some_uri1";
	private static final String TEST_URI_2 = "some_uri2";
	private static final String TEST_URI_3 = "some_uri3";

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDatabase = new ImageSystemDatabase(mDatabaseObserver);
		mDatabase.init(mContext);
	}

	@Override
	protected void tearDown() throws Exception {
		mDatabase.clear();
		super.tearDown();
	}

	public void testBeginWrite() {
		mDatabase.beginWrite(TEST_URI_1);
		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);

		assertNotNull(entry);
		assertFalse(entry.onDisk);
		assertFalse(entry.hasDetails());
		assertEquals(TEST_URI_1, entry.uri);
	}

	public void testEndWrite() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_1);

		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);
		assertNotNull(entry);
		assertTrue(entry.onDisk);
		assertFalse(entry.hasDetails());
	}

	public void testBump() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_1);

		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);
		long lastAccessedTime = entry.lastAccessedTime;

		SystemClock.sleep(10);

		mDatabase.bump(TEST_URI_1);

		MoreAsserts.assertNotEqual(lastAccessedTime, entry.lastAccessedTime);
	}

	public void testWriteFailed() {
		mDatabase.beginWrite(TEST_URI_1);
		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);

		assertNotNull(mDatabase.getEntry(TEST_URI_1));

		mDatabase.writeFailed(TEST_URI_1);
		entry = mDatabase.getEntry(TEST_URI_1);

		assertNull(entry);
	}

	public void testHasDetails() {
		int testSizeX = 5;
		int testSizeY = 10;
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_1);

		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);
		assertFalse(entry.hasDetails());

		mDatabase.submitDetails(TEST_URI_1, new Dimensions(testSizeX, testSizeY), 100L);

		entry = mDatabase.getEntry(TEST_URI_1);

		assertNotNull(entry);
		assertTrue(entry.onDisk);
		assertTrue(entry.hasDetails());
		assertEquals(testSizeX, entry.sizeX);
		assertEquals(testSizeY, entry.sizeY);
		assertEquals(TEST_URI_1, entry.uri);
	}

	public void testClear() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		mDatabase.submitDetails(TEST_URI_3, new Dimensions(0, 0), 100L);

		assertNotNull(mDatabase.getEntry(TEST_URI_1));
		assertNotNull(mDatabase.getEntry(TEST_URI_2));
		assertNotNull(mDatabase.getEntry(TEST_URI_3));

		mDatabase.clear();

		assertNull(mDatabase.getEntry(TEST_URI_1));
		assertNull(mDatabase.getEntry(TEST_URI_2));
		assertNull(mDatabase.getEntry(TEST_URI_3));
	}

	public void testFileSize() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);
		mDatabase.endWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		assertEquals(0, mDatabase.getTotalFileSize());

		mDatabase.submitDetails(TEST_URI_1, new Dimensions(0, 0), 100L);
		mDatabase.submitDetails(TEST_URI_2, new Dimensions(0, 0), 100L);
		mDatabase.submitDetails(TEST_URI_3, new Dimensions(0, 0), 100L);

		assertEquals(300, mDatabase.getTotalFileSize());

		mDatabase.submitDetails(TEST_URI_3, new Dimensions(0, 0), 200L);

		assertEquals(400, mDatabase.getTotalFileSize());
	}

	public void testNoImageOnDiskTriggerDownload() {
		fail();
		// should probably be somewhere else
		
		/*
		 * 1. Stub out getBitmapSynchronouslyFromDisk to throw a FileNotFoundException.
		 * 
		 * 2. Stub out the DiskCache interface with a method for "restart image load"
		 * 
		 * 3. Put an entry in the database for the "fake" image we are decoding.
		 * 
		 * 4. Create a decode prioritizable and synchronously call "execute" on it. 
		 * 
		 * 5. Assert true that the database has removed the row for our image.
		 * 
		 * 6. Assert true that the "restart image load" method was called. -- This is not built out yet and may not be 100% correct.
		 */
	}

	public void testStartupDataRecoveryOrdering() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		mDatabase.close();

		mDatabase = new ImageSystemDatabase(mDatabaseObserver);
		mDatabase.init(mContext);

		assertEquals(mDatabase.removeLRU().uri, TEST_URI_1);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_2);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_3);
		assertNull(mDatabase.removeLRU());

		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		mDatabase.bump(TEST_URI_1);

		mDatabase.close();

		mDatabase = new ImageSystemDatabase(mDatabaseObserver);
		mDatabase.init(mContext);

		assertEquals(mDatabase.removeLRU().uri, TEST_URI_2);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_3);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_1);
		assertNull(mDatabase.removeLRU());
	}

	public void testJournalingEvictionWithNoWrite() {
		mDatabase.beginWrite(TEST_URI_1);
		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);

		assertNotNull(entry);
		assertFalse(entry.onDisk);
		assertFalse(entry.hasDetails());
		assertEquals(TEST_URI_1, entry.uri);

		mDatabase.close();

		ImageSystemDatabase database = new ImageSystemDatabase(mDatabaseObserver);
		entry = database.getEntry(TEST_URI_1);
		assertNull(entry);
	}

	public void testJournalingData() {
		fail(); // TODO make sure that data written is same as data read after re-start (seen it fail)
	}

	public void testJournalingEvictionsWithNoWrite() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_2);

		ImageEntry entry1 = mDatabase.getEntry(TEST_URI_1);
		ImageEntry entry2 = mDatabase.getEntry(TEST_URI_2);
		ImageEntry entry3 = mDatabase.getEntry(TEST_URI_3);

		assertNotNull(entry1);
		assertNotNull(entry2);
		assertNotNull(entry3);

		mDatabase.close();

		ImageSystemDatabase database = new ImageSystemDatabase(mDatabaseObserver);
		database.init(mContext);
		entry1 = database.getEntry(TEST_URI_1);
		entry2 = database.getEntry(TEST_URI_2);
		entry3 = database.getEntry(TEST_URI_3);

		assertNull(entry1);
		assertNotNull(entry2);
		assertNull(entry3);
	}

	public void testRemoveLRU() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		assertEquals(mDatabase.removeLRU().uri, TEST_URI_1);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_2);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_3);
		assertNull(mDatabase.removeLRU());

		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_1);
		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		mDatabase.bump(TEST_URI_1);

		assertEquals(mDatabase.removeLRU().uri, TEST_URI_2);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_3);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_1);
		assertNull(mDatabase.removeLRU());
	}

	public void testNoLruEvictionsForIncompleteDownloads() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_2);
		mDatabase.endWrite(TEST_URI_3);

		assertEquals(mDatabase.removeLRU().uri, TEST_URI_2);
		assertEquals(mDatabase.removeLRU().uri, TEST_URI_3);
		assertNull(mDatabase.removeLRU());
	}

	private final ImageSystemDatabaseObserver mDatabaseObserver = new ImageSystemDatabaseObserver() {
		@Override
		public void onDetailsRequired(String filename) {}

		@Override
		public void onBadJournalEntry(ImageEntry entry) {}
	};
}
