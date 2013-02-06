package com.xtremelabs.imageutils;

public class AuxiliaryQueue {
	private final int mNumAccessors;
	private final PriorityAccessor[] mPriorityAccessors;

	public AuxiliaryQueue(int numAccessors) {
		mNumAccessors = numAccessors;
		mPriorityAccessors = new PriorityAccessor[numAccessors];
	}

	public AuxiliaryQueue(PriorityAccessor[] accessors) {
		this(accessors.length);
		for (int i = 0; i < mNumAccessors; i++) {
			mPriorityAccessors[i] = accessors[i];
		}
	}

	public synchronized void setPriorityAccessor(int index, PriorityAccessor queue) {
		mPriorityAccessors[index] = queue;
	}

	public synchronized Prioritizable removeHighestPriorityRunnable() {
		Prioritizable prioritizable;
		for (int i = 0; i < mNumAccessors; i++) {
			if (mPriorityAccessors[i] != null && (prioritizable = mPriorityAccessors[i].pop()) != null) {
				return prioritizable;
			}
		}
		return null;
	}

	public synchronized void add(Prioritizable prioritizable, int index) {
		boolean isDetached = prioritizable.detach();
		if (isDetached) {
			mPriorityAccessors[index].attach(prioritizable);
		}
	}
}
