package com.xtremelabs.imageutils.frontendtests;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.ImageView;

import com.example.xl_image_utils_android_testactivity.test.R;
import com.xtremelabs.imageutils.DiskManagerAccessUtil;
import com.xtremelabs.imageutils.ImageLoader;
import com.xtremelabs.imageutils.ImageLoaderListener;
import com.xtremelabs.imageutils.ImageReturnedFrom;
import com.xtremelabs.imageutils.testutils.GeneralTestUtils;
import com.xtremelabs.imageutils.testutils.GeneralTestUtils.DelayedLoopListener;
import com.xtremelabs.testactivity.MainActivity;

public class ImageLoaderTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private String mTestUrl = "http://placekitten.com/500/300";
	private ImageLoader mImageLoader;
	private ImageView mImageView;
	private Bitmap mBitmap;
	private ImageReturnedFrom mImageReturnedFrom;
	private boolean mComplete, mFailed;
	private String mErrorMessage;
	private DiskManagerAccessUtil mDiskManagerAccessUtil;

	public ImageLoaderTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@UiThreadTest
	public void testLoadImageWithCallback() {
		//Test currently fails because the test program does not have access to the main looper, so the callbacks never get called
		
		/*
		mImageLoader = new ImageLoader(getActivity());
		mDiskManagerAccessUtil = new DiskManagerAccessUtil(getActivity().getApplicationContext());
		
		mDiskManagerAccessUtil.clearDiskCache();

		ImageView imageView = new ImageView(getActivity());

		resetFields();

		mFailed = false;
		mComplete = false;

		mImageLoader.loadImage(imageView, mTestUrl, null, new ImageLoaderListener() {
			@Override
			public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				mImageView = imageView;
				mBitmap = bitmap;
				mImageReturnedFrom = returnedFrom;
				mComplete = true;
			}

			@Override
			public void onImageLoadError(String error) {
				mFailed = true;
			}
		});

		GeneralTestUtils.delayedLoop(2000, new DelayedLoopListener() {
			@Override
			public boolean shouldBreak() {
				return mFailed || mComplete;
			}
		});

		assertFalse(mFailed);
		assertTrue(mComplete);

		assertNotNull(mImageView);
		assertNotNull(mBitmap);
		assertNotNull(mImageReturnedFrom);
		assertEquals(ImageReturnedFrom.NETWORK, mImageReturnedFrom);
		assertEquals(imageView, mImageView);
		assertNotSame(mBitmap.getWidth(), 0);
		assertEquals(getTestImageBitmap().getWidth(), mBitmap.getWidth());
		assertEquals(getTestImageBitmap().getHeight(), mBitmap.getHeight());
		
		mImageLoader.destroy();
		*/
	}
	
	@UiThreadTest
	public void testNullUrlFailure() {
		mImageLoader = new ImageLoader(getActivity());
		mDiskManagerAccessUtil = new DiskManagerAccessUtil(getActivity().getApplicationContext());
		
		mDiskManagerAccessUtil.clearDiskCache();

		ImageView imageView = new ImageView(getActivity());

		resetFields();

		mFailed = false;
		mComplete = false;

		mImageLoader.loadImage(imageView, null, null, new ImageLoaderListener() {
			@Override
			public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				mComplete = true;
			}

			@Override
			public void onImageLoadError(String error) {
				mFailed = true;
				mErrorMessage = error;
			}
		});

		GeneralTestUtils.delayedLoop(2000, new DelayedLoopListener() {
			@Override
			public boolean shouldBreak() {
				return mFailed || mComplete;
			}
		});

		assertTrue(mFailed);
		assertFalse(mComplete);
		assertTrue(mErrorMessage.contains("Blank url"));
		
		mImageLoader.destroy();
	}

	private void resetFields() {
		mBitmap = null;
		mComplete = false;
		mFailed = false;
		mImageReturnedFrom = null;
		mImageView = null;
	}

	private Bitmap getTestImageBitmap() {
		BitmapDrawable kittehDrawable = (BitmapDrawable) getActivity().getResources().getDrawable(R.drawable.kitteh_500_by_300);
		return kittehDrawable.getBitmap();
	}
}
