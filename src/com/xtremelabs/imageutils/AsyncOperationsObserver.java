package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

interface AsyncOperationsObserver {
	public void onImageDecodeRequired(String url, int mSampleSize);
	
	public int getSampleSize(String url, ScalingInfo scalingInfo) throws FileNotFoundException;
	
	// TODO: Add onCancelNetworkRequest and onCancelDecodeRequest.
}
