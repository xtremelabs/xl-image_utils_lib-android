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

import com.xtremelabs.imageutils.AbstractImageLoader.Options;

class ImageRequest {
	static enum LocationOfImage {
		WEB, LOCAL_FILE_SYSTEM
	}

	static enum RequestType {
		CACHE_TO_DISK, CACHE_TO_DISK_AND_MEMORY, FULL_REQUEST
	}

	private final String mUri;
	private final ScalingInfo mScalingInfo;
	private LocationOfImage mImageRequestType;
	private RequestType mRequestType = RequestType.FULL_REQUEST;
	private final Options mOptions;

	public ImageRequest(String uri) {
		this(uri, null);
	}

	public ImageRequest(String uri, ScalingInfo scalingInfo) {
		this(uri, scalingInfo, null);
	}

	public ImageRequest(String uri, ScalingInfo scalingInfo, Options options) {
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

	void setRequestType(RequestType requestType) {
		mRequestType = requestType;
	}

	RequestType getRequestType() {
		return mRequestType;
	}

	private void setLocationOfImage() {
		if (GeneralUtils.isFileSystemUri(mUri)) {
			mImageRequestType = LocationOfImage.LOCAL_FILE_SYSTEM;
		} else {
			mImageRequestType = LocationOfImage.WEB;
		}
	}
}
