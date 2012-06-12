package com.xtremelabs.imageutils;

import android.graphics.Bitmap;


interface ImageMemoryCacherInterface {
	boolean isCached(String url, int sampleSize);

	Bitmap getBitmap(String url, int sampleSize);

	void cacheBitmap(Bitmap bitmap, String url, int sampleSize);
	
	void clearCache();

	void setMaximumCacheSize(int numImages);
}
