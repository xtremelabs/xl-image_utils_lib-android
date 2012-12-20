package com.xtremelabs.imageutils;

import java.io.InputStream;

public interface NetworkRequestCreator {
	public void getInputStream(String url, InputStreamListener listener);

	public static interface InputStreamListener {
		public void onInputStreamReady(InputStream inputStream);

		public void onFailure(String errorMessage);
	}
}
