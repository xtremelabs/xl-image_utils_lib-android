package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.os.Environment;

/**
 * Provides access to basic disk operations.
 * 
 * This class is not thread safe.
 * 
 * @author Jamie Halpern
 * 
 */
public class DiskManager {
	private final String subDirectory;
	private Context appContext;
	private File cacheDir; // Do not access this variable directly. It can disappear at any time. Use "getCacheDir()" instead.
	private long directorySize;

	public DiskManager(String subDirectory, Context appContext) {
		this.subDirectory = subDirectory;
		this.appContext = appContext;
		getCacheDir();
	}

	public boolean isOnDisk(String filename) {
		File file = new File(getCacheDir(), filename);
		return file.exists();
	}

	public File getFile(String filename) {
		return new File(getCacheDir(), filename);
	}

	public void loadStreamToFile(InputStream inputStream, String filename) throws IOException {
		File file = new File(getCacheDir(), filename);
		FileOutputStream fileOutputStream = null;

		try {
			if (file.exists()) {
				directorySize -= file.length();
			}
			fileOutputStream = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}
			directorySize += file.length();
		} catch (IOException e) {
			file.delete();
			throw e;
		} catch (OutOfMemoryError e) {
			file.delete();
			throw e;
		} finally {
			try {
				if (fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public long getLastModifiedTime(String filename) {
		File file = new File(cacheDir, filename);
		if (file != null && file.exists()) {
			return file.lastModified();
		} else {
			throw new IllegalArgumentException("File does not exist!");
		}
	}

	public long getDirectorySize() {
		return directorySize;
	}

	public void deleteLeastRecentlyUsedFile() {
		File[] files = getCacheDir().listFiles();
//		if (files.length == 0) {
//			return;
//		} // we load the file to disk before clearing, should never be length 0
		
		File leastUsed = files[0];
		for (int i = 1; i < files.length; i++) {
			if (files[i].lastModified() < leastUsed.lastModified()) {
				leastUsed = files[i];
			}
		}

		if (leastUsed != null) {
			directorySize -= leastUsed.length();
			leastUsed.delete();
		}
	}

	private File getCacheDir() {
		if (cacheDir == null || !cacheDir.exists()) {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state) && appContext.getExternalCacheDir() != null) {
				String directory = appContext.getExternalCacheDir().getAbsolutePath() + File.separatorChar + subDirectory;
				cacheDir = new File(directory);
			} else {
				String directory = appContext.getCacheDir().getAbsolutePath() + File.separatorChar + subDirectory;
				cacheDir = new File(directory);
			}

			if (!cacheDir.exists()) {
				if (!cacheDir.mkdirs()) {
					throw new RuntimeException("Was unable to create the directory!");
				}
			}
			calculateSizeOnDisk();
		}
		return cacheDir;
	}

	private void calculateSizeOnDisk() {
		File[] files = getCacheDir().listFiles();
		if (files == null) {
			return;
		}

		directorySize = 0;
		for (int i = 0; i < files.length; i++) {
			directorySize += files[i].length();
		}
	}

	public void clearDirectory() {
		deleteDirectory(getCacheDir());
		directorySize = 0;
	}

	private void deleteDirectory(File directory) {
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			}
			file.delete();
		}
	}

	public void deleteFile(String name) {
		File file = getFile(name);
		if (!file.isDirectory()) {
			file.delete();
		} else {
			deleteDirectory(file);
		}
	}
}
