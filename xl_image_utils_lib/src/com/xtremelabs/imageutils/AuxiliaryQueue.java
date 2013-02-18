package com.xtremelabs.imageutils;

import java.util.Iterator;

public class AuxiliaryQueue {
	private final int mNumAccessors;
	private final PriorityAccessor[] mPriorityAccessors;

	public AuxiliaryQueue(PriorityAccessor[] accessors) {
		mNumAccessors = accessors.length;
		mPriorityAccessors = new PriorityAccessor[mNumAccessors];

		for (int i = 0; i < mNumAccessors; i++) {
			if (accessors[i] == null) {
				throw new IllegalArgumentException("The accessor provided at index " + i + " is null!");
			}
			mPriorityAccessors[i] = accessors[i];
		}
	}

	public synchronized void add(Prioritizable e) {
		int index = e.getTargetPriorityAccessorIndex();
		mPriorityAccessors[index].attach(e);
	}

	public synchronized Prioritizable removeHighestPriorityRunnable() {
		Prioritizable prioritizable;
		for (int i = 0; i < mNumAccessors; i++) {
			if ((prioritizable = mPriorityAccessors[i].detachHighestPriorityItem()) != null) {
				return prioritizable;
			}
		}
		return null;
	}

	public synchronized int size() {
		int size = 0;
		for (int i = 0; i < mNumAccessors; i++) {
			size += mPriorityAccessors[i].size();
		}
		return size;
	}

	public synchronized Runnable peek() {
		Prioritizable prioritizable;
		for (int i = 0; i < mNumAccessors; i++) {
			if ((prioritizable = mPriorityAccessors[i].peek()) != null) {
				return prioritizable;
			}
		}
		return null;
	}

	// TODO Fill in the iterator.
	public Iterator<Runnable> buildIterator() {
		return new Iterator<Runnable>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Runnable next() {
				return null;
			}

			@Override
			public void remove() {
			}
		};
	}

	// TODO Fill in the "buildArray" method.
	public Object[] buildArray() {
		return null;
	}

	public void clear() {
		for (int i = 0; i < mNumAccessors; i++) {
			mPriorityAccessors[i].clear();
		}
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public interface OnRemovedListener {
		public void onRemoved();

		public void onRemovalFailed();
	}

	public synchronized void notifySwap(CacheKey cacheKey, int targetIndex, int memoryIndex, int diskIndex) {
		mPriorityAccessors[targetIndex].swap(cacheKey, mPriorityAccessors[memoryIndex], mPriorityAccessors[diskIndex]);
	}
}
