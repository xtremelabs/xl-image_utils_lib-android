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
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDiskCacher";
	private long mMaximumCacheSizeInBytes = 30 * 1024 * 1024; // 30MB
	private boolean mSynchronousDiskAccess = true;
	private DiskManager mDiskManager;
	private DiskCacheDatabaseHelper mDatabaseHelper;

	public DefaultImageDiskCacher(Context appContext) {
		mDiskManager = new DiskManager("img", appContext);
		mDatabaseHelper = new DiskCacheDatabaseHelper(appContext);
	}

	@Override
	public boolean isCached(String url) {
		return mDiskManager.isOnDisk(encode(url));
	}

	@Override
	public int getSampleSize(String url, Integer width, Integer height) throws FileNotFoundException {
		FileInputStream fileInputStream = null;
		try {
			File file = getFile(url);
			fileInputStream = new FileInputStream(file);
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(fileInputStream, null, opts);
			Point point = new Point(opts.outWidth, opts.outHeight);
			return calculateSampleSize(width, height, point);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public synchronized boolean synchronousDiskCacheEnabled() {
		return mSynchronousDiskAccess;
	}

//	@Override
//	public synchronized void cancelRequest(String url, ImageRequestListener listener) {
//
//	}

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
	public void getBitmapAsynchronousFromDisk(final String url, final int sampleSize, final DiskCacherListener diskCacherListener) {
		/*
		 * ImageRequestData data = new ImageRequestData(); data.url = url; data.sampleSize = sampleSize; List<DiskCacherListener> dataCacheListeners; dataCacheListeners =
		 * asyncRequestMap.get(data); if (dataCacheListeners == null) { dataCacheListeners = new }
		 * 
		 * ThreadPool.execute(new Runnable() {
		 * 
		 * @Override public void run() { Bitmap bitmap = getBitmapSynchronousFromDisk(appContext, url, sampleSize); if (bitmap != null) diskCacherListener.onImageDecoded(bitmap); }
		 * });
		 */

		ThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Bitmap bitmap = getBitmapSynchronouslyFromDisk(url, sampleSize);
					diskCacherListener.onImageDecoded(bitmap);
				} catch (FileNotFoundException e) {
				} catch (FileFormatException e) {
				}
			}
		});
	}

	@Override
	public void downloadImageFromInputStream(String url, InputStream inputStream) throws IOException {
		mDiskManager.loadStreamToFile(inputStream, encode(url));
		File file = mDiskManager.getFile(encode(url));
		if (!mDatabaseHelper.addFile(url, file.length(), System.currentTimeMillis())) {
			mDatabaseHelper.updateFile(url, System.currentTimeMillis());
		}
		clearLeastUsedFilesInCache();
	}

	@Override
	public void bump(String url) {
		mDatabaseHelper.updateFile(url, System.currentTimeMillis());
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		mMaximumCacheSizeInBytes = sizeInBytes;
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
		sampleSize /= 2;
		return sampleSize;
	}

	private void clearLeastUsedFilesInCache() {
		while (mDatabaseHelper.getTotalSizeOnDisk() > mMaximumCacheSizeInBytes) {
			String url = mDatabaseHelper.getLRU().getUrl();
			mDiskManager.deleteFile(encode(url));
			mDatabaseHelper.removeFile(url);
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
	public Dimensions getImageDimensions(String url) throws FileNotFoundException {
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
