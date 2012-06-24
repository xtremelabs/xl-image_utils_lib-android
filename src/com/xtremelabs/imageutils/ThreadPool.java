package com.xtremelabs.imageutils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class ThreadPool {
	private final int mNumThreads;
	private final ExecutorService mThreadPool;
	
	public ThreadPool(int numThreads) {
		mNumThreads = numThreads;
		mThreadPool = Executors.newFixedThreadPool(mNumThreads);
	}
	
	public void execute(Runnable runnable) {
		mThreadPool.execute(runnable);
	}
}
