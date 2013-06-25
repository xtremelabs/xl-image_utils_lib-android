package com.xtremelabs.imageutils;

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
		mDatabase.init(getContext());
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
		mDatabase.submitDetails(TEST_URI_1, new Dimensions(testSizeX, testSizeY));

		ImageEntry entry = mDatabase.getEntry(TEST_URI_1);

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

		mDatabase.submitDetails(TEST_URI_3, new Dimensions(0, 0));

		mDatabase.clear();

		assertNull(mDatabase.getEntry(TEST_URI_1));
		assertNull(mDatabase.getEntry(TEST_URI_2));
		assertNull(mDatabase.getEntry(TEST_URI_3));
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

		ImageSystemDatabase database = new ImageSystemDatabase(mDatabaseObserver);
		entry = database.getEntry(TEST_URI_1);
		assertNull(entry);
	}

	public void testJournalingEvictionsWithNoWrite() {
		mDatabase.beginWrite(TEST_URI_1);
		mDatabase.beginWrite(TEST_URI_2);
		mDatabase.beginWrite(TEST_URI_3);

		mDatabase.endWrite(TEST_URI_2);

		mDatabase.close();

		ImageSystemDatabase database = new ImageSystemDatabase(mDatabaseObserver);
		ImageEntry entry1 = database.getEntry(TEST_URI_1);
		ImageEntry entry2 = database.getEntry(TEST_URI_2);
		ImageEntry entry3 = database.getEntry(TEST_URI_3);

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

	public void testJournalingLruEvictions() {
		fail();
	}

	public void testDetailsOnDatabaseRecovery() {
		fail();
	}

	public void testNoEvictionsForIncompleteDownloads() {
		fail();
	}

	private final ImageSystemDatabaseObserver mDatabaseObserver = new ImageSystemDatabaseObserver() {
		@Override
		public void onDetailsRequired(String filename) {
		}

		@Override
		public void onBadJournalEntry(String filename) {
		}
	};
}
