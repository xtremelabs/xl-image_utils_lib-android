package com.xtremelabs.imageutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

abstract class BaseDiskManager {
	protected abstract File getDirectory();

	public synchronized boolean isOnDisk(String filename) {
		return GenericDiskOperations.isOnDisk(getDirectory(), filename);
	}

	public synchronized File getFile(String filename) {
		return GenericDiskOperations.getFile(getDirectory(), filename);
	}

	public synchronized void loadStreamToFile(InputStream inputStream, String filename) throws IOException {
		GenericDiskOperations.loadStreamToFile(inputStream, getDirectory(), filename);
	}

	public synchronized void clearDirectory() {
		GenericDiskOperations.clearDirectory(getDirectory());
	}

	public synchronized void deleteFile(String filename) {
		GenericDiskOperations.deleteFile(getDirectory(), filename);
	}
}
