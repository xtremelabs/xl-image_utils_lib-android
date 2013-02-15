package com.xtremelabs.imageutils;

import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageLoader.Options;

public class ImageRequest {
	private String mUri;
	private ImageView mImageView;
	private Options mOptions;
	private ImageLoaderListener mImageLoaderListener;
	private ImageRequestType mImageRequestType = ImageRequestType.DEFAULT;
	private CacheKey mCacheKey;

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

	public CacheKey getCacheKey() {
		return mCacheKey;
	}

	public void setCacheKey(CacheKey cacheKey) {
		mCacheKey = cacheKey;
	}
}
