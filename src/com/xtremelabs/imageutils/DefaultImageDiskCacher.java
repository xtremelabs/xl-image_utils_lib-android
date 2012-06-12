package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

public class DefaultImageDiskCacher implements ImageDiskCacherInterface {
	private long maximumCacheSizeInBytes = 31457280; // 30MB
	private boolean synchronousDiskAccess = true;
	private DiskManager diskManager;

	public DefaultImageDiskCacher(Context appContext) {
		diskManager = new DiskManager("img", appContext);
	}

	@Override
	public synchronized boolean isCached(String url) {
		return diskManager.isOnDisk(encode(url));
	}

	@Override
	public synchronized int getSampleSize(String url, Integer width, Integer height) throws FileNotFoundException {
		try {
			File file = getFile(url);
			FileInputStream fileInputStream = new FileInputStream(file);
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(fileInputStream, null, opts);
			Point point = new Point(opts.outWidth, opts.outHeight);
			return calculateSampleSize(width, height, point);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public synchronized boolean synchronousDiskCacheEnabled() {
		return synchronousDiskAccess;
	}

	@Override
	public synchronized void cancelRequest(String url, ImageRequestListener listener) {

	}

	@Override
	public synchronized Bitmap getBitmapSynchronouslyFromDisk(String url, int sampleSize) throws FileNotFoundException, FileFormatException {
		File file = getFile(url);
		FileInputStream fileInputStream;
		fileInputStream = new FileInputStream(file);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = sampleSize;
		Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null, opts);
		if (bitmap == null) {
			file.delete();
			throw new FileFormatException();
		}
		return bitmap;
	}

	@Override
	public synchronized void getBitmapAsynchronousFromDisk(final String url, final int sampleSize, final DiskCacherListener diskCacherListener) {
		/*
		 * ImageRequestData data = new ImageRequestData(); data.url = url; data.sampleSize = sampleSize; List<DiskCacherListener> dataCacheListeners; dataCacheListeners =
		 * asyncRequestMap.get(data); if (dataCacheListeners == null) { dataCacheListeners = new }
		 * 
		 * ThreadPool.execute(new Runnable() {
		 * 
		 * @Override public void run() { Bitmap bitmap = getBitmapSynchronousFromDisk(appContext, url, sampleSize); if (bitmap != null) diskCacherListener.onImageDecoded(bitmap); }
		 * });
		 */
	}

	@Override
	public void loadImageFromInputStream(String url, InputStream inputStream) throws IOException {
		diskManager.loadStreamToFile(inputStream, encode(url));
		clearLeastUsedFilesInCache();
	}

	@Override
	public void bump(String url) {
		diskManager.touchIfExists(encode(url));
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		maximumCacheSizeInBytes = sizeInBytes;
		clearLeastUsedFilesInCache();
	}

	/**
	 * This calculates the sample size be dividing the width and the height until the first point at which information is lost. Then, it takes one step back and returns the last
	 * sample size that did not lead to any loss of information.
	 * 
	 * @param width
	 *            The image will not be scaled down to be smaller than this width. Null for no scaling by width.
	 * @param height
	 *            The image will not be scaled down to be smaller than this height. Null for no scaling by height.
	 * @param imageDimensions
	 *            The dimensions of the image, as decoded from the full image on disk.
	 * @return The calculated sample size. 1 if both height and width are null.
	 */
	private int calculateSampleSize(Integer width, Integer height, Point imageDimensions) {
		if (width == null && height == null) {
			return 1;
		}

		int sampleSize = 2;
		while ((width == null || imageDimensions.x / sampleSize >= width) && (height == null || imageDimensions.y / sampleSize >= height)) {
			sampleSize *= 2;
		}
		return sampleSize / 2;
	}

	private synchronized void clearLeastUsedFilesInCache() {
		while (diskManager.getDirectorySize() > maximumCacheSizeInBytes) {
			diskManager.deleteLeastRecentlyUsedFile();
		}
	}

	private File getFile(String url) {
		return diskManager.getFile(encode(url));
	}

	private String encode(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static class FileFormatException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2180782787028503586L;
	}
}
