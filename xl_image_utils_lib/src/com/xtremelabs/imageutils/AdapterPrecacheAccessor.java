package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.SparseArray;

public class AdapterPrecacheAccessor implements PriorityAccessor {
	private final List<Integer> mPendingAdapterIds = new ArrayList<Integer>();
	private final SparseArray<CacheKey[]> mAdapterToCacheKeys = new SparseArray<CacheKey[]>();
	private final Map<CacheKey, List<DefaultPrioritizable>> mCacheKeyToPrioritizables = new HashMap<CacheKey, List<DefaultPrioritizable>>();

	private int mSize = 0;

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		CacheKey cacheKey = castedPrioritizable.getCacheRequest().getCacheKey();
		int adapterId = cacheKey.adapterId;

		if (!mPendingAdapterIds.contains(adapterId))
			mPendingAdapterIds.add(adapterId);

		boolean isKeyListEmpty = false;
		CacheKey[] keyList = mAdapterToCacheKeys.get(adapterId);
		if (keyList == null) {
			keyList = new CacheKey[cacheKey.memCacheRange + cacheKey.diskCacheRange];
			mAdapterToCacheKeys.append(adapterId, keyList);
			isKeyListEmpty = true;
		}

		if (isKeyListEmpty || !mCacheKeyToPrioritizables.containsKey(cacheKey)) {
			appendCacheKey(cacheKey, keyList);
		}

		List<DefaultPrioritizable> prioritizableList = mCacheKeyToPrioritizables.get(cacheKey);
		if (prioritizableList == null) {
			prioritizableList = new ArrayList<DefaultPrioritizable>();
			mCacheKeyToPrioritizables.put(cacheKey, prioritizableList);
		}
		prioritizableList.add(castedPrioritizable);

		mSize++;
	}

	@Override
	public synchronized Prioritizable detachHighestPriorityItem() {
		return retrieveHighestPriorityRunnable(true);
	}

	@Override
	public synchronized int size() {
		return mSize;
	}

	@Override
	public synchronized Prioritizable peek() {
		return retrieveHighestPriorityRunnable(false);
	}

	@Override
	public synchronized void clear() {
		mAdapterToCacheKeys.clear();
		mCacheKeyToPrioritizables.clear();
		mPendingAdapterIds.clear();
		mSize = 0;
	}

	private DefaultPrioritizable retrieveHighestPriorityRunnable(boolean removeOnRetrieval) {
		if (mSize == 0)
			return null;

		int targetAdapter = mPendingAdapterIds.get(mPendingAdapterIds.size() - 1);
		CacheKey[] keys = mAdapterToCacheKeys.get(targetAdapter);
		int targetKeyIndex = -1;
		int numEntries = 0;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) {
				numEntries++;
				if (targetKeyIndex == -1)
					targetKeyIndex = i;
			}
		}

		DefaultPrioritizable targetRunnable;
		if (removeOnRetrieval) {
			List<DefaultPrioritizable> pList = mCacheKeyToPrioritizables.get(keys[targetKeyIndex]);
			targetRunnable = pList.remove(0);
			forceRunnableToAppropriateCache(targetKeyIndex, targetRunnable);

			if (pList.isEmpty()) {
				mCacheKeyToPrioritizables.remove(keys[targetKeyIndex]);
				keys[targetKeyIndex] = null;
				numEntries--;
				if (numEntries == 0) {
					mAdapterToCacheKeys.remove(Integer.valueOf(targetAdapter));
					mPendingAdapterIds.remove(Integer.valueOf(targetAdapter));
				}
			}
			mSize--;
		} else {
			targetRunnable = mCacheKeyToPrioritizables.get(keys[targetKeyIndex]).get(0);
		}

		return targetRunnable;
	}

	private void forceRunnableToAppropriateCache(int targetKeyIndex, DefaultPrioritizable targetRunnable) {
		CacheRequest cacheRequest = targetRunnable.getCacheRequest();
		CacheKey cacheKey = cacheRequest.getCacheKey();
		if (targetKeyIndex < cacheKey.memCacheRange)
			cacheRequest.setRequestType(ImageRequestType.PRECACHE_TO_MEMORY_FOR_ADAPTER);
		else
			cacheRequest.setRequestType(ImageRequestType.PRECACHE_TO_DISK_FOR_ADAPTER);
	}

	private void appendCacheKey(CacheKey cacheKey, CacheKey[] keyList) {
		CacheKey placeholder;
		for (int i = 0; i < keyList.length && cacheKey != null; i++) {
			placeholder = keyList[i];
			keyList[i] = cacheKey;
			cacheKey = placeholder;
		}

		if (cacheKey != null) {
			List<DefaultPrioritizable> pList = mCacheKeyToPrioritizables.remove(cacheKey);
			mSize -= pList.size();
		}
	}

	@Override
	public void swap(CacheKey cacheKey, PriorityAccessor priorityAccessor, PriorityAccessor priorityAccessor2) {
		// TODO Auto-generated method stub

	}
}
