package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

public class DiskCacheTests extends AndroidTestCase {
	private ImageSystemDiskCache mDiskCache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDiskCache = new DiskCache(getContext(), mImageDiskObserver) {
		};
	}
	
	public void testDownloadImageFromInputStream() {
		fail();
	}

	public void testIsCached() {
		fail();
	}

	public void testGetSampleSize() {
		fail();
	}

	public void testBumpOnDisk() {
		fail();
	}

	public void testSetDiskCacheSize() {
		fail();
	}

	public void testGetImageDimensions() {
		fail();
	}

	public void testInvalidateUri() {
		fail();
	}

	public void testGetBitmapSynchronouslyFromDisk() {
		fail();
	}

	public void testCalculateAndSaveImageDetails() {
		fail();
	}

	public void testGetDetailsPrioritizable() {
		fail();
	}

	public void testGetDecodePrioritizable() {
		fail();
	}

	private final ImageDiskObserver mImageDiskObserver = new ImageDiskObserver() {
		@Override
		public void onImageDetailsRetrieved(String uri) {
		}

		@Override
		public void onImageDetailsRequestFailed(String uri, String errorMessage) {
		}

		@Override
		public void onImageDecoded(DecodeSignature decodeSignature, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
		}

		@Override
		public void onImageDecodeFailed(DecodeSignature decodeSignature, String error) {
		}
	};
}
