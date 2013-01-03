package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.testactivity.MainActivity;

public class SampleSizeTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private DiskLRUCacher mDiskCacher;

	public SampleSizeTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDiskCacher = new DiskLRUCacher(getActivity().getApplicationContext(), new ImageDiskObserver() {
			@Override
			public void onImageDecoded(Bitmap bitmap, String url, int sampleSize, ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onImageDecodeFailed(String url, int sampleSize, String error) {
			}

			@Override
			public void onImageDetailsRequestFailed(String uri, String errorMessage) {
			}

			@Override
			public void onImageDetailsRetrieved(String uri) {
			}
		});
	}

	public void testBasicSampleSizeCalculations() {
		int width;
		int height;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		width = 512;
		height = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 2000;
		height = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 257;
		height = 257;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 256;
		height = 256;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 250;
		height = 250;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 129;
		height = 129;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 128;
		height = 128;
		assertEquals(4, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 64;
		height = 64;
		assertEquals(8, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 32;
		height = 32;
		assertEquals(16, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 16;
		height = 16;
		assertEquals(32, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}

	public void testWidthOnlySampleSizeCalculations() {
		int width;
		Integer height = null;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		width = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 257;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 256;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 250;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 129;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 128;
		assertEquals(4, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 64;
		assertEquals(8, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 32;
		assertEquals(16, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 16;
		assertEquals(32, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}

	public void testHeightOnlySampleSizeCalculations() {
		Integer width = null;
		int height;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		height = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 257;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 256;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 250;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 129;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 128;
		assertEquals(4, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 64;
		assertEquals(8, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 32;
		assertEquals(16, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 16;
		assertEquals(32, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}

	public void testHeightHighWidthSampleSizeCalculations() {
		Integer width = 2000;
		int height;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		height = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 256;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 128;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}

	public void testHeightLowWidthSampleSizeCalculations() {
		Integer width = 128;
		int height;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		height = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 256;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		height = 128;
		assertEquals(4, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}

	public void testWidthHighHeightSampleSizeCalculations() {
		int width;
		Integer height = 2000;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		width = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 256;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 128;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}

	public void testWidthLowHeightSampleSizeCalculations() {
		int width;
		Integer height = 128;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		width = 512;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 2000;
		assertEquals(1, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 256;
		assertEquals(2, mDiskCacher.calculateSampleSize(width, height, imageDimensions));

		width = 128;
		assertEquals(4, mDiskCacher.calculateSampleSize(width, height, imageDimensions));
	}
}
