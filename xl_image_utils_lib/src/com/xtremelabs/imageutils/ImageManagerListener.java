package com.xtremelabs.imageutils;

interface ImageManagerListener {
	public void onImageReceived(ImageResponse imageResponse);

	public void onLoadImageFailed(String error);
}
