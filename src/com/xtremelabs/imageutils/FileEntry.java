package com.xtremelabs.imageutils;

public class FileEntry {
	private String url;
	private long lastAccessTime;
	private int width;
	private int height;
	private long size;
	
	public FileEntry(String url, long size, int width, int height, long lastAccessTime) {
		this.url = url;
		this.size = size;
		this.width = width;
		this.height = height;
		this.lastAccessTime = lastAccessTime;
	}
	
	public long getSize() {
		return size;
	}
	
	public long getLastAccessTime() {
		return lastAccessTime;
	}
	
	public Dimensions getDimensions() {
		return new Dimensions(width, height);
	}
	
	public String getUrl() {
		return url;
	}
}
