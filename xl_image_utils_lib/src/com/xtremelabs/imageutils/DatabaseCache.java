package com.xtremelabs.imageutils;

import java.util.HashMap;

public class DatabaseCache {
	private final HashMap<String, Long> mUrlToLastUpdatedTime = new HashMap<String, Long>();
	private final HashedQueue<String> hashedUrlQueue = new HashedQueue<String>();

	public void put(String url, long updateTime) {
		mUrlToLastUpdatedTime.put(url, updateTime);
		hashedUrlQueue.add(url);
	}

	public long getUpdateTime(String url) {
		Long lastUpdatedTime = mUrlToLastUpdatedTime.get(url);
		if (lastUpdatedTime == null) {
			return -1;
		} else {
			return lastUpdatedTime;
		}
	}

	public void remove(String url) {
		mUrlToLastUpdatedTime.remove(url);
		hashedUrlQueue.remove(url);
	}

	public String getLRU() {
		return hashedUrlQueue.peek();
	}
}
