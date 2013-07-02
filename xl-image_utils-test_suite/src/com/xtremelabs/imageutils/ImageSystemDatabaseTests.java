package com.xtremelabs.imageutils;

import android.os.SystemClock;
import android.test.AndroidTestCase;

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
		mDatabase.clear();
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

		assertTrue(lastAccessedTime != entry.lastAccessedTime);
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

	public void testStartupDataRecovery() {
		fail();
	}

	public void testStartupDataRecoveryOrdering() {
		fail();
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

	public void testJournalingWithDetailsRequest() {
		fail();
	}

	public void testJournalingWithDetailsRequests() {
		fail();
	}

	public void testDetailsOnDatabaseRecovery() {
		fail();
	}

	public void testJournalingLruEvictions() {
		// TODO on reboot make sure we have not exceeded file system size
		fail();
	}

	public void testNoImageOnDiskTriggerDownload() {
		fail();
		// should probably be somewhere else
	}

	public void testNoLruEvictionsForIncompleteDownloads() {
		fail();
		// make sure things that have not finished writing are not considered for eviction
	}

	private final ImageSystemDatabaseObserver mDatabaseObserver = new ImageSystemDatabaseObserver() {
		@Override
		public void onDetailsRequired(String filename) {}

		@Override
		public void onBadJournalEntry(ImageEntry entry) {}

	};
}
