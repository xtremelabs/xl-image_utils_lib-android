package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public interface ImageRequestListener {

	void onImageAvailable(Bitmap bitmap);
	
	void onFailure();
	
}
