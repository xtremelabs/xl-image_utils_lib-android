package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public class MemCacheStub implements ImageMemoryCacherInterface {

	@Override
	public void clearCache() {
	}

	@Override
	public void setMaximumCacheSize(long size) {
	}

	@Override
	public Bitmap getBitmap(DecodeSignature decodeSignature) {
		return null;
	}

	@Override
	public void cacheBitmap(Bitmap bitmap, DecodeSignature decodeSignature) {
	}
}
