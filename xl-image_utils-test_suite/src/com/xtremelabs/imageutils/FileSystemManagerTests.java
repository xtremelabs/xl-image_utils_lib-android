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

import android.test.AndroidTestCase;

import com.xtremelabs.imageutils.testutils.OneKilobyteStream;

public class FileSystemManagerTests extends AndroidTestCase {
	private FileSystemManager mFileSystemManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mFileSystemManager = new FileSystemManager("testdir", getContext());
	}

	public void testBasics() {
		String file1 = "file1";
		try {
			mFileSystemManager.loadStreamToFile(new OneKilobyteStream(), file1);
		} catch (IOException e) {
			fail();
		}
		assertTrue(mFileSystemManager.isOnDisk(file1));
		mFileSystemManager.deleteFile(file1);
		assertFalse(mFileSystemManager.isOnDisk(file1));

		try {
			mFileSystemManager.loadStreamToFile(new OneKilobyteStream(), file1);
		} catch (IOException e) {
			fail();
		}

		assertTrue(mFileSystemManager.isOnDisk(file1));
		mFileSystemManager.clearDirectory();
		assertFalse(mFileSystemManager.isOnDisk(file1));
	}
}
