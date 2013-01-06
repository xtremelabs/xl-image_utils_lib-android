package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public interface ImageMemoryCacherInterface {
	Bitmap getBitmap(DecodeSignature decodeSignature);

	void cacheBitmap(Bitmap bitmap, DecodeSignature decodeSignature);

	void clearCache();

	void setMaximumCacheSize(long size);
}
