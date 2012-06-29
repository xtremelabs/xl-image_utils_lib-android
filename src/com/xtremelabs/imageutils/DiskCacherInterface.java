package com.xtremelabs.imageutils;


/**
 * This interface defines the mechanisms that the ImageCacher uses to interract with the Disk Cache.
 *  
 * @author Jamie Halpern
 */
interface DiskCacherInterface extends NetworkToDiskInterface {
	boolean isCached(String url);

	int getSampleSize(String url, Integer width, Integer height);

	void getBitmapAsynchronouslyFromDisk(String url, int sampleSize, ImageReturnedFrom returnedFrom, boolean noPreviousNetworkRequest);

	void bumpOnDisk(String url);

	void setDiskCacheSize(long sizeInBytes);

	Dimensions getImageDimensions(String url);

	void cancelRequest(String url, int sampleSize);

	void bumpInQueue(String url, int sampleSize);
}