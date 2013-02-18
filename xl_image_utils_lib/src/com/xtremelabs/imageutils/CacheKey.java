package com.xtremelabs.imageutils;

public class CacheKey {
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
