package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

interface ImageManagerListener {
	public void onImageReceived(Bitmap bitmap, boolean isFromMemoryCache);
	public void onLoadImageFailed();
}
