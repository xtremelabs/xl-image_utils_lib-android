package com.xtremelabs.imageutils;

/**
 * This interface defines the mechanisms that the ImageCacher uses to interract with the Disk Cache.
 */
interface ImageDiskCacherInterface extends NetworkToDiskInterface {
	boolean isCached(String uri);

	int getSampleSize(ImageRequest imageRequest);

	void getBitmapAsynchronouslyFromDisk(DecodeSignature decodeSignature, ImageReturnedFrom returnedFrom, boolean noPreviousNetworkRequest);

	void bumpOnDisk(String uri);

	void setDiskCacheSize(long sizeInBytes);

	Dimensions getImageDimensions(String uri);

	void bumpInQueue(DecodeSignature decodeSignature);

	boolean isDecodeRequestPending(DecodeSignature decodeSignature);

	void retrieveImageDetails(String uri);
}