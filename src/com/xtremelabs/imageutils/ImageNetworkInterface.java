package com.xtremelabs.imageutils;

interface ImageNetworkInterface {
	void downloadImageToDisk(String url, NetworkImageRequestListener onComplete);

	public void cancelRequest(String url, NetworkImageRequestListener listener);

	boolean queueIfDownloadingFromNetwork(String url, NetworkImageRequestListener onLoadComplete);
}