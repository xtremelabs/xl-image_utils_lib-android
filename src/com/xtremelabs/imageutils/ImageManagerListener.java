package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

interface ImageManagerListener {
	public void onImageReceived(Bitmap bitmap);
	public void onLoadImageFailed();
}
