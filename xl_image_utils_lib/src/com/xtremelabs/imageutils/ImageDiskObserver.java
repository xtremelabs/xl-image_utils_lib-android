package com.xtremelabs.imageutils;

import android.graphics.Bitmap;

public interface ImageDiskObserver {
	public void onImageDecoded(DecodeSignature decodeSignature, Bitmap bitmap, ImageReturnedFrom returnedFrom);

	public void onImageDecodeFailed(DecodeSignature decodeSignature, String error);

	public void onImageDetailsRequestFailed(String uri, String errorMessage);

	public void onImageDetailsRetrieved(String uri);
}
