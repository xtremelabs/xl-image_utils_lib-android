package com.xtremelabs.imageutils;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AuxiliaryExecutor {

	private final ThreadPoolExecutor mExecutor;
	private final AuxiliaryBlockingQueue mQueue;

	public AuxiliaryExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, AuxiliaryBlockingQueue queue) {
		mQueue = queue;
		mExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, mQueue);
	}

	public void execute(Runnable runnable) {
		mExecutor.execute(runnable);
	}

	public void bump(Runnable runnable) {
		mQueue.bump(runnable);
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
}
