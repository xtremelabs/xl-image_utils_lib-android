package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public class ImageReturnValues {
	
	Bitmap bitmap;
	ImageReturnedFrom imageReturnFrom;
	Dimensions dimensions;
	
	
	public Bitmap getBitmap() {
		return bitmap;
	}
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	public ImageReturnedFrom getImageReturnFrom() {
		return imageReturnFrom;
	}
	public void setImageReturnFrom(ImageReturnedFrom imageReturnFrom) {
		this.imageReturnFrom = imageReturnFrom;
	}
	public Dimensions getDimensions() {
		return dimensions;
	}
	public void setDimensions(Dimensions dimensions) {
		this.dimensions = dimensions;
	}

}
