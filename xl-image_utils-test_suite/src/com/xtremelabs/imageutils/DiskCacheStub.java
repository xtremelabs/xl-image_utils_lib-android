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
	public boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
		return false;
	}

	@Override
	public void retrieveImageDetails(String uri) {
	}

	@Override
	public void getBitmapAsynchronouslyFromDisk(DecodeSignature decodeSignature, ImageReturnedFrom returnedFrom, boolean noPreviousNetworkRequest) {
	}

	@Override
	public void bumpInQueue(DecodeSignature decodeSignature) {
	}
}
