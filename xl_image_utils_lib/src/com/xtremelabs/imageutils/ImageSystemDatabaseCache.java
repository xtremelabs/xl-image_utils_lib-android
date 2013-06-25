package com.xtremelabs.imageutils;

import java.util.LinkedHashMap;
import java.util.Map;

class ImageSystemDatabaseCache {

	private final Map<String, ImageEntry> mEntryMap = new LinkedHashMap<String, ImageEntry>();

	synchronized ImageEntry getEntry(String uri) {
		return mEntryMap.get(uri);
	}

	synchronized void putEntry(ImageEntry entry) {
		mEntryMap.put(entry.uri, entry);
	}

	public ImageEntry removeEntry(String uri) {
		return mEntryMap.remove(uri);
	}

	public void clear() {
		mEntryMap.clear();
	}
}
