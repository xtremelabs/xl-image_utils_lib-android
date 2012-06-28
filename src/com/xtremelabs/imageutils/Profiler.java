package com.xtremelabs.imageutils;

import java.util.HashMap;

import android.util.Log;

class Profiler {
	private static HashMap<String, Long> map = new HashMap<String, Long>();

	public static void init(String key) {
		map.put(key, System.currentTimeMillis());
	}

	public static void report(String key) {
		Long previousTime = map.remove(key);
		if (previousTime != null) {
			long time = System.currentTimeMillis() - previousTime;

			if (time > 5) {
				Log.i("Profiler", "Operation: " + key + " - Time: " + time);
			}
		} else {
			Log.i("Profiler", "Previous time is null.");
		}
	}
}
