package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.test.AndroidTestCase;
import android.test.UiThreadTest;

import com.xtreme.utilities.testing.DelayedLoop;
import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

public class ImageCacherTests extends AndroidTestCase {
	private static final String TEST_URI = "file:///some/directory/with/an/image.jpg";

	private boolean mCallComplete = false;
	private ImageCacher mImageCacher;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mImageCacher = ImageCacher.getInstance(getContext());
		mImageCacher.stubMemCache(new MemCacheStub());
		mImageCacher.stubDiskCache(new DiskCacheStub());
		mImageCacher.stubNetwork(new NetworkStub());
	}

	@UiThreadTest
	public void testUnqueuedFileSystemRequest() {
		ThreadChecker.disableUiThreadCheck();

		final DelayedLoop delayedLoop = new DelayedLoop(2000);

		mCallComplete = false;
		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageCacherListener imageCacherListener, String url, ScalingInfo scalingInfo) {
				return AsyncOperationState.NOT_QUEUED;
			}

			@Override
			public void registerListenerForDetailsRequest(ImageCacherListener imageCacherListener, String uri, ScalingInfo scalingInfo) {
				mCallComplete = true;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public void retrieveImageDetails(String uri) {
				if (mCallComplete) {
					delayedLoop.flagSuccess();
				}
			}
		});

		assertNull(mImageCacher.getBitmap(TEST_URI, new ImageCacherListener() {
			@Override
			public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onFailure(String message) {
			}
		}, new ScalingInfo()));

		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}

	@UiThreadTest
	public void testQueuedFileSystemRequest() {
		ThreadChecker.disableUiThreadCheck();

		final DelayedLoop delayedLoop = new DelayedLoop(2000);

		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageCacherListener imageCacherListener, String url, ScalingInfo scalingInfo) {
				return AsyncOperationState.QUEUED_FOR_DETAILS_REQUEST;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public void retrieveImageDetails(String uri) {
				delayedLoop.flagFailure();
			}

			@Override
			public void bumpInQueue(String uri, int sampleSize) {
				if (sampleSize == 0) {
					delayedLoop.flagSuccess();
				} else {
					delayedLoop.flagFailure();
				}
			}
		});

		assertNull(mImageCacher.getBitmap(TEST_URI, new ImageCacherListener() {
			@Override
			public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onFailure(String message) {
			}
		}, new ScalingInfo()));

		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}

	public void testPermRequestInMemCache() {
		ThreadChecker.disableUiThreadCheck();

		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageCacherListener imageCacherListener, String url, ScalingInfo scalingInfo) {
				return AsyncOperationState.NOT_QUEUED;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public boolean isCached(String uri) {
				return true;
			}

			@Override
			public int getSampleSize(String uri, Integer width, Integer height) {
				return 1;
			}
		});

		mImageCacher.stubMemCache(new MemCacheStub() {
			@Override
			public Bitmap getBitmap(String url, int sampleSize) {
				return Bitmap.createBitmap(100, 100, Config.RGB_565);
			}
		});

		Bitmap bitmap = mImageCacher.getBitmap(TEST_URI, new ImageCacherListener() {
			@Override
			public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onFailure(String message) {
			}
		}, new ScalingInfo());

		assertNotNull(bitmap);
		assertEquals(100, bitmap.getWidth());
		assertEquals(100, bitmap.getHeight());
	}

	public void testImageDetailsRetrieved() {
		final DelayedLoop delayedLoop = new DelayedLoop(2000);
		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public void onDetailsRequestComplete(String uri) {
				if (uri.equals(TEST_URI)) {
					delayedLoop.flagSuccess();
				} else {
					delayedLoop.flagFailure();
				}
			}
		});

		mImageCacher.onImageDetailsRetrieved(TEST_URI);
		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}

	public void testImageDetailsRequestFailed() {
		final DelayedLoop delayedLoop = new DelayedLoop(2000);
		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public void onDetailsRequestFailed(String uri, String message) {
				if (uri.equals(TEST_URI)) {
					delayedLoop.flagSuccess();
				} else {
					delayedLoop.flagFailure();
				}
			}
		});

		mImageCacher.onImageDetailsRequestFailed(TEST_URI, "Forced failure.");
		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}
}
