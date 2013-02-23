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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// TODO Build unit tests
class AuxiliaryExecutor {

	private final ThreadPoolExecutor mExecutor;
	private final AuxiliaryBlockingQueue mQueue;
	private final QueuingMaps mQueuingMaps = new QueuingMaps();

	private AuxiliaryExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, AuxiliaryBlockingQueue queue) {
		mQueue = queue;
		mExecutor = new AuxiliaryThreadPool(corePoolSize, maximumPoolSize, keepAliveTime, unit, mQueue);
	}

	public synchronized void execute(final Prioritizable prioritizable) {
		if (!prioritizable.isCancelled()) {
			mQueuingMaps.put(prioritizable);
			mExecutor.execute(prioritizable);
		}
	}

	public synchronized void notifyRequestComplete(final Request<?> request) {
		mQueuingMaps.onComplete(request);
	}

	public synchronized void notifySwap(final CacheKey cacheKey, final int targetIndex, final int memoryIndex, final int diskIndex) {
		mQueue.notifySwap(cacheKey, targetIndex, memoryIndex, diskIndex);
	}

	public synchronized void cancel(final Prioritizable prioritizable) {
		mQueuingMaps.cancel(prioritizable);
	}

	private synchronized void notifyBeforeExecuteCalled(final Runnable r) {
		Prioritizable prioritizable = (Prioritizable) r;
		if (!prioritizable.isCancelled())
			mQueuingMaps.notifyExecuting(prioritizable);
	}

	static class Builder {
		private int mCorePoolSize = 1;
		private int mAdditionalThreads = 0;
		private long mKeepAliveTime = 0;
		private TimeUnit mTimeUnit = TimeUnit.MILLISECONDS;
		private final PriorityAccessor[] mPriorityAccessors;

		public Builder(PriorityAccessor[] accessors) {
			mPriorityAccessors = accessors;
		}

		public Builder setCorePoolSize(int corePoolSize) {
			mCorePoolSize = corePoolSize;
			return this;
		}

		public Builder setNumExtraAvailableThreads(int additionalThreads) {
			mAdditionalThreads = additionalThreads;
			return this;
		}

		public Builder setKeepAliveTime(int keepAliveTime, TimeUnit timeUnit) {
			mKeepAliveTime = keepAliveTime;
			mTimeUnit = timeUnit;
			return this;
		}

		public AuxiliaryExecutor create() {
			AuxiliaryBlockingQueue queue = new AuxiliaryBlockingQueue(mPriorityAccessors);
			AuxiliaryExecutor executor = new AuxiliaryExecutor(mCorePoolSize, mCorePoolSize + mAdditionalThreads, mKeepAliveTime, mTimeUnit, queue);
			return executor;
		}
	}

	private class AuxiliaryThreadPool extends ThreadPoolExecutor {
		private AuxiliaryThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			notifyBeforeExecuteCalled(r);

			super.beforeExecute(t, r);
		}
	}
}
