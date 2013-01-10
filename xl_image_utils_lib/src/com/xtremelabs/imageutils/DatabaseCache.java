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

class DatabaseCache {
	private final HashMap<String, Long> mUrlToLastUpdatedTime = new HashMap<String, Long>();
	private final HashedQueue<String> hashedUrlQueue = new HashedQueue<String>();

	public void put(String url, long updateTime) {
		mUrlToLastUpdatedTime.put(url, updateTime);
		hashedUrlQueue.add(url);
	}

	public long getUpdateTime(String url) {
		Long lastUpdatedTime = mUrlToLastUpdatedTime.get(url);
		if (lastUpdatedTime == null) {
			return -1;
		} else {
			return lastUpdatedTime;
		}
	}

	public void remove(String url) {
		mUrlToLastUpdatedTime.remove(url);
		hashedUrlQueue.remove(url);
	}

	public String getLRU() {
		return hashedUrlQueue.peek();
	}
}
