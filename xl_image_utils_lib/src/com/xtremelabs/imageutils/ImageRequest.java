/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageLoader.Options;

/**
 * Encapsulates all the setting for a single request.
 */
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

	/**
	 * @param imageView
	 *            The image view to which the image should be loaded.
	 */
	public void setImageView(ImageView imageView) {
		mImageView = imageView;
	}

	public void setOptions(Options options) {
		mOptions = options;
	}

	public void setImageLoaderListener(ImageLoaderListener imageLoaderListener) {
		mImageLoaderListener = imageLoaderListener;
	}

	void setImageRequestType(ImageRequestType imageRequestType) {
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

	CacheKey getCacheKey() {
		return mCacheKey;
	}

	void setCacheKey(CacheKey cacheKey) {
		mCacheKey = cacheKey;
	}
}
