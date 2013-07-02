package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;
import com.xtremelabs.imageutils.ImageSystemDatabase.ImageSystemDatabaseObserver;
import com.xtremelabs.imageutils.NetworkToDiskInterface.ImageDownloadResult.Result;

class DiskCache implements ImageSystemDiskCache {
	private static final int MAX_PERMANENT_STORAGE_IMAGE_DIMENSIONS_CACHED = 25;

	private final Context mContext;
	private final ImageDiskObserver mImageDiskObserver;
	private final Map<String, Dimensions> mPermanentStorageMap = new LRUMap<String, Dimensions>(34, MAX_PERMANENT_STORAGE_IMAGE_DIMENSIONS_CACHED);

	private ImageSystemDatabase mImageSystemDatabase;
	private FileSystemManager mFileSystemManager;
	private long mMaxCacheSize = 50 * 1024 * 1024;

	DiskCache(Context context, ImageDiskObserver imageDiskObserver) {
		mContext = context.getApplicationContext();
		mImageDiskObserver = imageDiskObserver;
		mFileSystemManager = new FileSystemManager("img", mContext);
		mImageSystemDatabase = new ImageSystemDatabase(mImageSystemDatabaseObserver);
		mImageSystemDatabase.init(mContext);
	}

	@Override
	public ImageDownloadResult downloadImageFromInputStream(String uri, InputStream inputStream) {
		ImageDownloadResult result;

		mImageSystemDatabase.beginWrite(uri);
		ImageEntry imageEntry = mImageSystemDatabase.getEntry(uri);
		try {
			mFileSystemManager.loadStreamToFile(inputStream, imageEntry.getFileName());
			mImageSystemDatabase.endWrite(uri);
			result = new ImageDownloadResult(Result.SUCCESS);

		} catch (IOException e) {
			mImageSystemDatabase.writeFailed(uri);
			result = new ImageDownloadResult(Result.FAILURE, "Failed to download image to disk! IOException caught. Error message: " + e.getMessage());
		}

		return result;
	}

	@Override
	public boolean isCached(CacheRequest cacheRequest) {
		boolean isCached;
		String uri = cacheRequest.getUri();
		if (cacheRequest.isFileSystemRequest()) {
			isCached = mPermanentStorageMap.get(uri) != null;
		} else {
			ImageEntry entry = mImageSystemDatabase.getEntry(uri);
			isCached = entry != null && entry.onDisk;
		}

		return isCached;
	}

	@Override
	public int getSampleSize(CacheRequest cacheRequest) {
		Dimensions dimensions = getImageDimensions(cacheRequest);
		if (dimensions == null)
			return -1;

		return SampleSizeCalculationUtility.calculateSampleSize(cacheRequest, dimensions);
	}

	@Override
	public void bumpOnDisk(String uri) {
		mImageSystemDatabase.bump(uri);
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
		// TODO Auto-generated method stub

	}

	@Override
	public Dimensions getImageDimensions(CacheRequest cacheRequest) {
		String uri = cacheRequest.getUri();
		Dimensions dimensions;
		if (cacheRequest.isFileSystemRequest()) {
			dimensions = mPermanentStorageMap.get(uri);
		} else {
			ImageEntry entry = mImageSystemDatabase.getEntry(uri);
			dimensions = entry != null ? new Dimensions(entry.sizeX, entry.sizeY) : null;
		}
		return dimensions;
	}

	@Override
	public void invalidateFileSystemUri(String uri) {
		mPermanentStorageMap.remove(uri);
	}

	@Override
	public Bitmap getBitmapSynchronouslyFromDisk(CacheRequest cacheRequest, DecodeSignature decodeSignature) throws FileNotFoundException, FileFormatException {
		int sampleSize = decodeSignature.sampleSize;
		Bitmap.Config bitmapConfig = decodeSignature.bitmapConfig;

		File file = getFile(cacheRequest);
		FileInputStream fileInputStream = new FileInputStream(file);

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

	@Override
	public void calculateAndSaveImageDetails(CacheRequest cacheRequest) throws URISyntaxException, FileNotFoundException {
		String uri = cacheRequest.getUri();
		File file = getFile(cacheRequest);

		Dimensions dimensions = getImageDimensionsFromDisk(file);

		if (cacheRequest.isFileSystemRequest()) {
			mPermanentStorageMap.put(uri, dimensions);
		} else {
			mImageSystemDatabase.submitDetails(uri, dimensions, file.length());
			clearLRUFiles();
		}
	}

	private void clearLRUFiles() {
		while (mImageSystemDatabase.getTotalFileSize() > mMaxCacheSize) {
			ImageEntry entry = mImageSystemDatabase.removeLRU();
			mFileSystemManager.deleteFile(entry.getFileName());
		}
	}

	private File getFile(CacheRequest cacheRequest) throws FileNotFoundException {
		String uri = cacheRequest.getUri();
		File file;
		if (cacheRequest.isFileSystemRequest()) {
			try {
				file = new File(new URI(uri.replace(" ", "%20")).getPath());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				throw new FileNotFoundException();
			}
		} else {
			ImageEntry entry = mImageSystemDatabase.getEntry(uri);
			if (entry == null)
				throw new FileNotFoundException();
			file = mFileSystemManager.getFile(entry.getFileName());
		}
		return file;
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
				} catch (IOException e) {}
			}
		}
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
					try {
						mFileSystemManager.deleteFile(getFile(cacheRequest));
					} catch (FileNotFoundException e) {}
					mImageSystemDatabase.deleteEntry(decodeSignature.uri);
					mImageDiskObserver.onImageDecodeFailed(decodeSignature, errorMessage);
				}
				// TODO need to re-start entire process if image was supposed to be on disk but wasn't
			}
		};
	}

	private final ImageSystemDatabaseObserver mImageSystemDatabaseObserver = new ImageSystemDatabaseObserver() {

		@Override
		public void onDetailsRequired(String filename) {
			cacheImageDetails(new CacheRequest(filename));
		}

		@Override
		public void onBadJournalEntry(ImageEntry entry) {
			mFileSystemManager.deleteFile(entry.getFileName());
		}
	};

	void clear() {
		mImageSystemDatabase.clear();
		mFileSystemManager.clearDirectory();
	}

}
