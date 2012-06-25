package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

interface AsyncOperationsObserver {
	public void onImageDecodeRequired(String url, int mSampleSize);
	
	public int getSampleSize(String url, ScalingInfo scalingInfo) throws FileNotFoundException;

	public void cancelNetworkRequest(String url);
	
	// TODO: Add onCancelDecodeRequest.
}
