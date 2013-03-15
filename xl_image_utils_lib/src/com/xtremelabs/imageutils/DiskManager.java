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
 */
class DiskManager {
	private static final int MAXIMUM_CACHE_DIR_ATTEMPTS = 3;
	private final String subDirectory;
	private final Context appContext;
	private File cacheDir; // Do not access this variable directly. It can disappear at any time. Use "getCacheDir()" instead.

	public DiskManager(String subDirectory, Context appContext) {
		this.subDirectory = subDirectory;
		this.appContext = appContext;
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
			fileOutputStream = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}
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

	private File getCacheDir() {
		boolean cacheDirExists = false;
		int attempts = 0;
		do {
			if (attempts != 0) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}

			synchronized (this) {
				if (cacheDir == null || !cacheDir.exists()) {
					String state = Environment.getExternalStorageState();
					if (Environment.MEDIA_MOUNTED.equals(state) && appContext.getExternalCacheDir() != null) {
						String directory = appContext.getExternalCacheDir().getAbsolutePath() + File.separatorChar + subDirectory;
						cacheDir = new File(directory);
					} else {
						String directory = appContext.getCacheDir().getAbsolutePath() + File.separatorChar + subDirectory;
						cacheDir = new File(directory);
					}

					cacheDirExists = !(cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs()));
				}
			}
		} while (attempts++ < MAXIMUM_CACHE_DIR_ATTEMPTS && !cacheDirExists);

		if (attempts == MAXIMUM_CACHE_DIR_ATTEMPTS)
			throw new RuntimeException("Was unable to create the cache directory!");

		return cacheDir;
	}

	public void clearDirectory() {
		deleteDirectory(getCacheDir());
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
