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

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

class AdapterAccessor implements PriorityAccessor {

	public static enum AdapterAccessorType {
		DEPRIORITIZED, PRECACHE_MEMORY, PRECACHE_DISK
	}

	private final List<Integer> mPendingAdapterIds = new ArrayList<Integer>();
	private final SparseArray<Position[]> mAdapterToPositions = new SparseArray<Position[]>();

	private int mSize = 0;
	private final AdapterAccessorType mAdapterAccessorType;
	private final RequestObserver mObserver;

	public AdapterAccessor(AdapterAccessorType adapterAccessorType, RequestObserver observer) {
		if (adapterAccessorType == null)
			throw new IllegalArgumentException("The adapter accessor type cannot be null.");

		mAdapterAccessorType = adapterAccessorType;
		mObserver = observer;
	}

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		CacheRequest cacheRequest = castedPrioritizable.getCacheRequest();
		CacheKey cacheKey = cacheRequest.getCacheKey();

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
	public synchronized void swap(CacheKey cacheKey, PriorityAccessor memAccessor, PriorityAccessor diskAccessor) {
		swap(cacheKey, (AdapterAccessor) memAccessor, (AdapterAccessor) diskAccessor);
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

	private synchronized void swap(CacheKey cacheKey, AdapterAccessor memAccessor, AdapterAccessor diskAccessor) {
		Position[] memoryPositions = memAccessor.getOrGeneratePositionsList(cacheKey);
		Position[] diskPositions = diskAccessor.getOrGeneratePositionsList(cacheKey);
		Position[] myPositions = getOrGeneratePositionsList(cacheKey);

		// **********SIZE CHANGES**********

		int memCacheSize = cacheKey.memCacheRange;
		int diskCacheSize = cacheKey.diskCacheRange;

		int memSize = getSize(memoryPositions, 0, memCacheSize);
		int diskSize = getSize(diskPositions, 0, diskCacheSize);
		int myMemSize = getSize(myPositions, 0, memCacheSize);
		int myDiskSize = getSize(myPositions, memCacheSize, myPositions.length);

		int memSizeChange = myMemSize - memSize;
		int diskSizeChange = myDiskSize - diskSize;
		int myChange = memSize + diskSize - myMemSize - myDiskSize;

		memAccessor.mSize += memSizeChange;
		diskAccessor.mSize += diskSizeChange;
		mSize += myChange;

		// **********SWAPPING**********

		Position temp;
		for (int i = 0; i < memCacheSize; i++) {
			temp = myPositions[i];
			myPositions[i] = memoryPositions[i];
			memoryPositions[i] = temp;
		}

		for (int i = 0; i < diskCacheSize; i++) {
			temp = myPositions[i + memCacheSize];
			myPositions[i + memCacheSize] = diskPositions[i];
			diskPositions[i] = temp;
		}

		// **********CLEAN UP**********

		int adapterId = cacheKey.adapterId;

		if (myMemSize == 0)
			memAccessor.removeAdapter(adapterId);
		if (myDiskSize == 0)
			diskAccessor.removeAdapter(adapterId);
		if (memSize == 0 && diskSize == 0)
			removeAdapter(adapterId);
	}

	private static int getSize(Position[] array, int startIndex, int length) {
		int size = 0;
		for (int i = startIndex; i < length; i++) {
			if (array[i] != null)
				size += array[i].prioritizables.size();
		}
		return size;
	}

	private void removeAdapter(int adapterId) {
		mAdapterToPositions.remove(Integer.valueOf(adapterId));
		mPendingAdapterIds.remove(Integer.valueOf(adapterId));
	}

	private DefaultPrioritizable retrieveHighestPriorityRunnable(boolean removeOnRetrieval) {
		if (mSize == 0)
			return null;

		if (mPendingAdapterIds.isEmpty())
			throw new IllegalStateException("Fatal error: The size of the accessor cannot be non-zero while there are no pending adapter ids!");

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
		if (mAdapterAccessorType != AdapterAccessorType.DEPRIORITIZED)
			return;

		ImageRequestType imageRequestType = targetRunnable.mCacheRequest.getImageRequestType();
		CacheRequest cacheRequest = targetRunnable.getCacheRequest();
		CacheKey cacheKey = cacheRequest.getCacheKey();

		if (imageRequestType == ImageRequestType.DEPRIORITIZED) {
			if (targetKeyIndex < cacheKey.memCacheRange)
				cacheRequest.setImageRequestType(ImageRequestType.DEPRIORITIZED_PRECACHE_TO_MEMORY_FOR_ADAPTER);
			else
				cacheRequest.setImageRequestType(ImageRequestType.DEPRIORITIZED_PRECACHE_TO_DISK_FOR_ADAPTER);
		}
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
			for (int i = 0; i < positions.length && temp != null; i++) {
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
			// The deprioritized accessor does not use an observer, so we need to perform the null check here.
			if (mObserver != null)
				mObserver.onRequestsCancelled(temp.prioritizables);
			mSize -= temp.prioritizables.size();
		}

		return position;
	}

	private Position[] getOrGeneratePositionsList(CacheKey cacheKey) {
		int adapterId = cacheKey.adapterId;

		if (!mPendingAdapterIds.contains(adapterId))
			mPendingAdapterIds.add(adapterId);

		Position[] positions = mAdapterToPositions.get(adapterId);
		if (positions == null)
			positions = generateNewPositionsArray(cacheKey);
		mAdapterToPositions.append(adapterId, positions);
		return positions;
	}

	private Position[] generateNewPositionsArray(CacheKey cacheKey) {
		Position[] positions;
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
		return positions;
	}

	private static class Position {
		CacheKey key;
		List<DefaultPrioritizable> prioritizables;
	}

	static interface RequestObserver {
		public void onRequestsCancelled(List<DefaultPrioritizable> cancelledPrioritizables);
	}
}
