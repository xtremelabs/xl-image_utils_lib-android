package com.xtremelabs.imageutils;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.AbstractImageLoader.Options;
import com.xtremelabs.imageutils.AbstractImageLoader.Options.ScalingPreference;
import com.xtremelabs.testactivity.MainActivity;

public class SampleSizeTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private ImageRequest mImageRequest;
	private ScalingInfo mScalingInfo;
	private Options mOptions;

	public SampleSizeTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mScalingInfo = new ScalingInfo();
		mImageRequest = new ImageRequest("some uri", mScalingInfo);
		mOptions = new Options();
		mOptions.scalingPreference = ScalingPreference.LARGER_THAN_VIEW_OR_FULL_SIZE;
		mImageRequest.setOptions(mOptions);
	}

	public void testBasicSampleSizeCalculations() {
		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.width = 512;
		mScalingInfo.height = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 2000;
		mScalingInfo.height = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 250;
		mScalingInfo.height = 250;
		assertEquals(2, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 129;
		mScalingInfo.height = 129;
		assertEquals(3, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 128;
		mScalingInfo.height = 128;
		assertEquals(4, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 64;
		mScalingInfo.height = 64;
		assertEquals(8, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 32;
		mScalingInfo.height = 32;
		assertEquals(16, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 16;
		mScalingInfo.height = 16;
		assertEquals(32, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}

	public void testWidthOnlySampleSizeCalculations() {
		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.width = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 257;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 256;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 250;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}

	public void testHeightOnlySampleSizeCalculations() {
		mOptions.scalingPreference = ScalingPreference.SMALLER_THAN_VIEW;

		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.height = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 257;
		assertEquals(2, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 256;
		assertEquals(2, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 250;
		assertEquals(3, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 129;
		assertEquals(4, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 128;
		assertEquals(4, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 64;
		assertEquals(8, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 32;
		assertEquals(16, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 16;
		assertEquals(32, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}

	public void testHeightHighWidthSampleSizeCalculations() {
		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.height = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 256;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 128;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}

	public void testHeightLowWidthSampleSizeCalculations() {
		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.height = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 256;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.height = 128;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}

	public void testWidthHighHeightSampleSizeCalculations() {
		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.width = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 256;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 128;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}

	public void testWidthLowHeightSampleSizeCalculations() {
		Dimensions imageDimensions;
		imageDimensions = new Dimensions(512, 512);

		mScalingInfo.width = 512;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 2000;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 256;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));

		mScalingInfo.width = 128;
		assertEquals(1, DiskLRUCacher.calculateSampleSize(mImageRequest, imageDimensions));
	}
}
