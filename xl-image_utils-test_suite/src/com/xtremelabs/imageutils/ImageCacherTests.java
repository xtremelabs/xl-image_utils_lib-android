package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.test.AndroidTestCase;
import android.test.UiThreadTest;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.RequestType;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.testutils.DelayedLoop;

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
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
				return AsyncOperationState.NOT_QUEUED;
			}

			@Override
			public void registerListenerForDetailsRequest(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
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

		ImageRequest imageRequest = new ImageRequest(TEST_URI, new ScalingInfo());
		assertEquals(ImageResponseStatus.REQUEST_QUEUED, mImageCacher.getBitmap(imageRequest, new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		}).getImageResponseStatus());

		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}

	@UiThreadTest
	public void testQueuedFileSystemRequest() {
		ThreadChecker.disableUiThreadCheck();

		final DelayedLoop delayedLoop = new DelayedLoop(2000);

		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
				return AsyncOperationState.QUEUED_FOR_DETAILS_REQUEST;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public void retrieveImageDetails(String uri) {
				delayedLoop.flagFailure();
			}

			@Override
			public void bumpInQueue(DecodeSignature decodeSignature) {
				if (decodeSignature.mSampleSize == 0) {
					delayedLoop.flagSuccess();
				} else {
					delayedLoop.flagFailure();
				}
			}
		});

		ImageRequest imageRequest = new ImageRequest(TEST_URI, new ScalingInfo());
		assertEquals(ImageResponseStatus.REQUEST_QUEUED, mImageCacher.getBitmap(imageRequest, new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		}).getImageResponseStatus());

		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}

	public void testPermRequestInMemCache() {
		ThreadChecker.disableUiThreadCheck();

		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
				return AsyncOperationState.NOT_QUEUED;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public boolean isCached(String uri) {
				return true;
			}

			@Override
			public int getSampleSize(ImageRequest imageRequest) {
				return 1;
			}
		});

		mImageCacher.stubMemCache(new MemCacheStub() {
			@Override
			public Bitmap getBitmap(DecodeSignature decodeSignature) {
				return Bitmap.createBitmap(100, 100, Config.RGB_565);
			}
		});

		ImageRequest imageRequest = new ImageRequest(TEST_URI, new ScalingInfo());
		ImageResponse imageResponse = mImageCacher.getBitmap(imageRequest, new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		});

		assertNotNull(imageResponse);
		assertEquals(100, imageResponse.getBitmap().getWidth());
		assertEquals(100, imageResponse.getBitmap().getHeight());
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

	public void testSuccessfulMemcacheRetrieval() {
		final DelayedLoop delayedLoop = new DelayedLoop(1000);

		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
				return AsyncOperationState.NOT_QUEUED;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public int getSampleSize(ImageRequest imageRequest) {
				return 1;
			}

			@Override
			public boolean isCached(String uri) {
				return true;
			}
		});

		mImageCacher.stubMemCache(new MemCacheStub() {
			@Override
			public Bitmap getBitmap(DecodeSignature decodeSignature) {
				if (decodeSignature == null || decodeSignature.mSampleSize != 1 || decodeSignature.mUri != TEST_URI) {
					delayedLoop.flagFailure();
					return null;
				}

				delayedLoop.flagSuccess();
				return Bitmap.createBitmap(100, 100, Config.RGB_565);
			}
		});

		ImageResponse imageResponse = mImageCacher.getBitmap(new ImageRequest(TEST_URI, new ScalingInfo()), new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
				delayedLoop.flagFailure();
			}

			@Override
			public void onFailure(String message) {
				delayedLoop.flagFailure();
			}
		});

		delayedLoop.startLoop();
		delayedLoop.assertPassed();

		assertNotNull(imageResponse);
		assertNotNull(imageResponse.getBitmap());
		assertEquals(100, imageResponse.getBitmap().getWidth());
		assertEquals(ImageReturnedFrom.MEMORY, imageResponse.getImageReturnedFrom());
		assertEquals(ImageResponseStatus.SUCCESS, imageResponse.getImageResponseStatus());
	}

	public void testPrecacheImageToDisk() {
		final DelayedLoop delayedLoop = new DelayedLoop(2000);
		mCallComplete = false;

		mImageCacher.stubAsynchOperationsMaps(new AsyncOperationsMaps(mImageCacher) {
			@Override
			public synchronized void registerListenerForNetworkRequest(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
				if (imageRequest != null && imageRequest.getRequestType() == RequestType.CACHE_TO_DISK) {
					delayedLoop.flagSuccess();
				} else {
					delayedLoop.flagFailure();
				}
			}
		});

		mImageCacher.stubNetwork(new NetworkInterfaceStub() {
			@Override
			public void downloadImageToDisk(String url) {
				mCallComplete = true;
			}
		});

		mImageCacher.stubDiskCache(new DiskCacheStub() {
			@Override
			public boolean isCached(String uri) {
				return false;
			}
		});

		ImageRequest imageRequest = new ImageRequest("random URI");
		imageRequest.setRequestType(RequestType.CACHE_TO_DISK);
		mImageCacher.precacheImageToDisk(imageRequest);

		delayedLoop.startLoop();
		delayedLoop.assertPassed();
		assertTrue(mCallComplete);
	}
}
