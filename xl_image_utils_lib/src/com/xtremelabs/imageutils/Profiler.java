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

			if (time >= 1) {
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
