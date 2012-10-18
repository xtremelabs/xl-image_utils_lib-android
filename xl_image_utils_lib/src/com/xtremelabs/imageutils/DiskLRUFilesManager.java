/*
 * Copyright 2012 Xtreme Labs
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

import android.content.Context;
import android.os.Environment;

/**
 * Provides access to basic disk operations.
 * 
 * This class is not thread safe.
 */
public class DiskLRUFilesManager extends BaseDiskManager {
	private final String mSubDirectory;
	private final Context mAppContext;
	private File mCacheDir; // Do not access this variable directly. It can disappear at any time. Use "getCacheDir()" instead.

	public DiskLRUFilesManager(String subDirectory, Context appContext) {
		mSubDirectory = subDirectory;
		mAppContext = appContext;
		getDirectory();
	}

	@Override
	protected synchronized File getDirectory() {
		if (mCacheDir == null || !mCacheDir.exists()) {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state) && mAppContext.getExternalCacheDir() != null) {
				String directory = mAppContext.getExternalCacheDir().getAbsolutePath() + File.separatorChar + mSubDirectory;
				mCacheDir = new File(directory);
			} else {
				String directory = mAppContext.getCacheDir().getAbsolutePath() + File.separatorChar + mSubDirectory;
				mCacheDir = new File(directory);
			}

			if (!mCacheDir.exists()) {
				if (!mCacheDir.mkdirs()) {
					throw new RuntimeException("Was unable to create the directory!");
				}
			}
		}
		return mCacheDir;
	}
}
