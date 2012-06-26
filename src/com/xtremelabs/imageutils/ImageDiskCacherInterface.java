package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.DefaultImageDiskCacher.FileFormatException;

/**
 * This interface defines the mechanisms that the ImageCacher uses to interract with the Disk Cache.
 *  
 * @author Jamie Halpern
 */
interface ImageDiskCacherInterface extends NetworkToDiskInterface {
	boolean isCached(String url);

	int getSampleSize(String url, Integer width, Integer height);

	Bitmap getBitmapSynchronouslyFromDisk(String url, int sampleSize) throws FileNotFoundException, FileFormatException;

	void getBitmapAsynchronouslyFromDisk(String url, int sampleSize);

	void bump(String url);

	void setDiskCacheSize(long sizeInBytes);

	Dimensions getImageDimensions(String url);

	void cancelRequest(String url, int sampleSize);
}