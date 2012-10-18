package com.xtremelabs.imageutils;

import com.xtremelabs.imageutils.ImageLoader.Options.StorageType;

class RequestIdentifier {
	private final String mUrlOrFilename;
	private final StorageType mStorageType;

	RequestIdentifier(String urlOrFilename, StorageType storageType) {
		mUrlOrFilename = urlOrFilename;
		mStorageType = storageType;
	}

	public String getUrlOrFilename() {
		return mUrlOrFilename;
	}

	public StorageType getStorageType() {
		return mStorageType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mStorageType == null) ? 0 : mStorageType.hashCode());
		result = prime * result + ((mUrlOrFilename == null) ? 0 : mUrlOrFilename.hashCode());
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
		RequestIdentifier other = (RequestIdentifier) obj;
		if (mStorageType != other.mStorageType)
			return false;
		if (mUrlOrFilename == null) {
			if (other.mUrlOrFilename != null)
				return false;
		} else if (!mUrlOrFilename.equals(other.mUrlOrFilename))
			return false;
		return true;
	}
}
