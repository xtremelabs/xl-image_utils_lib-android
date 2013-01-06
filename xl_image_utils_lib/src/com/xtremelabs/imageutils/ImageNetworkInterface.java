package com.xtremelabs.imageutils;

interface ImageNetworkInterface {
	void downloadImageToDisk(String url);

	void bump(String url);

	boolean isNetworkRequestPendingForUrl(String url);

	void setNetworkRequestCreator(NetworkRequestCreator networkRequestImplementer);
}