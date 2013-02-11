package com.xtremelabs.imageutils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AuxiliaryExecutor {

	private final ThreadPoolExecutor mExecutor;
	private final AuxiliaryBlockingQueue mQueue;

	// private final Map<Request<?>, List<Prioritizable>> mQueuedRequests = new HashMap<Request<?>, List<Prioritizable>>();
	// private final Set<Request<?>> mRunningRequests = new HashSet<Request<?>>();

	private AuxiliaryExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, AuxiliaryBlockingQueue queue) {
		mQueue = queue;
		mExecutor = new AuxiliaryThreadPool(corePoolSize, maximumPoolSize, keepAliveTime, unit, mQueue);
	}

	public synchronized void execute(Prioritizable prioritizable) {
		// Request<?> request = prioritizable.getRequest();
		//
		// if (mRunningRequests.contains(request)) {
		// prioritizable.cleanUp();
		// Log.d(ImageLoader.TAG, "Preventing execution.");
		// } else {
		// Log.d(ImageLoader.TAG, "Queuing runnable...");
		// List<Prioritizable> list = mQueuedRequests.get(request);
		// if (list == null) {
		// list = new ArrayList<Prioritizable>();
		// mQueuedRequests.put(request, list);
		// }
		// list.add(prioritizable);

		mExecutor.execute(prioritizable);
		// }
	}

	public void bump(Prioritizable runnable) {
		mQueue.bump(runnable);
	}

	private synchronized void onPreExecute(Prioritizable prioritizable) {
		// Request<?> request = prioritizable.getRequest();
		// if (mRunningRequests.contains(request)) {
		// Log.d(ImageLoader.TAG, "Forcing cancel.");
		// prioritizable.cancel();
		// prioritizable.cleanUp();
		// } else {
		// mRunningRequests.add(request);
		// List<Prioritizable> list = mQueuedRequests.remove(request);
		// if (list != null) {
		// mQueue.removeAll(list);
		// for (Prioritizable p : list) {
		// p.cleanUp();
		// }
		// }
		// }
	}

	private synchronized void onPostExecute(Prioritizable prioritizable) {
		// mRunningRequests.remove(prioritizable.getRequest());
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
			onPreExecute((Prioritizable) r);

			super.beforeExecute(t, r);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);

			onPostExecute((Prioritizable) r);
		}
	}
}
