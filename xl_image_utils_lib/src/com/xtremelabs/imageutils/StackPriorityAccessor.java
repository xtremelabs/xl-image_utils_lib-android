package com.xtremelabs.imageutils;

class StackPriorityAccessor implements PriorityAccessor {
	private final HashedStack<Prioritizable> mStack = new HashedStack<Prioritizable>();

	@Override
	public synchronized Prioritizable detachHighestPriorityItem() {
		return mStack.pop();
	}

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		mStack.push(prioritizable);
	}

	@Override
	public synchronized int size() {
		return mStack.size();
	}

	@Override
	public boolean detach(Prioritizable p) {
		return mStack.remove(p);
	}

	@Override
	public Prioritizable peek() {
		return mStack.peek();
	}

	@Override
	public boolean contains(Prioritizable prioritizable) {
		return mStack.contains(prioritizable);
	}

	@Override
	public void clear() {
		mStack.clear();
	}
}
