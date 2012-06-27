package com.xtremelabs.imageutils;

interface ImageNetworkInterface {
	void downloadImageToDisk(String url);

	void cancelRequest(String url);
}