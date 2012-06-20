package com.xtremelabs.imageutils;

class EvictionQueueContainer {
	private String mUrl;
	private int mSampleSize;

	public EvictionQueueContainer(String url, int sampleSize) {
		mUrl = url;
		mSampleSize = sampleSize;
	}

	public String getUrl() {
		return mUrl;
	}

	public int getSampleSize() {
		return mSampleSize;
	}

	public String toString() {
		return "EvictionQueueContainer url, samplesize: (" + this.mUrl + ", " + this.mSampleSize + ")"; 
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof EvictionQueueContainer))
			return false;

		final EvictionQueueContainer evictionQueueContainer = (EvictionQueueContainer) o;
		final String url = evictionQueueContainer.getUrl();
		final int sampleSize = evictionQueueContainer.getSampleSize();

		return (mUrl != null && mUrl.equals(url)) && sampleSize == mSampleSize;
	}
}
