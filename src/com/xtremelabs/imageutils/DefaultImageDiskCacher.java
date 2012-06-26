package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class DefaultImageDiskCacher implements ImageDiskCacherInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDiskCacher";
	private long mMaximumCacheSizeInBytes = 30 * 1024 * 1024; // 30MB
	private DiskManager mDiskManager;
	private DiskCacheDatabaseHelper mDatabaseHelper;
	private ImageDecodeObserver mImageDecodeObserver;
	private ImageDimensionsMap mImageDimensionsMap = new ImageDimensionsMap();

	private ThreadPool mThreadPool = new ThreadPool(5);

	public DefaultImageDiskCacher(Context appContext, ImageDecodeObserver imageDecodeObserver) {
		mDiskManager = new DiskManager("img", appContext);
		mDatabaseHelper = new DiskCacheDatabaseHelper(appContext);
		mImageDecodeObserver = imageDecodeObserver;

		List<FileEntry> entries = mDatabaseHelper.getAllEntries();
		for (FileEntry entry : entries) {
			mImageDimensionsMap.putDimensions(entry.getUrl(), entry.getDimensions());
		}
	}

	@Override
	public boolean isCached(String url) {
		return mDiskManager.isOnDisk(encode(url));
	}

	@Override
	public int getSampleSize(String url, Integer width, Integer height) {
		Dimensions dimensions = getImageDimensions(url);
		if (dimensions == null) {
			return -1;
		}
		int sampleSize = calculateSampleSize(width, height, dimensions);
		return sampleSize;
	}

	@Override
	public void cancelRequest(String url, int sampleSize) {
		// TODO: Cancel pending disk requests here.
	}

	@Override
	public Bitmap getBitmapSynchronouslyFromDisk(String url, int sampleSize) throws FileNotFoundException, FileFormatException {
		File file = getFile(url);
		FileInputStream fileInputStream = null;
		fileInputStream = new FileInputStream(file);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = sampleSize;
		Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null, opts);
		if (fileInputStream != null) {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (bitmap == null) {
			file.delete();
			throw new FileFormatException();
		}
		return bitmap;
	}

	@Override
	public void getBitmapAsynchronouslyFromDisk(final String url, final int sampleSize) {
		/*
		 * ImageRequestData data = new ImageRequestData(); data.url = url; data.sampleSize = sampleSize; List<DiskCacherListener> dataCacheListeners;
		 * dataCacheListeners = asyncRequestMap.get(data); if (dataCacheListeners == null) { dataCacheListeners = new }
		 * 
		 * ThreadPool.execute(new Runnable() {
		 * 
		 * @Override public void run() { Bitmap bitmap = getBitmapSynchronousFromDisk(appContext, url, sampleSize); if (bitmap != null)
		 * diskCacherListener.onImageDecoded(bitmap); } });
		 */
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					Bitmap bitmap = getBitmapSynchronouslyFromDisk(url, sampleSize);
					mImageDecodeObserver.onImageDecoded(bitmap, url, sampleSize);
				} catch (FileNotFoundException e) {
					mImageDecodeObserver.onImageDecodeFailed(url, sampleSize);
				} catch (FileFormatException e) {
					mImageDecodeObserver.onImageDecodeFailed(url, sampleSize);
				}
			}
		};

		mThreadPool.execute(runnable);
	}

	@Override
	public void downloadImageFromInputStream(String url, InputStream inputStream) throws IOException {
		mDiskManager.loadStreamToFile(inputStream, encode(url));
		File file = mDiskManager.getFile(encode(url));
		Dimensions dimensions = getImageDimensionsFromDisk(url);
		mImageDimensionsMap.putDimensions(url, dimensions);
		mDatabaseHelper.addOrUpdateFile(url, file.length(), dimensions.getWidth(), dimensions.getHeight());
		clearLeastUsedFilesInCache();
	}

	@Override
	public void bump(String url) {
		mDatabaseHelper.updateFile(url);
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		mMaximumCacheSizeInBytes = sizeInBytes;
		clearLeastUsedFilesInCache();
	}

	/**
	 * This calculates the sample size be dividing the width and the height until the first point at which information is lost. Then, it takes one step back and
	 * returns the last sample size that did not lead to any loss of information.
	 * 
	 * @param width
	 *            The image will not be scaled down to be smaller than this width. Null for no scaling by width.
	 * @param height
	 *            The image will not be scaled down to be smaller than this height. Null for no scaling by height.
	 * @param imageDimensions
	 *            The dimensions of the image, as decoded from the full image on disk.
	 * @return The calculated sample size. 1 if both height and width are null.
	 */
	private int calculateSampleSize(Integer width, Integer height, Dimensions imageDimensions) {
		if (width == null && height == null) {
			return 1;
		}

		int sampleSize = 2;
		while ((width == null || imageDimensions.getWidth() / sampleSize >= width) && (height == null || imageDimensions.getHeight() / sampleSize >= height)) {
			sampleSize *= 2;
		}
		sampleSize /= 2;
		return sampleSize;
	}

	private void clearLeastUsedFilesInCache() {
		while (mDatabaseHelper.getTotalSizeOnDisk() > mMaximumCacheSizeInBytes) {
			String url = mDatabaseHelper.getLRU().getUrl();
			mDiskManager.deleteFile(encode(url));
			mDatabaseHelper.removeFile(url);
			mImageDimensionsMap.removeDimensions(url);
		}
	}

	private File getFile(String url) {
		return mDiskManager.getFile(encode(url));
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

	@Override
	public Dimensions getImageDimensions(String url) {
		Dimensions dimensions = mImageDimensionsMap.getImageDimensions(url);
		return dimensions;
	}

	private Dimensions getImageDimensionsFromDisk(String url) throws FileNotFoundException {
		try {
			FileInputStream fileInputStream;
			fileInputStream = new FileInputStream(getFile(url));
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(fileInputStream, null, o);
			return new Dimensions(o.outWidth, o.outHeight);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw e;
		}
	}
}
