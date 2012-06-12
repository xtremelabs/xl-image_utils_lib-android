package com.xtremelabs.imageutils;

public class ImageRequestData {
	public String url;
	public int sampleSize;
	
	@Override
	public int hashCode() {
		int result = 19;
		
		result = 31 * result + sampleSize;
		result = 31 * result + url.hashCode();
		
		return result;
	}
}
