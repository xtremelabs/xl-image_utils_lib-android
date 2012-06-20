package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;


interface ImageInputStreamLoader {
	public void loadImageFromInputStream(String url, InputStream inputStream) throws IOException;
}
