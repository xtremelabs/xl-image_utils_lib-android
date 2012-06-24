package com.xtremelabs.imageutils;

interface ImageNetworkInterface {
	void downloadImageToDisk(String url);

	public void cancelRequest(String url);
}