/*
 * Copyright 2012 Xtreme Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
