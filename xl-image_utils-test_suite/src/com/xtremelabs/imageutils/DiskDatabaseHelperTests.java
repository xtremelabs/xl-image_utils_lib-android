package com.xtremelabs.imageutils;

import java.util.List;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.DiskDatabaseHelper.DiskDatabaseHelperObserver;
import com.xtremelabs.testactivity.MainActivity;

public class DiskDatabaseHelperTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private DiskDatabaseHelper mDatabaseHelper;

	public DiskDatabaseHelperTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDatabaseHelper = new DiskDatabaseHelper(getActivity().getApplicationContext(), new DiskDatabaseHelperObserver() {
			@Override
			public void onDatabaseWiped() {
			}
		});
		mDatabaseHelper.resetTable(mDatabaseHelper.getWritableDatabase());
	}

	public void testClearingDatabase() {
		String url = "blah";
		long size = 100;
		int width = 100;
		int height = 100;

		List<FileEntry> entries = mDatabaseHelper.getAllEntries();
		assertNotNull(entries);
		assertEquals(entries.size(), 0);

		addOrUpdateAndVerifyEntry(url, size, width, height);

		entries = mDatabaseHelper.getAllEntries();
		assertNotNull(entries);
		assertEquals(entries.size(), 1);

		mDatabaseHelper.resetTable(mDatabaseHelper.getWritableDatabase());
		entries = mDatabaseHelper.getAllEntries();
		assertNotNull(entries);
		assertEquals(entries.size(), 0);
	}

	public void testTotalSizeOnDisk() {
		int width = 100;
		int height = 100;

		addOrUpdateAndVerifyEntry("url1", 100, width, height);
		assertEquals(100, mDatabaseHelper.getTotalSizeOnDisk());
		addOrUpdateAndVerifyEntry("url2", 200, width, height);
		assertEquals(300, mDatabaseHelper.getTotalSizeOnDisk());
		addOrUpdateAndVerifyEntry("url3", 300, width, height);
		assertEquals(600, mDatabaseHelper.getTotalSizeOnDisk());

		mDatabaseHelper.removeFile("url1");
		assertEquals(500, mDatabaseHelper.getTotalSizeOnDisk());
	}

	public void testDatabaseLRU() {
		int width = 100;
		int height = 100;

		addOrUpdateAndVerifyEntry("url1", 100, width, height);
		sleep(1);
		addOrUpdateAndVerifyEntry("url2", 100, width, height);
		sleep(1);
		addOrUpdateAndVerifyEntry("url3", 100, width, height);
		sleep(1);
		addOrUpdateAndVerifyEntry("url4", 100, width, height);
		sleep(1);
		addOrUpdateAndVerifyEntry("url5", 100, width, height);
		sleep(1);

		FileEntry entry;

		entry = mDatabaseHelper.getLRU();
		assertEquals("url1", entry.getUrl());
		mDatabaseHelper.removeFile(entry.getUrl());

		mDatabaseHelper.updateFile("url2");
		entry = mDatabaseHelper.getLRU();
		assertEquals("url3", entry.getUrl());

		mDatabaseHelper.updateFile("url3");
		entry = mDatabaseHelper.getLRU();
		assertEquals("url4", entry.getUrl());
	}

	private void addOrUpdateAndVerifyEntry(String url, long size, int width, int height) {
		mDatabaseHelper.addOrUpdateFile(url, size, width, height);
		FileEntry entry = mDatabaseHelper.getFileEntry(url);
		assertEquals(url, entry.getUrl());
		assertEquals(size, entry.getSize());
		Dimensions dimensions = entry.getDimensions();
		assertNotNull(dimensions);
		assertNotNull(dimensions.width);
		assertNotNull(dimensions.height);
		assertEquals(width, entry.getDimensions().width.intValue());
		assertEquals(height, entry.getDimensions().height.intValue());
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
}
