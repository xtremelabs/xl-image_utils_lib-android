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

public class DiskManagerAccessUtil {
	private final DiskManager mDiskManager;

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
