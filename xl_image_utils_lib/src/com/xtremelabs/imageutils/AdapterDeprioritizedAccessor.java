package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

public class AdapterDeprioritizedAccessor implements PriorityAccessor {

	public static enum AdapterAccessorType {
		DEPRIORITIZED, PRECACHE_MEMORY, PRECACHE_DISK
	}

	private final List<Integer> mPendingAdapterIds = new ArrayList<Integer>();
	private final SparseArray<Position[]> mAdapterToPositions = new SparseArray<Position[]>();

	private int mSize = 0;
	private final AdapterAccessorType mAdapterAccessorType;

	public AdapterDeprioritizedAccessor(AdapterAccessorType adapterAccessorType) {
		if (adapterAccessorType == null)
			throw new IllegalArgumentException("The adapter accessor type cannot be null.");

		mAdapterAccessorType = adapterAccessorType;
	}

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		CacheRequest cacheRequest = castedPrioritizable.getCacheRequest();
		CacheKey cacheKey = cacheRequest.getCacheKey();
		int adapterId = cacheKey.adapterId;

		Position[] positions = getOrGeneratePositionsList(cacheKey);
		Position position = getOrGeneratePosition(cacheKey, positions);
		position.prioritizables.add(castedPrioritizable);

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
		mAdapterToPositions.clear();
		mPendingAdapterIds.clear();
		mSize = 0;
	}

	@Override
	public void swap(CacheKey cacheKey, PriorityAccessor memAccessor, PriorityAccessor diskAccessor) {
		// swap(cacheKey, (AdapterDeprioritizedAccessor) memAccessor, (AdapterDeprioritizedAccessor) diskAccessor);
	}

	synchronized boolean contains(DefaultPrioritizable prioritizable) {
		for (Integer adapterId : mPendingAdapterIds) {
			Position[] positions = mAdapterToPositions.get(adapterId);
			for (int i = 0; i < positions.length; i++) {
				Position position = positions[i];
				if (position == null)
					continue;
				for (DefaultPrioritizable p : position.prioritizables) {
					if (p.equals(prioritizable))
						return true;
				}
			}
		}
		return false;
	}

	private synchronized void swap(CacheKey cacheKey, AdapterDeprioritizedAccessor memAccessor, AdapterDeprioritizedAccessor diskAccessor) {
		Position[] memoryPositions = memAccessor.getOrGeneratePositionsList(cacheKey);
		Position[] diskPositions = diskAccessor.getOrGeneratePositionsList(cacheKey);
		Position[] myPositions = getOrGeneratePositionsList(cacheKey);

		Position temp;
		int localSizeChange = 0;
		int memCacheRange = cacheKey.memCacheRange;
		int remoteSize = 0;
		int localSize = 0;

		for (int i = 0; i < memCacheRange; i++) {
			temp = myPositions[i];
			if (temp != null) {
				localSizeChange--;
				remoteSize++;
			}

			myPositions[i] = memoryPositions[i];
			if (myPositions[i] != null) {
				localSizeChange++;
				localSize++;
			}

			memoryPositions[i] = temp;
		}
		mSize += localSizeChange;
		memAccessor.mSize -= localSizeChange;
		if (remoteSize == 0)
			memAccessor.removeAdapter(cacheKey.adapterId);
		remoteSize = 0;
		localSizeChange = 0;

		for (int i = memCacheRange; i < myPositions.length; i++) {
			temp = myPositions[i];
			if (temp != null) {
				localSizeChange--;
				remoteSize++;
			}

			int diskPosition = i - memCacheRange;
			myPositions[i] = diskPositions[diskPosition];
			if (myPositions[i] != null) {
				localSizeChange++;
				localSize++;
			}

			diskPositions[diskPosition] = temp;
		}
		mSize += localSizeChange;
		diskAccessor.mSize -= localSizeChange;
		if (remoteSize == 0)
			diskAccessor.removeAdapter(cacheKey.adapterId);

		if (localSize == 0)
			removeAdapter(cacheKey.adapterId);
	}

	private void removeAdapter(int adapterId) {
		mAdapterToPositions.remove(adapterId);
		mPendingAdapterIds.remove(Integer.valueOf(adapterId));
	}

	private DefaultPrioritizable retrieveHighestPriorityRunnable(boolean removeOnRetrieval) {
		if (mSize == 0)
			return null;

		int targetAdapter = mPendingAdapterIds.get(mPendingAdapterIds.size() - 1);
		Position[] positions = mAdapterToPositions.get(targetAdapter);
		int targetKeyIndex = -1;
		int numEntries = 0;
		for (int i = 0; i < positions.length; i++) {
			if (positions[i] != null) {
				numEntries++;
				if (targetKeyIndex == -1)
					targetKeyIndex = i;
			}
		}

		DefaultPrioritizable targetRunnable;
		if (removeOnRetrieval) {
			targetRunnable = removeHighestPriorityRunnable(targetAdapter, positions, targetKeyIndex, numEntries);
		} else {
			targetRunnable = positions[targetKeyIndex].prioritizables.get(0);
		}

		return targetRunnable;
	}

	private DefaultPrioritizable removeHighestPriorityRunnable(int targetAdapter, Position[] positions, int targetKeyIndex, int numEntries) {
		DefaultPrioritizable targetRunnable;
		List<DefaultPrioritizable> pList = positions[targetKeyIndex].prioritizables;
		targetRunnable = pList.remove(0);

		forceRunnableToAppropriateCache(targetKeyIndex, targetRunnable);

		if (pList.isEmpty()) {
			positions[targetKeyIndex] = null;
			numEntries--;
			if (numEntries == 0) {
				mAdapterToPositions.remove(Integer.valueOf(targetAdapter));
				mPendingAdapterIds.remove(Integer.valueOf(targetAdapter));
			}
		}
		mSize--;
		return targetRunnable;
	}

	private void forceRunnableToAppropriateCache(int targetKeyIndex, DefaultPrioritizable targetRunnable) {
		CacheRequest cacheRequest = targetRunnable.getCacheRequest();
		CacheKey cacheKey = cacheRequest.getCacheKey();
		if (mAdapterAccessorType == AdapterAccessorType.PRECACHE_MEMORY || (mAdapterAccessorType == AdapterAccessorType.DEPRIORITIZED && targetKeyIndex < cacheKey.memCacheRange))
			cacheRequest.setRequestType(ImageRequestType.PRECACHE_TO_MEMORY_FOR_ADAPTER);
		else if (mAdapterAccessorType == AdapterAccessorType.PRECACHE_DISK || mAdapterAccessorType == AdapterAccessorType.DEPRIORITIZED)
			cacheRequest.setRequestType(ImageRequestType.PRECACHE_TO_DISK_FOR_ADAPTER);
	}

	private Position getOrGeneratePosition(CacheKey cacheKey, Position[] positions) {
		Position position = null;
		for (int i = 0; i < positions.length && position == null; i++) {
			Position temp = positions[i];
			if (temp != null && temp.key.equals(cacheKey))
				position = temp;
		}

		if (position == null)
			position = addPositionForKey(cacheKey, positions);

		return position;
	}

	private Position addPositionForKey(CacheKey cacheKey, Position[] positions) {
		Position position = new Position();
		position.key = cacheKey;
		position.prioritizables = new ArrayList<DefaultPrioritizable>();

		Position temp = position;

		switch (mAdapterAccessorType) {
		case DEPRIORITIZED:
			for (int i = 0; i < positions.length && position != null; i++) {
				Position placeholder = positions[i];
				positions[i] = temp;
				temp = placeholder;
			}
			break;
		case PRECACHE_DISK:
		case PRECACHE_MEMORY:
			for (int i = positions.length - 1; i >= 0; i--) {
				Position placeholder = positions[i];
				positions[i] = temp;
				temp = placeholder;
			}
			break;
		}

		if (temp != null) {
			mSize -= temp.prioritizables.size();
		}

		return position;
	}

	private Position[] getOrGeneratePositionsList(CacheKey cacheKey) {
		int adapterId = cacheKey.adapterId;

		if (!mPendingAdapterIds.contains(adapterId))
			mPendingAdapterIds.add(adapterId);

		Position[] positions = mAdapterToPositions.get(adapterId);
		if (positions != null)
			return positions;

		int maxNumPositions;
		switch (mAdapterAccessorType) {
		case DEPRIORITIZED:
			maxNumPositions = cacheKey.memCacheRange + cacheKey.diskCacheRange;
			break;
		case PRECACHE_DISK:
			maxNumPositions = cacheKey.diskCacheRange;
			break;
		case PRECACHE_MEMORY:
			maxNumPositions = cacheKey.memCacheRange;
			break;
		default:
			throw new IllegalStateException("The adapter accessor type cannot be null.");
		}
		positions = new Position[maxNumPositions];
		mAdapterToPositions.append(adapterId, positions);
		return positions;
	}

	private static class Position {
		CacheKey key;
		List<DefaultPrioritizable> prioritizables;
	}
}
