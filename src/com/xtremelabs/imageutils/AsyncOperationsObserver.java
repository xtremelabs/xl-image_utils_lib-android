package com.xtremelabs.imageutils;


interface AsyncOperationsObserver {
	public void onImageDecodeRequired(String url, int mSampleSize);
	
	public int getSampleSize(String url, ScalingInfo scalingInfo);

	public void cancelNetworkRequest(String url);

	public void cancelDecodeRequest(String url, int sampleSize);
}
