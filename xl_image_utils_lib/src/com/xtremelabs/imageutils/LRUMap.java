package com.xtremelabs.imageutils;

import java.util.LinkedHashMap;

class LRUMap<KEY, VALUE> extends LinkedHashMap<KEY, VALUE> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3832920826263301600L;
	private final int mMaxEntriesBeforeEviction;

	public LRUMap(int initialCapacity, int maxEntriesBeforeEviction) {
		super(initialCapacity, 0.75f, true);
		mMaxEntriesBeforeEviction = maxEntriesBeforeEviction;
	}

	@Override
	protected boolean removeEldestEntry(java.util.Map.Entry<KEY, VALUE> eldest) {
		return size() >= mMaxEntriesBeforeEviction;
	}
}
