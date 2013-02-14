package com.xtremelabs.imageutils;

import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageLoader.Options;

public class ImageRequest {
	public static enum ImageRequestType {
		DEFAULT, PRECACHE_TO_DISK, PRECACHE_TO_MEMORY, ADAPTER_REQUEST, PRECACHE_TO_DISK_FOR_ADAPTER, PRECACHE_TO_MEMORY_FOR_ADAPTER
	}

	private String mUri;
	private ImageView mImageView;
	private Options mOptions;
	private ImageLoaderListener mImageLoaderListener;
	private ImageRequestType mImageRequestType = ImageRequestType.DEFAULT;
	private int mPosition;
	private int mPrecacheQueueLimit;

	public ImageRequest(ImageView imageView, String uri) {
		mUri = uri;
		mImageView = imageView;
	}

	public ImageRequest(String uri, BitmapListener bitmapListener) {
		mImageLoaderListener = bitmapListener.getImageLoaderListener();
	}

	public ImageRequest(String uri) {
		mUri = uri;
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
		if (imageRequestType == null)
			mImageRequestType = ImageRequestType.DEFAULT;
		else
			mImageRequestType = imageRequestType;
	}

	void setUri(String uri) {
		mUri = uri;
	}

	void setPosition(int position) {
		mPosition = position;
	}

	void setPrecacheQueueLimit(int precacheQueueLimit) {
		mPrecacheQueueLimit = precacheQueueLimit;
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

	int getPosition() {
		return mPosition;
	}

	int getPrecacheQueueLimit() {
		return mPrecacheQueueLimit;
	}
}
