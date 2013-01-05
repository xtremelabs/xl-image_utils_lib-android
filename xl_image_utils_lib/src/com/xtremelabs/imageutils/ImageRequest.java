package com.xtremelabs.imageutils;

import java.net.URI;
import java.net.URISyntaxException;

import com.xtremelabs.imageutils.AbstractImageLoader.Options;

class ImageRequest {
	public static enum ImageRequestType {
		WEB, LOCAL
	}

	private final String mUri;
	private final ScalingInfo mScalingInfo;
	private ImageRequestType mImageRequestType;
	private Options mOptions;

	public ImageRequest(String uri, ScalingInfo scalingInfo) {
		if (GeneralUtils.isStringBlank(uri)) {
			throw new IllegalArgumentException("You cannot create an ImageRequest with a null or empty URI!");
		}

		if (scalingInfo == null) {
			throw new IllegalArgumentException("You cannot create an ImageRequest with null ScalingInfo!");
		}

		mUri = uri;
		mScalingInfo = scalingInfo;

		setRequestType();
	}

	public String getUri() {
		return mUri;
	}

	public ImageRequestType getImageRequestType() {
		return mImageRequestType;
	}

	public Options getOptions() {
		return mOptions;
	}

	public ScalingInfo getScalingInfo() {
		return mScalingInfo;
	}

	public void setOptions(Options options) {
		mOptions = options;
	}

	private void setRequestType() {
		try {
			URI uri = new URI(mUri);
			String scheme = uri.getScheme();
			if (scheme != null && scheme.equals("file")) {
				mImageRequestType = ImageRequestType.LOCAL;
			} else {
				mImageRequestType = ImageRequestType.WEB;
			}
		} catch (URISyntaxException e) {
			mImageRequestType = ImageRequestType.WEB;
		}

	}
}
