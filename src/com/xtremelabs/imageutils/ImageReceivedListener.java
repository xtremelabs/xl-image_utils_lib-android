package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public interface ImageReceivedListener {
	public void onImageReceived(Bitmap bitmap, String url);
	public void onLoadImageFailed();
}
