package com.xtremelabs.imageutils;

public class NetworkStub implements ImageNetworkInterface {

	@Override
	public void downloadImageToDisk(String url) {
	}

	@Override
	public void bump(String url) {
	}

	@Override
	public boolean isNetworkRequestPendingForUrl(String url) {
		return false;
	}
}
