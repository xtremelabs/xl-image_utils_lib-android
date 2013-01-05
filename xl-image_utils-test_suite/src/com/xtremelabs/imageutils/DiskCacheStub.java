package com.xtremelabs.imageutils;

import java.io.IOException;
import java.io.InputStream;

public class DiskCacheStub implements ImageDiskCacherInterface {
	@Override
	public void downloadImageFromInputStream(String uri, InputStream inputStream) throws IOException {
	}

	@Override
	public boolean isCached(String uri) {
		return false;
	}

	@Override
	public int getSampleSize(ImageRequest imageRequest) {
		return 0;
	}

	@Override
	public void getBitmapAsynchronouslyFromDisk(String uri, int sampleSize, ImageReturnedFrom returnedFrom, boolean noPreviousNetworkRequest) {
	}

	@Override
	public void bumpOnDisk(String uri) {
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
	}

	@Override
	public Dimensions getImageDimensions(String uri) {
		return null;
	}

	@Override
	public void bumpInQueue(String uri, int sampleSize) {
	}

	@Override
	public boolean isDecodeRequestPending(DecodeOperationParameters decodeOperationParameters) {
		return false;
	}

	@Override
	public void retrieveImageDetails(String uri) {
	}
}
