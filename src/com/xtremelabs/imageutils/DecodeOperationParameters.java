package com.xtremelabs.imageutils;


class DecodeOperationParameters {
	String mUrl;
	int mSampleSize;

	DecodeOperationParameters(String url, int sampleSize) {
		mUrl = url;
		mSampleSize = sampleSize;
	}

	@Override
	public int hashCode() {
		int hash;
		hash = 31 * mSampleSize + 17;
		hash += mUrl.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DecodeOperationParameters)) {
			return false;
		}

		DecodeOperationParameters otherObject = (DecodeOperationParameters) o;
		if (otherObject.mSampleSize == mSampleSize && otherObject.mUrl.equals(mUrl)) {
			return true;
		}
		return false;
	}
}
