package com.xtremelabs.imageutils;

interface ImageNetworkInterface {
	void loadImageToDisk(String url, NetworkImageRequestListener onComplete);

	public void cancelRequest(String url, NetworkImageRequestListener listener);

	boolean queueIfLoadingFromNetwork(String url, NetworkImageRequestListener onLoadComplete);
}