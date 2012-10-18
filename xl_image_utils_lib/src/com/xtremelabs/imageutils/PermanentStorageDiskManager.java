package com.xtremelabs.imageutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

class PermanentStorageDiskManager extends BaseDiskManager {
	private static final String PERMANENT_STORAGE_SUB_DIRECTORY = "xl-images-perm-storage";
	private File mDirectory;
	private final Context mApplicationContext;

	PermanentStorageDiskManager(Context applicationContext) {
		mApplicationContext = applicationContext;
		getDirectory();
	}

	public List<String> listFilesInPermanentStorage() {
		File directory = getDirectory();
		File[] files = directory.listFiles();
		List<String> filenames = new ArrayList<String>(files.length);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile())
				filenames.add(files[i].getName());
		}
		return filenames;
	}

	@Override
	protected File getDirectory() {
		if (mDirectory == null) {
			mDirectory = new File(mApplicationContext.getFilesDir() + File.separator + PERMANENT_STORAGE_SUB_DIRECTORY);

			if (mDirectory.exists() && !mDirectory.isDirectory()) {
				GenericDiskOperations.deleteFile(mApplicationContext.getFilesDir(), PERMANENT_STORAGE_SUB_DIRECTORY);
			}
			if (!mDirectory.exists()) {
				mDirectory.mkdir();
			}
		}
		return mDirectory;
	}
}
