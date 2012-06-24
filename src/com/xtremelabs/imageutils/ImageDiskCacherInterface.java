package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.DefaultImageDiskCacher.FileFormatException;

interface ImageDiskCacherInterface extends NetworkToDiskInterface {
	boolean isCached(String url);

	int getSampleSize(String url, Integer width, Integer height) throws FileNotFoundException;

//	void cancelRequest(String url, ICListener listener);

	Bitmap getBitmapSynchronouslyFromDisk(String url, int sampleSize) throws FileNotFoundException, FileFormatException;

	void getBitmapAsynchronouslyFromDisk(String url, int sampleSize);

	void bump(String url);

	void setDiskCacheSize(long sizeInBytes);

	Dimensions getImageDimensions(String url) throws FileNotFoundException;
}