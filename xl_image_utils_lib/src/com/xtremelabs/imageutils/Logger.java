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
