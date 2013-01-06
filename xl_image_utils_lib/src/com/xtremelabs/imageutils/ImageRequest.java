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
	private final Options mOptions;

	public ImageRequest(String uri, ScalingInfo scalingInfo) {
		this(uri, scalingInfo, null);
	}

	public ImageRequest(String uri, ScalingInfo scalingInfo, Options options) {
		mUri = uri;
		mScalingInfo = scalingInfo;

		if (options == null) {
			mOptions = new Options();
		} else {
			mOptions = options;
		}

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
