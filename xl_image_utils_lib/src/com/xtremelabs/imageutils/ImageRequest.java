package com.xtremelabs.imageutils;

import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageLoader.Options;

public class ImageRequest {
	public static enum ImageRequestType {
		DEFAULT, PRECACHE_TO_DISK, PRECACHE_TO_MEMORY
	}

	private String mUri;
	private ImageView mImageView;
	private Options mOptions;
	private ImageLoaderListener mImageLoaderListener;
	private ImageRequestType mImageRequestType = ImageRequestType.DEFAULT;

	public ImageRequest(ImageView imageView, String uri) {
		mUri = uri;
		mImageView = imageView;
	}

	public ImageRequest(String uri, BitmapListener bitmapListener) {
		mImageLoaderListener = bitmapListener.getImageLoaderListener();
	}

	ImageRequest() {
	}

	public void setImageView(ImageView imageView) {
		mImageView = imageView;
	}

	public void setOptions(Options options) {
		mOptions = options;
	}

	public void setImageLoaderListener(ImageLoaderListener imageLoaderListener) {
		mImageLoaderListener = imageLoaderListener;
	}

	public void setImageRequestType(ImageRequestType imageRequestType) {
		mImageRequestType = imageRequestType;
	}

	void setUri(String uri) {
		mUri = uri;
	}

	String getUri() {
		return mUri;
	}

	ImageView getImageView() {
		return mImageView;
	}

	Options getOptions() {
		return mOptions;
	}

	ImageLoaderListener getImageLoaderListener() {
		return mImageLoaderListener;
	}

	ImageRequestType getImageRequestType() {
		return mImageRequestType;
	}
}
