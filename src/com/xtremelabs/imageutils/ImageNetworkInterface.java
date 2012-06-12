package com.xtremelabs.imageutils;

public interface ImageNetworkInterface {
	void loadImageToDisk(String url, NetworkImageRequestListener onComplete);

	public void cancelRequest(String url, NetworkImageRequestListener listener);
}