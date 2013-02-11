/*
 * Copyright 2013 Xtreme Labs
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.xtremelabs.imageutils.AuxiliaryExecutor.Builder;
import com.xtremelabs.imageutils.DiskDatabaseHelper.DiskDatabaseHelperObserver;

public class DiskLRUCacher implements ImageDiskCacherInterface {
	private static final int MAX_PERMANENT_STORAGE_IMAGE_DIMENSIONS_CACHED = 25; // TODO Optimize this value, or allow for API access to modify it.

	private long mMaximumCacheSizeInBytes = 50 * 1024 * 1024; // 50MB
	private final DiskManager mDiskManager;
	private final DiskDatabaseHelper mDatabaseHelper;
	private ImageDiskObserver mImageDiskObserver;
	private final Map<String, Dimensions> mPermanentStorageMap = new LRUMap<String, Dimensions>(34, MAX_PERMANENT_STORAGE_IMAGE_DIMENSIONS_CACHED);
	private final Map<DecodeSignature, DiskRunnable> mRequestToRunnableMap = new HashMap<DecodeSignature, DiskRunnable>();

	private final AuxiliaryExecutor mExecutor;

	public DiskLRUCacher(Context appContext, ImageDiskObserver imageDecodeObserver) {
		/*
		 * WARNING: Increasing the number of threads for image decoding will lag the UI thread.
		 * 
		 * It is highly recommended to leave the number of decode threads at one. Increasing this number too high will cause performance problems.
		 */
		Builder builder = new Builder(new PriorityAccessor[] { new StackPriorityAccessor() });
		builder.setCorePoolSize(1);
		mExecutor = builder.create();
		mDiskManager = new DiskManager("img", appContext);
		mDatabaseHelper = new DiskDatabaseHelper(appContext, mDiskDatabaseHelperObserver);
		mImageDiskObserver = imageDecodeObserver;
	}

	@Override
	public boolean isCached(String uri) {
		boolean isPermanentStorageUri = GeneralUtils.isFileSystemUri(uri);
		boolean isCached = false;

		if (isPermanentStorageUri) {
			isCached = mPermanentStorageMap.containsKey(uri);
			if (isCached) {
				mPermanentStorageMap.get(uri);
			}
		} else {
			isCached = mDatabaseHelper.isCached(uri);
		}

		return isCached;
	}

	@Override
	public int getSampleSize(ImageRequest imageRequest) {
		Dimensions dimensions = getImageDimensions(imageRequest.getUri());
		if (dimensions == null) {
			return -1;
		}
		int sampleSize = SampleSizeCalculationUtility.calculateSampleSize(imageRequest, dimensions);
		return sampleSize;
	}

	@Override
	public void retrieveImageDetails(final String uri) {
		if (mPermanentStorageMap.get(uri) == null) {
			mExecutor.execute(new DiskRunnable() {
				@Override
				public void execute() {
					cacheImageDetails(uri);
				}

				@Override
				public Request<?> getRequest() {
					return new Request<String>(uri);
				}
			});
		}
	}

	void cacheImageDetails(String uri) {
		try {
			calculateAndSaveImageDetails(uri);

			mImageDiskObserver.onImageDetailsRetrieved(uri);
		} catch (URISyntaxException e) {
			mImageDiskObserver.onImageDetailsRequestFailed(uri, "URISyntaxException caught when attempting to retrieve image details. URI: " + uri);
		} catch (FileNotFoundException e) {
			mImageDiskObserver.onImageDetailsRequestFailed(uri, "Image file not found. URI: " + uri);
		}
	}

	@Override
	public void calculateAndSaveImageDetails(String uri) throws URISyntaxException, FileNotFoundException {
		File file;

		boolean isFileSystemUri = GeneralUtils.isFileSystemUri(uri);
		if (isFileSystemUri) {
			file = new File(new URI(uri.replace(" ", "%20")).getPath());
		} else {
			file = getFile(uri);
		}

		Dimensions dimensions = getImageDimensionsFromDisk(file);

		if (isFileSystemUri) {
			mPermanentStorageMap.put(uri, dimensions);
		} else {
			mDatabaseHelper.addOrUpdateFile(uri, file.length(), dimensions.width, dimensions.height);
			clearLeastUsedFilesInCache();
		}
	}

	@Override
	public void getBitmapAsynchronouslyFromDisk(final DecodeSignature decodeSignature, final ImageReturnedFrom returnedFrom, final boolean noPreviousNetworkRequest) {
		DiskRunnable runnable = new DiskRunnable() {
			@Override
			public void execute() {
				boolean failed = false;
				String errorMessage = null;
				Bitmap bitmap = null;
				try {
					bitmap = getBitmapSynchronouslyFromDisk(decodeSignature);
				} catch (FileNotFoundException e) {
					failed = true;
					errorMessage = "Disk decode failed with error message: " + e.getMessage();
				} catch (FileFormatException e) {
					failed = true;
					errorMessage = "Disk decode failed with error message: " + e.getMessage();
				}
				removeRequestFromMap(decodeSignature);

				if (!failed) {
					mImageDiskObserver.onImageDecoded(decodeSignature, bitmap, returnedFrom);
				} else {
					mDiskManager.deleteFile(encode(decodeSignature.mUri));
					mDatabaseHelper.deleteEntry(decodeSignature.mUri);
					mImageDiskObserver.onImageDecodeFailed(decodeSignature, errorMessage);
				}
			}

			@Override
			public Request<?> getRequest() {
				return new Request<DecodeSignature>(decodeSignature);
			}
		};

		if (mapRunnableToParameters(runnable, decodeSignature)) {
			mExecutor.execute(runnable);
		}
	}

	@Override
	public void downloadImageFromInputStream(String uri, InputStream inputStream) throws IOException {
		mDiskManager.loadStreamToFile(inputStream, encode(uri));
	}

	@Override
	public void bumpOnDisk(String uri) {
		mDatabaseHelper.updateFile(uri);
	}

	// TODO This method should NOT be taking the sampleSize in directly, but rather the scaling info. The sampleSize should be calculated by the disk system.
	@Override
	public void bumpInQueue(DecodeSignature decodeSignature) {
		synchronized (mRequestToRunnableMap) {
			mExecutor.bump(mRequestToRunnableMap.get(decodeSignature));
		}
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		mMaximumCacheSizeInBytes = sizeInBytes;
		clearLeastUsedFilesInCache();
	}

	@Override
	public Dimensions getImageDimensions(String uri) {
		boolean isFromPermanentStorage = GeneralUtils.isFileSystemUri(uri);

		Dimensions dimensions;
		if (isFromPermanentStorage) {
			dimensions = mPermanentStorageMap.get(uri);
		} else {
			FileEntry fileEntry = mDatabaseHelper.getFileEntryFromCache(uri);
			if (fileEntry != null) {
				dimensions = fileEntry.getDimensions();
			} else {
				dimensions = null;
			}
		}

		return dimensions;
	}

	@Override
	public void invalidateFileSystemUri(String uri) {
		mPermanentStorageMap.remove(uri);
	}

	private boolean mapRunnableToParameters(DiskRunnable runnable, DecodeSignature parameters) {
		synchronized (mRequestToRunnableMap) {
			if (!mRequestToRunnableMap.containsKey(parameters)) {
				mRequestToRunnableMap.put(parameters, runnable);
				return true;
			} else {
				return false;
			}
		}
	}

	private void removeRequestFromMap(DecodeSignature parameters) {
		synchronized (mRequestToRunnableMap) {
			mRequestToRunnableMap.remove(parameters);
		}
	}

	@Override
	public Bitmap getBitmapSynchronouslyFromDisk(DecodeSignature decodeSignature) throws FileNotFoundException, FileFormatException {
		String uri = decodeSignature.mUri;
		int sampleSize = decodeSignature.mSampleSize;
		Bitmap.Config bitmapConfig = decodeSignature.mBitmapConfig;

		File file = null;
		if (GeneralUtils.isFileSystemUri(uri)) {
			try {
				file = new File(new URI(uri).getPath());
			} catch (URISyntaxException e) {
				throw new FileNotFoundException("Bad URI.");
			}
		} else {
			file = getFile(uri);
		}
		FileInputStream fileInputStream = null;
		fileInputStream = new FileInputStream(file);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = sampleSize;
		opts.inPreferredConfig = bitmapConfig;
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

	private void clearLeastUsedFilesInCache() {
		mDatabaseHelper.removeLeastUsedFileFromCache(mMaximumCacheSizeInBytes);
	}

	private File getFile(String uri) {
		return mDiskManager.getFile(encode(uri));
	}

	private String encode(String uri) {
		try {
			return URLEncoder.encode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Dimensions getImageDimensionsFromDisk(File file) throws FileNotFoundException {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(fileInputStream, null, o);
			return new Dimensions(o.outWidth, o.outHeight);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
		}
	}

	public static class FileFormatException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2180782787028503586L;
	}

	@Override
	public synchronized boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
		return mRequestToRunnableMap.containsKey(decodeSignature);
	}

	private final DiskDatabaseHelperObserver mDiskDatabaseHelperObserver = new DiskDatabaseHelperObserver() {
		@Override
		public void onDatabaseWiped() {
			mDiskManager.clearDirectory();
		}

		@Override
		public void onImageEvicted(String uri) {
			mDiskManager.deleteFile(encode(uri));
		}
	};

	void stubImageDiskObserver(ImageDiskObserver imageDecodeObserver) {
		mImageDiskObserver = imageDecodeObserver;
	}

	private abstract class DiskRunnable extends Prioritizable {
		@Override
		public int getTargetPriorityAccessorIndex() {
			return 0;
		}
	}
}
