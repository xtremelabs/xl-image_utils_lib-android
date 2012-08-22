package com.xtremelabs.imageutils;

import java.io.IOException;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.DiskManager;
import com.xtremelabs.imageutils.testutils.OneKilobyteStream;
import com.xtremelabs.testactivity.MainActivity;

public class DiskManagerTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private DiskManager mDiskManager;

	public DiskManagerTests() {
		super(MainActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mDiskManager = new DiskManager("testdir", getActivity().getApplicationContext());
	}
	
	public void testBasics() {
		String file1 = "file1";
		try {
			mDiskManager.loadStreamToFile(new OneKilobyteStream(), file1);
		} catch (IOException e) {
			fail();
		}
		assertTrue(mDiskManager.isOnDisk(file1));
		mDiskManager.deleteFile(file1);
		assertFalse(mDiskManager.isOnDisk(file1));
		
		try {
			mDiskManager.loadStreamToFile(new OneKilobyteStream(), file1);
		} catch (IOException e) {
			fail();
		}
		
		assertTrue(mDiskManager.isOnDisk(file1));
		mDiskManager.clearDirectory();
		assertFalse(mDiskManager.isOnDisk(file1));
	}
}
