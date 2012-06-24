package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.widget.ImageView;

interface ImageLoaderListener {
	public void onImageAvailable(ImageView imageView, Bitmap bitmap);

	public void onImageLoadError();
}
