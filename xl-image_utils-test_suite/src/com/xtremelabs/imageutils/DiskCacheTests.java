package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.widget.ImageView;

import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;
import com.xtremelabs.imageutils.ImageLoader.Options;
import com.xtremelabs.imageutils.ImageLoader.Options.ScalingPreference;
import com.xtremelabs.imageutils.NetworkToDiskInterface.ImageDownloadResult;
import com.xtremelabs.imageutils.NetworkToDiskInterface.ImageDownloadResult.Result;

public class DiskCacheTests extends AndroidTestCase {
	private static final String IMAGE_URL = "http://placekitten.com/400/600";

	private static final CacheRequest CACHE_REQUEST = new CacheRequest(IMAGE_URL);

	private DiskCache mDiskCache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		if (mDiskCache == null)
			mDiskCache = new DiskCache(getContext(), mImageDiskObserver);

		mDiskCache.clear();
	}

	public void testDownloadImageFromInputStream() {
		downloadImage();
	}

	public void testIsCached() {
		assertFalse(mDiskCache.isCached(CACHE_REQUEST));

		downloadImage();

		assertTrue(mDiskCache.isCached(CACHE_REQUEST));
	}

	public void testGetSampleSize() {
		assertEquals(-1, mDiskCache.getSampleSize(CACHE_REQUEST));

		downloadImage();
		populateImageDetails();

		MoreAsserts.assertNotEqual(-1, mDiskCache.getSampleSize(CACHE_REQUEST));
	}

	public void testSetDiskCacheSize() {
		assertFalse(mDiskCache.isCached(CACHE_REQUEST));

		downloadImage();
		populateImageDetails();

		assertTrue(mDiskCache.isCached(CACHE_REQUEST));

		mDiskCache.setDiskCacheSize(1);

		assertFalse(mDiskCache.isCached(CACHE_REQUEST));

		downloadImage();
		populateImageDetails();

		assertFalse(mDiskCache.isCached(CACHE_REQUEST));

		mDiskCache.setDiskCacheSize(1 * 1024 * 1024);

		downloadImage();
		populateImageDetails();

		assertTrue(mDiskCache.isCached(CACHE_REQUEST));
	}

	public void testGetImageDimensions() {
		assertNull(mDiskCache.getImageDimensions(CACHE_REQUEST));

		downloadImage();
		populateImageDetails();

		assertNotNull(mDiskCache.getImageDimensions(CACHE_REQUEST));
	}

	public void testInvalidateUri() {
		fail();
		// TODO how to test this?
	}

	public void testGetBitmapSynchronouslyFromDisk() throws FileNotFoundException, FileFormatException {
		downloadImage();
		populateImageDetails();

		DecodeSignature decodeSignature = new DecodeSignature(IMAGE_URL, 1, null);
		Bitmap bitmap = mDiskCache.getBitmapSynchronouslyFromDisk(CACHE_REQUEST, decodeSignature);
		assertNotNull(bitmap);
	}

	public void testCalculateAndSaveImageDetails() {
		assertFalse(mDiskCache.isCached(CACHE_REQUEST));

		downloadImage();

		assertEquals(1, mDiskCache.getSampleSize(CACHE_REQUEST));

		populateImageDetails();

		Options options = new Options();
		options.heightBounds = 300;
		options.widthBounds = 200;
		options.scalingPreference = ScalingPreference.MATCH_TO_SMALLER_DIMENSION;
		CacheRequest cacheRequest = new CacheRequest(IMAGE_URL, getScalingInfo(null, options));
		assertEquals(2, mDiskCache.getSampleSize(cacheRequest));
	}

	ScalingInfo getScalingInfo(ImageView imageView, final Options options) {
		ScalingInfo scalingInfo = new ScalingInfo();
		if (options.overrideSampleSize != null) {
			scalingInfo.sampleSize = options.overrideSampleSize;
			return scalingInfo;
		}

		Integer width = options.widthBounds;
		Integer height = options.heightBounds;

		if (options.autoDetectBounds && imageView != null) {
			Point viewBounds = ViewDimensionsUtil.getImageViewDimensions(imageView);

			width = getBounds(width, viewBounds.x);
			height = getBounds(height, viewBounds.y);
		}

		scalingInfo.width = width;
		scalingInfo.height = height;
		return scalingInfo;
	}

	private static Integer getBounds(Integer currentDimension, int viewDimension) {
		if (viewDimension != -1) {
			if (currentDimension == null) {
				currentDimension = viewDimension;
			} else {
				currentDimension = Math.min(currentDimension, viewDimension);
			}
		}
		return currentDimension;
	}

	private InputStream getInputStream() {
		try {
			return new URL(IMAGE_URL).openStream();
		} catch (Exception e) {}
		return null;
	}

	private void downloadImage() {
		ImageDownloadResult result = mDiskCache.downloadImageFromInputStream(IMAGE_URL, getInputStream());
		assertEquals(result.getResult(), Result.SUCCESS);
	}

	private void populateImageDetails() {
		mDiskCache.cacheImageDetails(CACHE_REQUEST);
	}

	private final ImageDiskObserver mImageDiskObserver = new ImageDiskObserver() {
		@Override
		public void onImageDetailsRetrieved(String uri) {}

		@Override
		public void onImageDetailsRequestFailed(String uri, String errorMessage) {}

		@Override
		public void onImageDecoded(DecodeSignature decodeSignature, Bitmap bitmap, ImageReturnedFrom returnedFrom) {}

		@Override
		public void onImageDecodeFailed(DecodeSignature decodeSignature, String error) {}
	};
}
