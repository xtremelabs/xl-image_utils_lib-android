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

class TwoWayHashMap<T, U> {
	private final HashMap<T, U> mTToUMap = new HashMap<T, U>();
	private final HashMap<U, T> mUToTMap = new HashMap<U, T>();

	public synchronized void put(T firstKey, U secondKey) {
		if (firstKey != null) {
			mTToUMap.put(firstKey, secondKey);
		}

		if (secondKey != null) {
			mUToTMap.put(secondKey, firstKey);
		}
	}

	public synchronized T getPrimaryItem(U key) {
		return mUToTMap.get(key);
	}

	public synchronized U getSecondaryItem(T key) {
		return mTToUMap.get(key);
	}

	public synchronized U removePrimaryItem(T key) {
		U temp = mTToUMap.remove(key);
		if (temp != null) {
			mUToTMap.remove(temp);
			return temp;
		}
		return null;
	}

	public synchronized T removeSecondaryItem(U key) {
		T temp = mUToTMap.remove(key);
		if (temp != null) {
			mTToUMap.remove(temp);
			return temp;
		}
		return null;
	}
}
