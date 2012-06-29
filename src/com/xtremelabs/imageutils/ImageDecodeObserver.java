package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

interface ImageDecodeObserver {
	public void onImageDecoded(Bitmap bitmap, String url, int sampleSize, ImageReturnedFrom returnedFrom);
	
	public void onImageDecodeFailed(String url, int sampleSize);
}
