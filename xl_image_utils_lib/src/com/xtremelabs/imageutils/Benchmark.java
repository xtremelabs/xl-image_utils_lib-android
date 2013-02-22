package com.xtremelabs.imageutils;

import android.util.Log;

class Benchmark {
	private static volatile long minTime = Long.MAX_VALUE;
	private static volatile long maxTime = Long.MIN_VALUE;
	private static volatile long numRuns = 0;
	private static volatile long totalTimeElapsed = 0;

	private final long myStartTime = System.currentTimeMillis();

	public Benchmark() {
	}

	public synchronized void complete() {
		long totalTime = System.currentTimeMillis() - myStartTime;
		totalTimeElapsed += totalTime;

		if (minTime > totalTime)
			minTime = totalTime;

		if (maxTime < totalTime)
			maxTime = totalTime;

		numRuns++;

		if (numRuns % 20 == 0) {
			long averageTime = totalTimeElapsed / numRuns;
			Log.d("BENCHMARK", "MIN: " + minTime);
			Log.d("BENCHMARK", "AVE: " + averageTime);
			Log.d("BENCHMARK", "MAX: " + maxTime);
		}
	}
}
