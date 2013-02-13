package com.xtremelabs.imageutils;

import java.util.Collection;
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

	public synchronized boolean containsAll(Collection<Prioritizable> collection) {
		for (Prioritizable object : collection) {
			if (!contains(object)) {
				return false;
			}
		}
		return true;
	}

	synchronized boolean contains(Prioritizable prioritizable) {
		boolean isContained = false;
		for (int i = 0; i < mNumAccessors; i++) {
			if (mPriorityAccessors[i].contains(prioritizable)) {
				isContained = true;
				break;
			}
		}
		return isContained;
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

	public boolean remove(Prioritizable prioritizable) {
		PriorityAccessor accessor = mPriorityAccessors[prioritizable.getTargetPriorityAccessorIndex()];
		return accessor.detach(prioritizable);
	}

	public boolean remove(Prioritizable prioritizable, OnRemovedListener listener) {
		if (remove(prioritizable)) {
			if (listener != null) {
				listener.onRemoved();
			}
			return true;
		} else {
			if (listener != null) {
				listener.onRemovalFailed();
			}
			return false;
		}
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public interface OnRemovedListener {
		public void onRemoved();

		public void onRemovalFailed();
	}
}
