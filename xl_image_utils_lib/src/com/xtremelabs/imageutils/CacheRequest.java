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
	private static final String FILE_SCHEME = "file:";

	static enum LocationOfImage {
		WEB, LOCAL_FILE_SYSTEM
	}

	private final String mUri;
	private final ScalingInfo mScalingInfo;
	private LocationOfImage mLocationOfImage;
	private ImageRequestType mImageRequestType = ImageRequestType.DEFAULT;
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

	public LocationOfImage getLocationOfImage() {
		return mLocationOfImage;
	}

	public Options getOptions() {
		return mOptions;
	}

	public ScalingInfo getScalingInfo() {
		return mScalingInfo;
	}

	void setImageRequestType(ImageRequestType requestType) {
		mImageRequestType = requestType;
	}

	ImageRequestType getImageRequestType() {
		return mImageRequestType;
	}

	private void setLocationOfImage() {
		if (isFileSystemUri(mUri)) {
			mLocationOfImage = LocationOfImage.LOCAL_FILE_SYSTEM;
		} else {
			mLocationOfImage = LocationOfImage.WEB;
		}
	}

	public CacheKey getCacheKey() {
		return mCacheKey;
	}

	public void setCacheKey(CacheKey cacheKey) {
		mCacheKey = cacheKey;
	}

	public boolean isFileSystemRequest() {
		return mLocationOfImage == LocationOfImage.LOCAL_FILE_SYSTEM;
	}

	public boolean isPrecacheRequest() {
		boolean isPrecacheRequest;
		switch (mImageRequestType) {
		case PRECACHE_TO_DISK:
		case PRECACHE_TO_DISK_FOR_ADAPTER:
		case PRECACHE_TO_MEMORY:
		case PRECACHE_TO_MEMORY_FOR_ADAPTER:
		case DEPRIORITIZED:
		case DEPRIORITIZED_PRECACHE_TO_MEMORY_FOR_ADAPTER:
		case DEPRIORITIZED_PRECACHE_TO_DISK_FOR_ADAPTER:
			isPrecacheRequest = true;
			break;
		case DEFAULT:
		case ADAPTER_REQUEST:
		default:
			isPrecacheRequest = false;
			break;
		}

		return isPrecacheRequest;
	}

	private static boolean isFileSystemUri(String uri) {
		if (uri != null) {
			int fileSchemeLength = FILE_SCHEME.length();
			if (uri.length() < fileSchemeLength)
				return false;

			for (int i = 0; i < fileSchemeLength; i++) {
				if (uri.charAt(i) != FILE_SCHEME.charAt(i))
					return false;
			}
			return true;
		} else {
			return false;
		}
	}
}
