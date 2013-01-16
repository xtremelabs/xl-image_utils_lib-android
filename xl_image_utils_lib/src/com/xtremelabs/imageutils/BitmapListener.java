package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.widget.ImageView;

public abstract class BitmapListener {
	private final ImageLoaderListener mImageLoaderListener;

	public BitmapListener() {
		mImageLoaderListener = new ImageLoaderListener() {
			@Override
			public void onImageLoadError(String error) {
				BitmapListener.this.onImageLoadError(error);
			}

			@Override
			public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				BitmapListener.this.onImageAvailable(bitmap, returnedFrom);
			}
		};
	}

	ImageLoaderListener getImageLoaderListener() {
		return mImageLoaderListener;
	}

	public abstract void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom);

	public abstract void onImageLoadError(String error);
}
