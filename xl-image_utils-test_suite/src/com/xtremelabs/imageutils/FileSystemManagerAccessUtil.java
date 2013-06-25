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

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

public class FileSystemManagerAccessUtil {
	private final FileSystemManager mFileSystemManager;

	public FileSystemManagerAccessUtil(Context applicationContext) {
		mFileSystemManager = new FileSystemManager("img", applicationContext);
	}

	public void clearDiskCache() {
		mFileSystemManager.clearDirectory();
	}

	public void loadStreamToFile(InputStream inputStream, String filename) throws IOException {
		mFileSystemManager.loadStreamToFile(inputStream, filename);
	}

	public void deleteFile(String filename) {
		mFileSystemManager.deleteFile(filename);
	}

	public boolean isOnDisk(String filename) {
		return mFileSystemManager.isOnDisk(filename);
	}
}
