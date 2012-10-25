/*
 * Copyright 2012 Xtreme Labs
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

class CachedImagesMap {
	private HashMap<String, Dimensions> mImageDimensionsMap = new HashMap<String, Dimensions>();
	
	public synchronized void putDimensions(String url, Dimensions dimensions) {
		mImageDimensionsMap.put(url, dimensions);
	}
	
	public synchronized Dimensions getImageDimensions(String url) {
		return mImageDimensionsMap.get(url);
	}
	
	public synchronized void removeDimensions(String url) {
		mImageDimensionsMap.remove(url);
	}
	
	public synchronized boolean isCached(String url) {
		return mImageDimensionsMap.containsKey(url);
	}
	
	public synchronized void clear() {
		mImageDimensionsMap.clear();
	}
}
