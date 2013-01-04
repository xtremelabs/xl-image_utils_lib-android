package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

class ImageResponse {
	public static enum ImageResponseStatus {
		REQUEST_QUEUED, SUCCESS
	}

	private final ImageResponseStatus mImageResponseStatus;
	private final Bitmap mBitmap;
	private final ImageReturnedFrom mImageReturnedFrom;

	public ImageResponse(Bitmap bitmap, ImageReturnedFrom imageReturnedFrom, ImageResponseStatus imageResponseStatus) {
		mBitmap = bitmap;
		mImageReturnedFrom = imageReturnedFrom;
		mImageResponseStatus = imageResponseStatus;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public ImageReturnedFrom getImageReturnedFrom() {
		return mImageReturnedFrom;
	}

	public ImageResponseStatus getImageResponseStatus() {
		return mImageResponseStatus;
	}
}
