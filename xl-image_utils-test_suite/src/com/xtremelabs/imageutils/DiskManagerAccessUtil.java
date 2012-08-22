package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

public class DiskManagerAccessUtil {
	private DiskManager mDiskManager;

	public DiskManagerAccessUtil(Context applicationContext) {
		mDiskManager = new DiskManager("img", applicationContext);
	}

	public void clearDiskCache() {
		mDiskManager.clearDirectory();
	}

	public void loadStreamToFile(InputStream inputStream, String filename) throws IOException {
		mDiskManager.loadStreamToFile(inputStream, filename);
	}
	
	public void deleteFile(String filename) {
		mDiskManager.deleteFile(filename);
	}
	
	public boolean isOnDisk(String filename) {
		return mDiskManager.isOnDisk(filename);
	}
}
