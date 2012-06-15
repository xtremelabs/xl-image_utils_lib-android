package com.xtremelabs.imageutils;

public class FileEntry {
	private String url;
	private long lastAccessTime;
	private long size;
	
	public FileEntry(String url, long size, long lastAccessTime) {
		this.url = url;
		this.size = size;
		this.lastAccessTime = lastAccessTime;
	}
	
	public long getSize() {
		return size;
	}
	
	public long getLastAccessTime() {
		return lastAccessTime;
	}
	
	public String getUrl() {
		return url;
	}
}
