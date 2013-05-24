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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class AuxiliaryBlockingQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {

	private final AuxiliaryQueue mQueue;
	private final ReentrantLock mLock = new ReentrantLock(true);
	private final Condition mNotEmpty;
	private int mCount = 0;

	public AuxiliaryBlockingQueue(PriorityAccessor[] accessors) {
		mQueue = new AuxiliaryQueue(accessors);
		mNotEmpty = mLock.newCondition();
	}

	@Override
	public boolean offer(Runnable e) {
		checkNotNull(e);
		mLock.lock();
		try {
			insert(e);
			return true;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public synchronized Runnable peek() {
		mLock.lock();
		try {
			return mQueue.peek();
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public Runnable poll() {
		mLock.lock();
		try {
			return extract();
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public Iterator<Runnable> iterator() {
		return null;
	}

	@Override
	public int size() {
		mLock.lock();
		try {
			return mQueue.size();
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public int drainTo(Collection<? super Runnable> collection) {
		checkNotNull(collection);
		mLock.lock();
		try {
			int numDrained = 0;
			while (mQueue.size() > 0) {
				Runnable runnable = extract();
				if (runnable == null)
					break;
				collection.add(runnable);
				numDrained++;
			}
			// FIXME We need to signal an empty queue here.
			return numDrained;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public int drainTo(Collection<? super Runnable> collection, int maxNumberToDrain) {
		checkNotNull(collection);
		mLock.lock();
		try {
			int numDrained = 0;
			for (int i = 0; i < maxNumberToDrain; i++) {
				Runnable runnable = extract();
				if (runnable == null) {
					break;
				} else {
					collection.add(runnable);
					numDrained++;
				}
			}
			return numDrained;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public boolean remove(Object object) {
		return false;
	}

	@Override
	public boolean removeAll(java.util.Collection<?> collection) {
		return false;
	}

	@Override
	public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
		checkNotNull(e);
		mLock.lockInterruptibly();
		try {
			insert(e);
			return true;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		mLock.lockInterruptibly();
		try {
			Runnable runnable = null;
			while (runnable == null) {
				if (nanos <= 0)
					return null;
				nanos = mNotEmpty.awaitNanos(nanos);
				runnable = extract();
			}
			return runnable;
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void put(Runnable e) throws InterruptedException {
		checkNotNull(e);
		mLock.lockInterruptibly();
		try {
			insert(e);
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Runnable take() throws InterruptedException {
		mLock.lockInterruptibly();
		try {
			Runnable runnable = extract();
			while (runnable == null) {
				mNotEmpty.await();
				runnable = extract();
			}
			return runnable;
		} finally {
			mLock.unlock();
		}
	}

	private void insert(Runnable r) {
		mQueue.add((Prioritizable) r);
		mCount++;
		if (mCount == 1)
			mNotEmpty.signal();
	}

	private Runnable extract() {
		Prioritizable prioritizable;
		do {
			prioritizable = mQueue.removeHighestPriorityRunnable();
			if (mCount > 0) {
				if (prioritizable != null)
					mCount--;
				else
					mCount = mQueue.size();
			}
		} while ((prioritizable == null || prioritizable.isCancelled()) && mCount > 0);

		if (prioritizable == null)
			return null;
		else
			return prioritizable.isCancelled() ? null : prioritizable;
	}

	private static void checkNotNull(Object o) {
		if (o == null) {
			throw new NullPointerException();
		}
	}

	public void notifySwap(CacheKey cacheKey, int targetIndex, int memoryIndex, int diskIndex) {
		mLock.lock();
		try {
			mQueue.notifySwap(cacheKey, targetIndex, memoryIndex, diskIndex);
		} finally {
			mLock.unlock();
		}
	}
}
