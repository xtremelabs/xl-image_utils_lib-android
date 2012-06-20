package com.xtremelabs.imageutils;

class ImageRequestData {
	public String url;
	public int sampleSize;

	@Override
	public int hashCode() {
		int result = 19;

		result = 31 * result + sampleSize;
		if (url != null) {
			result = 31 * result + url.hashCode();
		}

		return result;
	}
}
