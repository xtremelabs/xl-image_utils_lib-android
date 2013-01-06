package com.xtremelabs.imageutils;

import java.util.HashMap;

class CachedImagesMap {
	private final HashMap<String, Dimensions> imageDimensionsMap = new HashMap<String, Dimensions>();

	public synchronized void putDimensions(String url, Dimensions dimensions) {
		imageDimensionsMap.put(url, dimensions);
	}

	public synchronized Dimensions getImageDimensions(String url) {
		return imageDimensionsMap.get(url);
	}

	public synchronized void removeDimensions(String url) {
		imageDimensionsMap.remove(url);
	}

	public synchronized boolean isCached(String url) {
		return imageDimensionsMap.containsKey(url);
	}
}
