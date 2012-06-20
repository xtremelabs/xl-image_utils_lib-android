package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

interface ImageReceivedListener {
	public void onImageReceived(Bitmap bitmap);
	public void onLoadImageFailed();
}
