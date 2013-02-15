package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.SparseArray;

public class AdapterAccessor implements PriorityAccessor {
	public static enum AdapterAccessorType {
		STACK, QUEUE
	}

	private final List<Integer> mPendingAdapterIds = new ArrayList<Integer>();
	private final SparseArray<List<CacheKey>> mAdapterToCacheKeys = new SparseArray<List<CacheKey>>();
	private final Map<CacheKey, List<DefaultPrioritizable>> mCacheKeyToPrioritizables = new HashMap<CacheKey, List<DefaultPrioritizable>>();

	private int mSize = 0;

	private final AdapterAccessorType mType;

	public AdapterAccessor(AdapterAccessorType type) {
		if (type == null)
			throw new IllegalArgumentException("You may not initialize this class with a null type!");
		mType = type;
	}

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		CacheKey cacheKey = castedPrioritizable.getCacheRequest().getCacheKey();
		int adapterId = cacheKey.adapterId;

		if (!mPendingAdapterIds.contains(adapterId))
			mPendingAdapterIds.add(adapterId);

		sizeCheck(cacheKey);

		List<CacheKey> keyList = mAdapterToCacheKeys.get(adapterId);
		if (keyList == null) {
			keyList = new ArrayList<CacheKey>();
			mAdapterToCacheKeys.append(adapterId, keyList);
		}

		if (keyList.isEmpty() || !mCacheKeyToPrioritizables.containsKey(cacheKey)) {
			keyList.add(cacheKey);
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
	public synchronized boolean detach(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		CacheKey cacheKey = castedPrioritizable.getCacheRequest().getCacheKey();
		int adapterId = cacheKey.adapterId;

		List<DefaultPrioritizable> prioritizableList = mCacheKeyToPrioritizables.get(cacheKey);
		if (prioritizableList != null && prioritizableList.remove(prioritizable)) {
			if (prioritizableList.size() == 0) {
				mCacheKeyToPrioritizables.remove(cacheKey);

				List<CacheKey> cacheKeyList = mAdapterToCacheKeys.get(adapterId);
				cacheKeyList.remove(cacheKey);
				if (cacheKeyList.size() == 0) {
					mAdapterToCacheKeys.remove(adapterId);
					mPendingAdapterIds.remove(Integer.valueOf(adapterId));
				}
			}
			mSize--;
			return true;
		}

		return false;
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

	@Override
	public synchronized boolean contains(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		CacheKey cacheKey = castedPrioritizable.getCacheRequest().getCacheKey();

		return mCacheKeyToPrioritizables.get(cacheKey).contains(prioritizable);
	}

	private DefaultPrioritizable retrieveHighestPriorityRunnable(boolean removeOnRetrieval) {
		if (mSize == 0)
			return null;

		int targetAdapterIndex = mPendingAdapterIds.size() - 1;
		int adapterId = mPendingAdapterIds.get(targetAdapterIndex);

		List<CacheKey> cacheKeyList = mAdapterToCacheKeys.get(adapterId);

		int cacheKeyToRemove = 0;
		switch (mType) {
		case QUEUE:
			cacheKeyToRemove = 0;
			break;
		case STACK:
			cacheKeyToRemove = cacheKeyList.size() - 1;
			break;
		}
		CacheKey cacheKey = cacheKeyList.get(cacheKeyToRemove);

		List<DefaultPrioritizable> prioritizables = mCacheKeyToPrioritizables.get(cacheKey);

		DefaultPrioritizable prioritizable;
		if (removeOnRetrieval) {
			prioritizable = prioritizables.remove(0);

			if (prioritizables.size() == 0) {
				mCacheKeyToPrioritizables.remove(cacheKey);
				cacheKeyList.remove(cacheKey);
				if (cacheKeyList.size() == 0) {
					mAdapterToCacheKeys.remove(adapterId);
					mPendingAdapterIds.remove(targetAdapterIndex);
				}
			}

			if (prioritizable != null)
				mSize--;
		} else {
			prioritizable = prioritizables.get(0);
		}

		return prioritizable;
	}

	private void sizeCheck(CacheKey cacheKey) {
		if (mCacheKeyToPrioritizables.containsKey(cacheKey))
			return;

		int limit = cacheKey.queuedRequestLimit;
		if (limit <= 0)
			return;

		int adapterId = cacheKey.adapterId;
		List<CacheKey> cacheKeys = mAdapterToCacheKeys.get(adapterId);
		if (cacheKeys == null) {
			return;
		}

		while (cacheKeys.size() >= limit) {
			removeCacheKey(cacheKeys.remove(0));
		}

		if (cacheKeys.isEmpty()) {
			mAdapterToCacheKeys.remove(adapterId);
			mPendingAdapterIds.remove(Integer.valueOf(adapterId));
		}
	}

	private void removeCacheKey(CacheKey keyToRemove) {
		List<DefaultPrioritizable> prioritizables = mCacheKeyToPrioritizables.remove(keyToRemove);
		mSize -= prioritizables.size();
	}
}
