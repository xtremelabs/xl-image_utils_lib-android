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

import java.util.Collection;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.DiskDatabaseHelper.DiskDatabaseHelperObserver;
import com.xtremelabs.imageutils.testutils.DelayedLoop;
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

			@Override
			public void onImageEvicted(String uri) {
			}
		});
		mDatabaseHelper.resetTable(mDatabaseHelper.getWritableDatabase());
	}

	public void testClearingDatabase() {
		String url = "blah";
		long size = 100;
		int width = 100;
		int height = 100;

		Collection<FileEntry> entries = mDatabaseHelper.getAllEntries();
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

		mDatabaseHelper.deleteEntry("url1");
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

		String entry;

		// TODO The getLRU logic should be added back in.
		entry = mDatabaseHelper.getLRU();
		assertEquals("url1", entry);
		mDatabaseHelper.deleteEntry(entry);

		mDatabaseHelper.updateFile("url2");
		DelayedLoop.sleep(100);
		entry = mDatabaseHelper.getLRU();
		assertEquals("url3", entry);

		mDatabaseHelper.updateFile("url3");
		DelayedLoop.sleep(100);
		entry = mDatabaseHelper.getLRU();
		assertEquals("url4", entry);
	}

	private void addOrUpdateAndVerifyEntry(String url, long size, int width, int height) {
		mDatabaseHelper.addOrUpdateFile(url, size, width, height);
		FileEntry entry = mDatabaseHelper.getFileEntryFromCache(url);
		assertEquals(url, entry.getUri());
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
