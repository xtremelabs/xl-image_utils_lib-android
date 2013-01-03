package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public class MemCacheStub implements ImageMemoryCacherInterface {

	@Override
	public Bitmap getBitmap(String url, int sampleSize) {
		return null;
	}

	@Override
	public void cacheBitmap(Bitmap bitmap, String url, int sampleSize) {
	}

	@Override
	public void clearCache() {
	}

	@Override
	public void setMaximumCacheSize(long size) {
	}

}
