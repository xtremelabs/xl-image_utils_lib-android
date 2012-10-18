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

import com.xtremelabs.imageutils.DiskDatabaseHelper.DiskDatabaseHelperObserver;

public class DiskLRUCacher implements ImageDiskCacherInterface {
	private long mMaximumCacheSizeInBytes = 30 * 1024 * 1024; // 30MB
	private final DiskLRUFilesManager mDiskLRUManager;
	private final PermanentStorageDiskManager mPermanentStorageDiskManager;
	private final DiskDatabaseHelper mDatabaseHelper;
	private final ImageDecodeObserver mImageDecodeObserver;
	private final CachedImagesMap mCachedImagesMap = new CachedImagesMap();
	private final HashMap<DecodeOperationParameters, Runnable> mRequestToRunnableMap = new HashMap<DecodeOperationParameters, Runnable>();

	/*
	 * WARNING: Increasing the number of threads for image decoding will lag the UI thread.
	 * 
	 * It is highly recommended to leave the number of decode threads at one. Increasing this number too high will cause performance problems.
	 */
	private final LifoThreadPool mThreadPool = new LifoThreadPool(1);

	public DiskLRUCacher(Context applicationContext, ImageDecodeObserver imageDecodeObserver) {
		mImageDecodeObserver = imageDecodeObserver;

		mDiskLRUManager = new DiskLRUFilesManager("img", applicationContext);
		mPermanentStorageDiskManager = new PermanentStorageDiskManager(applicationContext);

		mDatabaseHelper = new DiskDatabaseHelper(applicationContext, mDiskDatabaseHelperObserver);

		List<FileEntry> entries = mDatabaseHelper.getAllEntries();
		for (FileEntry entry : entries) {
			if (mDiskLRUManager.isOnDisk(encode(entry.getUrl()))) {
				mCachedImagesMap.putDimensions(entry.getUrl(), entry.getDimensions());
			}
		}
	}

	@Override
	public boolean isCached(RequestIdentifier requestIdentifier) {
		return mCachedImagesMap.isCached(requestIdentifier);
	}

	@Override
	public int getSampleSize(RequestIdentifier requestIdentifier, Integer width, Integer height) {
		Dimensions dimensions = getImageDimensions(requestIdentifier);
		if (dimensions == null) {
			return -1;
		}
		int sampleSize = calculateSampleSize(width, height, dimensions);
		return sampleSize;
	}

	@Override
	public void getBitmapAsynchronouslyFromDisk(final RequestIdentifier requestIdentifier, final int sampleSize, final ImageReturnedFrom returnedFrom, final boolean noPreviousNetworkRequest) {
		final DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, sampleSize);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				boolean failed = false;
				String errorMessage = null;
				Bitmap bitmap = null;
				try {
					bitmap = getBitmapSynchronouslyFromDisk(requestIdentifier, sampleSize);
				} catch (FileNotFoundException e) {
					failed = true;
					errorMessage = "Disk decode failed with error message: " + e.getMessage();
				} catch (FileFormatException e) {
					failed = true;
					errorMessage = "Disk decode failed with error message: " + e.getMessage();
				}
				removeRequestFromMap(decodeOperationParameters);

				if (!failed) {
					mImageDecodeObserver.onImageDecoded(bitmap, requestIdentifier, sampleSize, returnedFrom);
				} else {
					mDiskLRUManager.deleteFile(encode(requestIdentifier));
					mImageDecodeObserver.onImageDecodeFailed(requestIdentifier, sampleSize, errorMessage);
				}
			}
		};

		if (mapRunnableToParameters(runnable, decodeOperationParameters)) {
			mThreadPool.execute(runnable);
		}
	}

	@Override
	public void downloadImageFromInputStream(RequestIdentifier requestIdentifier, InputStream inputStream) throws IOException {
		mDiskLRUManager.loadStreamToFile(inputStream, encode(requestIdentifier));
		File file = mDiskLRUManager.getFile(encode(requestIdentifier));
		Dimensions dimensions = getImageDimensionsFromDisk(requestIdentifier);
		mCachedImagesMap.putDimensions(requestIdentifier, dimensions);
		mDatabaseHelper.addOrUpdateFile(requestIdentifier, file.length(), dimensions.getWidth(), dimensions.getHeight());
		clearLeastUsedFilesInCache();
	}

	@Override
	public void bumpOnDisk(RequestIdentifier requestIdentifier) {
		mDatabaseHelper.updateFile(requestIdentifier);
	}

	@Override
	public void bumpInStack(RequestIdentifier requestIdentifier, int sampleSize) {
		DecodeOperationParameters parameters = new DecodeOperationParameters(requestIdentifier, sampleSize);
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
	public Dimensions getImageDimensions(RequestIdentifier requestIdentifier) {
		Dimensions dimensions = mCachedImagesMap.getImageDimensions(requestIdentifier);
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

	private Bitmap getBitmapSynchronouslyFromDisk(RequestIdentifier requestIdentifier, int sampleSize) throws FileNotFoundException, FileFormatException {
		File file = getFile(requestIdentifier);
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
	 * This calculates the sample size be dividing the width and the height until the first point at which information is lost. Then, it takes one step back and returns the last sample size that did not lead to any loss
	 * of information.
	 * 
	 * @param width
	 *            The image will not be scaled down to be smaller than this width. Null for no scaling by width.
	 * @param height
	 *            The image will not be scaled down to be smaller than this height. Null for no scaling by height.
	 * @param imageDimensions
	 *            The dimensions of the image, as decoded from the full image on disk.
	 * @return The calculated sample size. 1 if both height and width are null.
	 */
	public int calculateSampleSize(Integer width, Integer height, Dimensions imageDimensions) {
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

	private synchronized void clearLeastUsedFilesInCache() {
		while (mDatabaseHelper.getTotalSizeOnDisk() > mMaximumCacheSizeInBytes) {
			RequestIdentifier requestIdentifier = mDatabaseHelper.getLRU().getUrl();
			mDiskLRUManager.deleteFile(encode(requestIdentifier));
			mDatabaseHelper.removeFile(requestIdentifier);
			mCachedImagesMap.removeDimensions(requestIdentifier);
		}
	}

	private File getFile(RequestIdentifier requestIdentifier) {
		return mDiskLRUManager.getFile(encode(requestIdentifier));
	}

	private String encode(RequestIdentifier requestIdentifier) {
		try {
			return URLEncoder.encode(requestIdentifier, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Dimensions getImageDimensionsFromDisk(RequestIdentifier requestIdentifier) throws FileNotFoundException {
		try {
			FileInputStream fileInputStream;
			fileInputStream = new FileInputStream(getFile(requestIdentifier));
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

	private final DiskDatabaseHelperObserver mDiskDatabaseHelperObserver = new DiskDatabaseHelperObserver() {
		@Override
		public void onDatabaseWiped() {
			mDiskLRUManager.clearDirectory();
		}
	};
}
