package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.content.Context;
import android.graphics.Bitmap;

import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;

class DiskCache implements ImageSystemDiskCache {

	private final Context mContext;
	private final ImageDiskObserver mImageDiskObserver;

	DiskCache(Context context, ImageDiskObserver imageDiskObserver) {
		mContext = context;
		mImageDiskObserver = imageDiskObserver;
	}
	
	@Override
	public void downloadImageFromInputStream(String url, InputStream inputStream) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isCached(CacheRequest cacheRequest) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getSampleSize(CacheRequest imageRequest) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void bumpOnDisk(String uri) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		// TODO Auto-generated method stub

	}

	@Override
	public Dimensions getImageDimensions(CacheRequest cacheRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void invalidateFileSystemUri(String uri) {
		// TODO Auto-generated method stub

	}

	@Override
	public Bitmap getBitmapSynchronouslyFromDisk(CacheRequest cacheRequest, DecodeSignature decodeSignature) throws FileNotFoundException, FileFormatException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void calculateAndSaveImageDetails(CacheRequest cacheRequest) throws URISyntaxException, FileNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public Prioritizable getDetailsPrioritizable(CacheRequest imageRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Prioritizable getDecodePrioritizable(CacheRequest cacheRequest, DecodeSignature decodeSignature, ImageReturnedFrom imageReturnedFrom) {
		// TODO Auto-generated method stub
		return null;
	}

}
