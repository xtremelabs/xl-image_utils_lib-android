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

import com.xtremelabs.imageutils.ImageLoader.Options;

class CacheRequest {
	static enum LocationOfImage {
		WEB, LOCAL_FILE_SYSTEM
	}

	private final String mUri;
	private final ScalingInfo mScalingInfo;
	private LocationOfImage mImageRequestType;
	private ImageRequestType mRequestType = ImageRequestType.DEFAULT;
	private final Options mOptions;
	private CacheKey mCacheKey;

	public CacheRequest(String uri) {
		this(uri, null);
	}

	public CacheRequest(String uri, ScalingInfo scalingInfo) {
		this(uri, scalingInfo, null);
	}

	public CacheRequest(String uri, ScalingInfo scalingInfo, Options options) {
		mUri = uri;

		if (scalingInfo == null) {
			mScalingInfo = new ScalingInfo();
		} else {
			mScalingInfo = scalingInfo;
		}

		if (options == null) {
			mOptions = new Options();
		} else {
			mOptions = options;
		}

		setLocationOfImage();
	}

	public String getUri() {
		return mUri;
	}

	public LocationOfImage getImageRequestType() {
		return mImageRequestType;
	}

	public Options getOptions() {
		return mOptions;
	}

	public ScalingInfo getScalingInfo() {
		return mScalingInfo;
	}

	void setRequestType(ImageRequestType requestType) {
		mRequestType = requestType;
	}

	ImageRequestType getRequestType() {
		return mRequestType;
	}

	private void setLocationOfImage() {
		if (GeneralUtils.isFileSystemUri(mUri)) {
			mImageRequestType = LocationOfImage.LOCAL_FILE_SYSTEM;
		} else {
			mImageRequestType = LocationOfImage.WEB;
		}
	}

	public CacheKey getCacheKey() {
		return mCacheKey;
	}

	public void setCacheKey(CacheKey cacheKey) {
		mCacheKey = cacheKey;
	}
}
