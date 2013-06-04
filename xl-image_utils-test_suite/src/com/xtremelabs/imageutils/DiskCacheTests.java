package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

public class DiskCacheTests extends AndroidTestCase {
	private ImageSystemDiskCache mDiskCache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDiskCache = new DiskCache(getContext(), mImageDiskObserver);
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
