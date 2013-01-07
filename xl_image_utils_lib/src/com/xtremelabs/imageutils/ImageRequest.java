package com.xtremelabs.imageutils;

import java.net.URI;
import java.net.URISyntaxException;

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
		try {
			URI uri = new URI(mUri);
			String scheme = uri.getScheme();
			if (scheme != null && scheme.equals("file")) {
				mImageRequestType = LocationOfImage.LOCAL_FILE_SYSTEM;
			} else {
				mImageRequestType = LocationOfImage.WEB;
			}
		} catch (URISyntaxException e) {
			mImageRequestType = LocationOfImage.WEB;
		}

	}
}
