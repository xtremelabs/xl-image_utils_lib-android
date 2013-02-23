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

import android.util.Log;

class Benchmark {
	private static volatile long minTime = Long.MAX_VALUE;
	private static volatile long maxTime = Long.MIN_VALUE;
	private static volatile long numRuns = 0;
	private static volatile long totalTimeElapsed = 0;

	private final long myStartTime = System.nanoTime();

	public Benchmark() {
	}

	public synchronized void complete() {
		long totalTime = System.nanoTime() - myStartTime;
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
