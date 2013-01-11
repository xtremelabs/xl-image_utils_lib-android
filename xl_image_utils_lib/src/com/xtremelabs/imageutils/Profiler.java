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

import java.util.HashMap;

import android.os.Debug;
import android.util.Log;

class Profiler {
	private static HashMap<String, Long> map = new HashMap<String, Long>();

	public static synchronized void init(String key) {
		map.put(key, System.currentTimeMillis());
	}

	public static synchronized void report(String key) {
		Long previousTime = map.remove(key);
		if (previousTime != null) {
			long time = System.currentTimeMillis() - previousTime;

			if (time >= 4) {
				Log.i("Profiler", "Operation: " + key + " - Time: " + time);
			}
		} else {
			Log.i("Profiler", "Previous time is null.");
		}
	}

	public static void logHeapValues(String prefix) {
		Log.i("Profiler", prefix + "Heap size: " + Debug.getNativeHeapSize() + ", Free size: " + Debug.getNativeHeapFreeSize() + ", Used: " + Debug.getNativeHeapAllocatedSize());
	}
}
