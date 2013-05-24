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
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.xtremelabs.imageutils.DiskDatabaseHelper.DiskDatabaseHelperObserver;

class DiskLRUCacher implements ImageDiskCacherInterface {
	private static final int MAX_PERMANENT_STORAGE_IMAGE_DIMENSIONS_CACHED = 25; // TODO Optimize this value, or allow for API access to modify it.

	private long mMaximumCacheSizeInBytes = 50 * 1024 * 1024; // 50MB
	private final DiskManager mDiskManager;
	private final DiskDatabaseHelper mDatabaseHelper;
	private ImageDiskObserver mImageDiskObserver;
	private final Map<String, Dimensions> mPermanentStorageMap = new LRUMap<String, Dimensions>(34, MAX_PERMANENT_STORAGE_IMAGE_DIMENSIONS_CACHED);

	public DiskLRUCacher(Context appContext, ImageDiskObserver imageDecodeObserver) {
		/*
		 * WARNING: Increasing the number of threads for image decoding will lag the UI thread.
		 * 
		 * It is highly recommended to leave the number of decode threads at one. Increasing this number too high will cause performance problems.
		 */
		mDiskManager = new DiskManager("img", appContext);
		mDatabaseHelper = new DiskDatabaseHelper(appContext, mDiskDatabaseHelperObserver);
		mImageDiskObserver = imageDecodeObserver;
	}

	// TODO This method is very slow. It could be due to synchronized blocks. See if performance can be improved.
	@Override
	public boolean isCached(CacheRequest cacheRequest) {
		boolean isCached;
		String uri = cacheRequest.getUri();
		if (cacheRequest.isFileSystemRequest()) {
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
	public int getSampleSize(CacheRequest cacheRequest) {
		Dimensions dimensions = getImageDimensions(cacheRequest);
		if (dimensions == null) {
			return -1;
		}
		int sampleSize = SampleSizeCalculationUtility.calculateSampleSize(cacheRequest, dimensions);
		return sampleSize;
	}

	@Override
	public Prioritizable getDetailsPrioritizable(final CacheRequest cacheRequest) {
		return new DefaultPrioritizable(cacheRequest, new Request<String>(cacheRequest.getUri())) {
			@Override
			public void execute() {
				cacheImageDetails(cacheRequest);
			}
		};
	}

	@Override
	public Prioritizable getDecodePrioritizable(final CacheRequest cacheRequest, final DecodeSignature decodeSignature, final ImageReturnedFrom imageReturnedFrom) {
		return new DefaultPrioritizable(cacheRequest, new Request<DecodeSignature>(decodeSignature)) {
			@Override
			public void execute() {
				boolean failed = false;
				String errorMessage = null;
				Bitmap bitmap = null;
				try {
					bitmap = getBitmapSynchronouslyFromDisk(cacheRequest, decodeSignature);
				} catch (FileNotFoundException e) {
					failed = true;
					errorMessage = "FileNotFoundException -- Disk decode failed with error message: " + e.getMessage();
				} catch (FileFormatException e) {
					failed = true;
					errorMessage = "FileFormatException -- Disk decode failed with error message: " + e.getMessage();
				}

				if (!failed) {
					mImageDiskObserver.onImageDecoded(decodeSignature, bitmap, imageReturnedFrom);
				} else if (cacheRequest.isFileSystemRequest()) {
					mPermanentStorageMap.remove(decodeSignature.uri);
					mImageDiskObserver.onImageDecodeFailed(decodeSignature, errorMessage);
				} else {
					mDiskManager.deleteFile(encode(decodeSignature.uri));
					mDatabaseHelper.deleteEntry(decodeSignature.uri);
					mImageDiskObserver.onImageDecodeFailed(decodeSignature, errorMessage);
				}
			}
		};
	}

	void cacheImageDetails(CacheRequest cacheRequest) {
		String uri = cacheRequest.getUri();
		try {
			calculateAndSaveImageDetails(cacheRequest);

			mImageDiskObserver.onImageDetailsRetrieved(uri);
		} catch (URISyntaxException e) {
			mImageDiskObserver.onImageDetailsRequestFailed(uri, "URISyntaxException caught when attempting to retrieve image details. URI: " + uri);
		} catch (FileNotFoundException e) {
			mImageDiskObserver.onImageDetailsRequestFailed(uri, "Image file not found. URI: " + uri);
		}
	}

	@Override
	public void calculateAndSaveImageDetails(CacheRequest cacheRequest) throws URISyntaxException, FileNotFoundException {
		File file;
		String uri = cacheRequest.getUri();

		if (cacheRequest.isFileSystemRequest()) {
			file = new File(new URI(uri.replace(" ", "%20")).getPath());
		} else {
			file = getFile(uri);
		}

		Dimensions dimensions = getImageDimensionsFromDisk(file);

		if (cacheRequest.isFileSystemRequest()) {
			mPermanentStorageMap.put(uri, dimensions);
		} else {
			mDatabaseHelper.addOrUpdateFile(uri, file.length(), dimensions.width, dimensions.height);
			clearLeastUsedFilesInCache();
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

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		mMaximumCacheSizeInBytes = sizeInBytes;
		clearLeastUsedFilesInCache();
	}

	@Override
	public Dimensions getImageDimensions(CacheRequest cacheRequest) {
		String uri = cacheRequest.getUri();
		Dimensions dimensions;
		if (cacheRequest.isFileSystemRequest()) {
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

	@Override
	public Bitmap getBitmapSynchronouslyFromDisk(CacheRequest cacheRequest, DecodeSignature decodeSignature) throws FileNotFoundException, FileFormatException {
		String uri = decodeSignature.uri;
		int sampleSize = decodeSignature.sampleSize;
		Bitmap.Config bitmapConfig = decodeSignature.bitmapConfig;

		File file = null;
		if (cacheRequest.isFileSystemRequest()) {
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

	private static String encode(String uri) {
		try {
			return URLEncoder.encode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Dimensions getImageDimensionsFromDisk(File file) throws FileNotFoundException {
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
}
