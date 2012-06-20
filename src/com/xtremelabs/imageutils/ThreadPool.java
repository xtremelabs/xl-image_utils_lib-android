package com.xtremelabs.imageutils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ThreadPool {
	private static final int NUM_THREADS = 10;
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);
	
	public static void execute(Runnable runnable) {
		threadPool.execute(runnable);
	}
}
