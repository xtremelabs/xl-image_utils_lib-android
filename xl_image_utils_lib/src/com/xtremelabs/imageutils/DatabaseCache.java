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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class DatabaseCache {
	private final Map<String, FileEntry> mUriToFileEntry = new HashMap<String, FileEntry>();
	private final HashedQueue<String> hashedUriQueue = new HashedQueue<String>();
	private long totalSizeOnDisk = 0;

	public synchronized void put(String uri, FileEntry fileEntry) {
		if (mUriToFileEntry.containsKey(uri)) {
			totalSizeOnDisk -= mUriToFileEntry.get(uri).getSize();
		}
		mUriToFileEntry.put(uri, fileEntry);
		totalSizeOnDisk += fileEntry.getSize();
		hashedUriQueue.add(uri);
	}

	public synchronized FileEntry getFileEntry(String uri) {
		return mUriToFileEntry.get(uri);
	}

	public synchronized boolean isCached(String uri) {
		return mUriToFileEntry.containsKey(uri);
	}

	public synchronized void remove(String uri) {
		FileEntry entry = mUriToFileEntry.remove(uri);
		hashedUriQueue.remove(uri);

		if (entry != null) {
			totalSizeOnDisk -= entry.getSize();
		}
	}

	public synchronized String getLRU() {
		return hashedUriQueue.peek();
	}

	public synchronized String removeLRU(long maximumCacheSize) {
		if (totalSizeOnDisk > maximumCacheSize) {
			String uri = getLRU();
			remove(uri);
			return uri;
		}
		return null;
	}

	public synchronized void updateTime(String uri, long updateTime) {
		FileEntry entry = mUriToFileEntry.get(uri);
		if (entry != null) {
			entry.setLastAccessTime(updateTime);
			hashedUriQueue.add(uri);
		}
	}

	public synchronized long getTotalSizeOnDisk() {
		return totalSizeOnDisk;
	}

	public Collection<FileEntry> getAllEntries() {
		return mUriToFileEntry.values();
	}
}
