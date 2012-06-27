package com.xtremelabs.imageutils;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Build;

class LifoThreadPool {
	private ThreadPoolExecutor mThreadPool;

	public LifoThreadPool(int poolSize) {
		if (Build.VERSION.SDK_INT >= 9) {
			mThreadPool = new ThreadPoolExecutor(poolSize, poolSize, 5000, TimeUnit.SECONDS, new LifoBlockingDeque());
		} else {
			mThreadPool = new ThreadPoolExecutor(poolSize, poolSize, 5000, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		}
	}

	public void execute(Runnable runnable) {
		mThreadPool.execute(runnable);
	}

	private class LifoBlockingDeque extends LinkedBlockingDeque<Runnable> {
		private static final long serialVersionUID = -4854985351588039351L;

		@Override
		public boolean offer(Runnable e) {
			return super.offerFirst(e);
		}

		@Override
		public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
			return super.offerFirst(e, timeout, unit);
		}

		@Override
		public boolean add(Runnable e) {
			return super.offerFirst(e);
		}

		@Override
		public void put(Runnable e) throws InterruptedException {
			super.putFirst(e);
		}
	};
}
