package com.xtremelabs.imageutils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.os.Build;

@SuppressLint("NewApi")
public class AdapterPrecacheAccessor implements PriorityAccessor {

	private static final int GINGERBREAD = 9;

	private final Deque<DefaultPrioritizable> mQueue = (Build.VERSION.SDK_INT >= GINGERBREAD ? new ArrayDeque<DefaultPrioritizable>() : new LinkedList<DefaultPrioritizable>());

	// private final Map<CacheKey, List<DefaultPrioritizable>> mMap = new HashMap<CacheKey, List<DefaultPrioritizable>>();

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		DefaultPrioritizable castedPrioritizable = (DefaultPrioritizable) prioritizable;
		mQueue.add(castedPrioritizable);
		sizeCheck(castedPrioritizable.getQueueLimit() <= 0 ? Integer.MAX_VALUE : castedPrioritizable.getQueueLimit());
	}

	@Override
	public synchronized boolean detach(Prioritizable p) {
		return mQueue.remove(p);
	}

	@Override
	public synchronized Prioritizable detachHighestPriorityItem() {
		return mQueue.poll();
	}

	@Override
	public synchronized int size() {
		return mQueue.size();
	}

	@Override
	public synchronized Prioritizable peek() {
		return mQueue.peek();
	}

	@Override
	public synchronized void clear() {
		mQueue.clear();
	}

	@Override
	public synchronized boolean contains(Prioritizable prioritizable) {
		return mQueue.contains(prioritizable);
	}

	private synchronized void sizeCheck(int limit) {
		while (mQueue.size() > limit) {
			mQueue.poll();
		}
	}
}
