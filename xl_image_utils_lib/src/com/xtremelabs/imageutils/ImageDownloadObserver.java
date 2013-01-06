package com.xtremelabs.imageutils;

interface ImageDownloadObserver {
	public void onImageDownloaded(String url);

	public void onImageDownloadFailed(String url, String error);
}
