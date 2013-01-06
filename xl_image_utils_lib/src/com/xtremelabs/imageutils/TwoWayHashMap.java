package com.xtremelabs.imageutils;

import java.util.HashMap;

class TwoWayHashMap<T, U> {
	private final HashMap<T, U> mTToUMap = new HashMap<T, U>();
	private final HashMap<U, T> mUToTMap = new HashMap<U, T>();

	public synchronized void put(T firstKey, U secondKey) {
		mTToUMap.put(firstKey, secondKey);
		mUToTMap.put(secondKey, firstKey);
	}

	public synchronized T getPrimaryItem(U key) {
		return mUToTMap.get(key);
	}

	public synchronized U getSecondaryItem(T key) {
		return mTToUMap.get(key);
	}

	public synchronized U removePrimaryItem(T key) {
		U temp = mTToUMap.remove(key);
		if (temp != null) {
			mUToTMap.remove(temp);
			return temp;
		}
		return null;
	}

	public synchronized T removeSecondaryItem(U key) {
		T temp = mUToTMap.remove(key);
		if (temp != null) {
			mTToUMap.remove(temp);
			return temp;
		}
		return null;
	}
}
