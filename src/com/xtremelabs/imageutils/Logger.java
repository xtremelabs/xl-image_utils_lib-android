package com.xtremelabs.imageutils;

import android.util.Log;

class Logger {
	/*
	 * All logging levels must be powers of two.
	 */
	public static final int ALL = 1;
	public static final int PROFILER = 2;
	public static final int ASYNC_MAPS = 4;
	public static final int VERBOSE = 8;
	public static final int NETWORK = 16;
	public static final int DISK = 32;
	
	private static int logLevel;
	
	public static void d(String message) {
		Log.d(ImageLoader.TAG, message);
	}
	
	public static void i(String message) {
		Log.i(ImageLoader.TAG, message);
	}
	
	public static void w(String message) {
		Log.w(ImageLoader.TAG, message);
	}

	public static void reportLogType(int logType) {
		logLevel |= logType;
	}
	
	public static void stopReportingLogType(int logType) {
		if (logType > 0 && (logLevel & logType) == logType) {
			logLevel -= logType;
		}
	}
	
	public static boolean logAll() {
		return (logLevel & ALL) > 0;
	}
	
	public static boolean isProfiling() {
		return (logLevel & PROFILER) > 0 || (logLevel & ALL) > 0;
	}
	
	public static boolean logMaps() {
		return (logLevel & ALL) > 0 || (logLevel & ASYNC_MAPS) > 0;
	}
	
	public static boolean logNetwork() {
		return (logLevel & ALL) > 0 || (logLevel & NETWORK) > 0;
	}
	
	public static boolean logDisk() {
		return (logLevel & ALL) > 0 || (logLevel & DISK) > 0;
	}
}
