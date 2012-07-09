/*
 * Copyright 2012 Xtreme Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class DiskLRUCacher implements ImageDiskCacherInterface {
	private long mMaximumCacheSizeInBytes = 30 * 1024 * 1024; // 30MB
	private DiskManager mDiskManager;
	private DiskCacheDatabaseHelper mDatabaseHelper;
	private ImageDecodeObserver mImageDecodeObserver;
	private CachedImagesMap mCachedImagesMap = new CachedImagesMap();
	private HashMap<DecodeOperationParameters, Runnable> mRequestToRunnableMap = new HashMap<DecodeOperationParameters, Runnable>();

	private LifoThreadPool mThreadPool = new LifoThreadPool(1);

	public DiskLRUCacher(Context appContext, ImageDecodeObserver imageDecodeObserver) {
		mDiskManager = new DiskManager("img", appContext);
		mDatabaseHelper = new DiskCacheDatabaseHelper(appContext);
		mImageDecodeObserver = imageDecodeObserver;

		List<FileEntry> entries = mDatabaseHelper.getAllEntries();
		for (FileEntry entry : entries) {
			if (mDiskManager.isOnDisk(encode(entry.getUrl()))) {
				mCachedImagesMap.putDimensions(entry.getUrl(), entry.getDimensions());
			}
		}
	}

	@Override
	public boolean isCached(String url) {
		return mCachedImagesMap.isCached(url);
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
	public void getBitmapAsynchronouslyFromDisk(final String url, final int sampleSize, final ImageReturnedFrom returnedFrom,
			final boolean noPreviousNetworkRequest) {
		final DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				boolean failed = false;
				Bitmap bitmap = null;
				try {
					bitmap = getBitmapSynchronouslyFromDisk(url, sampleSize);
				} catch (FileNotFoundException e) {
					failed = true;
				} catch (FileFormatException e) {
					failed = true;
				}
				removeRequestFromMap(decodeOperationParameters);

				if (!failed) {
					mImageDecodeObserver.onImageDecoded(bitmap, url, sampleSize, returnedFrom);
				} else {
					mDiskManager.deleteFile(encode(url));
					mImageDecodeObserver.onImageDecodeFailed(url, sampleSize);
				}
			}
		};

		if (mapRunnableToParameters(runnable, decodeOperationParameters)) {
			mThreadPool.execute(runnable);
		}
	}

	@Override
	public void downloadImageFromInputStream(String url, InputStream inputStream) throws IOException {
		mDiskManager.loadStreamToFile(inputStream, encode(url));
		File file = mDiskManager.getFile(encode(url));
		Dimensions dimensions = getImageDimensionsFromDisk(url);
		mCachedImagesMap.putDimensions(url, dimensions);
		mDatabaseHelper.addOrUpdateFile(url, file.length(), dimensions.getWidth(), dimensions.getHeight());
		clearLeastUsedFilesInCache();
	}

	@Override
	public void bumpOnDisk(String url) {
		mDatabaseHelper.updateFile(url);
	}

	@Override
	public void bumpInQueue(String url, int sampleSize) {
		DecodeOperationParameters parameters = new DecodeOperationParameters(url, sampleSize);
		synchronized (mRequestToRunnableMap) {
			mThreadPool.bump(mRequestToRunnableMap.get(parameters));
		}
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		mMaximumCacheSizeInBytes = sizeInBytes;
		clearLeastUsedFilesInCache();
	}

	@Override
	public Dimensions getImageDimensions(String url) {
		Dimensions dimensions = mCachedImagesMap.getImageDimensions(url);
		return dimensions;
	}

	private boolean mapRunnableToParameters(Runnable runnable, DecodeOperationParameters parameters) {
		synchronized (mRequestToRunnableMap) {
			if (!mRequestToRunnableMap.containsKey(parameters)) {
				mRequestToRunnableMap.put(parameters, runnable);
				return true;
			} else {
				return false;
			}
		}
	}

	private void removeRequestFromMap(DecodeOperationParameters parameters) {
		synchronized (mRequestToRunnableMap) {
			mRequestToRunnableMap.remove(parameters);
		}
	}

	private Bitmap getBitmapSynchronouslyFromDisk(String url, int sampleSize) throws FileNotFoundException, FileFormatException {
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
			mCachedImagesMap.removeDimensions(url);
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

	public static class FileFormatException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2180782787028503586L;
	}

	@Override
	public synchronized boolean isDecodeRequestPending(DecodeOperationParameters decodeOperationParameters) {
		return mRequestToRunnableMap.containsKey(decodeOperationParameters);
	}
}
