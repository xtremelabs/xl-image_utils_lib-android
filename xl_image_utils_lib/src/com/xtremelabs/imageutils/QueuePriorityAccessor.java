package com.xtremelabs.imageutils;

public class QueuePriorityAccessor implements PriorityAccessor {
	private final HashedQueue<Prioritizable> mQueue = new HashedQueue<Prioritizable>();

	@Override
	public void attach(Prioritizable prioritizable) {
		mQueue.add(prioritizable);
	}

	@Override
	public Prioritizable detachHighestPriorityItem() {
		return mQueue.poll();
	}

	@Override
	public int size() {
		return mQueue.size();
	}

	@Override
	public Prioritizable peek() {
		return mQueue.peek();
	}

	@Override
	public void clear() {
		mQueue.clear();
	}

	@Override
	public void swap(CacheKey cacheKey, PriorityAccessor priorityAccessor, PriorityAccessor priorityAccessor2) {
		// TODO Auto-generated method stub

	}
}
