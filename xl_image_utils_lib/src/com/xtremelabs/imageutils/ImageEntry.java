package com.xtremelabs.imageutils;

class ImageEntry {
	public long id;
	public String uri;
	public boolean onDisk = false;
	public long creationTime = System.currentTimeMillis();
	public long lastAccessedTime = creationTime;
	public int sizeX = -1;
	public int sizeY = -1;
	public long fileSize;
	public long expiry = Long.MAX_VALUE;

	String getFileName() {
		return Long.toString(id);
	}

	public boolean hasDetails() {
		return sizeX != -1 || sizeY != -1 || fileSize != 0;
	}
}
