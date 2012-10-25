package com.xtremelabs.imageutils;

import android.widget.ImageView;

public interface ImageLoaderValueListener {
	
	public void onImageAvailable(ImageView imageView, ImageReturnValues returnValues);

	public void onImageLoadError(String error);

}
