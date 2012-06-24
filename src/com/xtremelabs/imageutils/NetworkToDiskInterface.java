package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;


interface NetworkToDiskInterface {
	public void downloadImageFromInputStream(String url, InputStream inputStream) throws IOException;
}
