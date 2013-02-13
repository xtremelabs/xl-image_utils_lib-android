package com.xtremelabs.imageutils;

public class QueuePriorityAccessor implements PriorityAccessor {
	private final HashedQueue<Prioritizable> mQueue = new HashedQueue<Prioritizable>();

	@Override
	public void attach(Prioritizable prioritizable) {
		mQueue.add(prioritizable);
	}

	@Override
	public boolean detach(Prioritizable p) {
		return mQueue.remove(p);
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
	public boolean contains(Prioritizable prioritizable) {
		return mQueue.contains(prioritizable);
	}

}
