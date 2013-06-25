package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.content.Context;
import android.graphics.Bitmap;

import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;
import com.xtremelabs.imageutils.NetworkToDiskInterface.ImageDownloadResult.Result;

class DiskCache implements ImageSystemDiskCache {

	private final Context mContext;
	private final ImageDiskObserver mImageDiskObserver;

	private ImageSystemDatabase mImageSystemDatabase;
	private FileSystemManager mFileSystemManager;

	DiskCache(Context context, ImageDiskObserver imageDiskObserver) {
		mContext = context;
		mImageDiskObserver = imageDiskObserver;
	}
	
	@Override
	public ImageDownloadResult downloadImageFromInputStream(String uri, InputStream inputStream) {
		ImageDownloadResult result;

		mImageSystemDatabase.beginWrite(uri);
		ImageEntry imageEntry = mImageSystemDatabase.getEntry(uri);
		try {
			mFileSystemManager.loadStreamToFile(inputStream, imageEntry.getFileName());
			mImageSystemDatabase.endWrite(uri);
			result = new ImageDownloadResult(Result.SUCCESS);
		} catch (IOException e) {
			mImageSystemDatabase.writeFailed(uri);
			result = new ImageDownloadResult(Result.FAILURE, "Failed to download image to disk! IOException caught. Error message: " + e.getMessage());
		}

		return result;
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
