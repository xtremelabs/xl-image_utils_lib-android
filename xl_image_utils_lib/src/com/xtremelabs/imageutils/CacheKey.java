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

class CacheKey {
	public final int adapterId;
	public final int position;
	public final int memCacheRange;
	public final int diskCacheRange;

	public CacheKey(int adapterId, int position, int memCacheRange, int diskCacheRange) {
		this.adapterId = adapterId;
		this.position = position;
		this.memCacheRange = memCacheRange;
		this.diskCacheRange = diskCacheRange;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + adapterId;
		result = prime * result + position;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheKey other = (CacheKey) obj;
		if (adapterId != other.adapterId)
			return false;
		if (position != other.position)
			return false;
		return true;
	}
}
