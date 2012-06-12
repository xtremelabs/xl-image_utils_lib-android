package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import com.xtremelabs.imageutils.DefaultImageDiskCacher.FileFormatException;

import android.graphics.Bitmap;



interface ImageDiskCacherInterface extends ImageInputStreamLoader {
	boolean isCached(String url);

	int getSampleSize(String url, Integer width, Integer height) throws FileNotFoundException;

	boolean synchronousDiskCacheEnabled();

	void cancelRequest(String url, ImageRequestListener listener);

	Bitmap getBitmapSynchronouslyFromDisk(String url, int sampleSize) throws FileNotFoundException, FileFormatException;

	void getBitmapAsynchronousFromDisk(String url, int sampleSize, DiskCacherListener diskCacherListener);

	void bump(String url);
	
	void setDiskCacheSize(long sizeInBytes);
}