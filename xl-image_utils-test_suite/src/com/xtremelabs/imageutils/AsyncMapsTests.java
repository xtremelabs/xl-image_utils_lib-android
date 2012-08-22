package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;
import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.testactivity.MainActivity;

public class AsyncMapsTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private AsyncOperationsMaps mMaps;
	private AsyncOperationsObserver mObserver;
	private boolean mAsyncPassed;
	private boolean mAsyncFailed;
	
	public AsyncMapsTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mObserver = new AsyncOperationsObserver() {
			@Override
			public void onImageDecodeRequired(String url, int mSampleSize) {
			}

			@Override
			public boolean isNetworkRequestPendingForUrl(String url) {
				return true;
			}

			@Override
			public boolean isDecodeRequestPending(DecodeOperationParameters decodeOperationParameters) {
				return true;
			}

			@Override
			public int getSampleSize(String url, ScalingInfo scalingInfo) {
				return scalingInfo.sampleSize;
			}
		};
		mMaps = new AsyncOperationsMaps(mObserver);

		mAsyncPassed = false;
		mAsyncFailed = false;

		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertTrue(mMaps.areMapsEmpty());
	}

	public void testSuccessfulNetworkFlow() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.queueListenerIfRequestPending(imageCacherListener, url, scalingInfo) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(imageCacherListener, url, scalingInfo);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));

		mMaps.onDownloadComplete(url);
		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));

		assertFalse(mAsyncFailed);
	}

	public void testNetworkFlowWithFailure() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.queueListenerIfRequestPending(imageCacherListener, url, scalingInfo) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(imageCacherListener, url, scalingInfo);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));

		mMaps.onDownloadFailed(url, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);

		assertTrue(mMaps.areMapsEmpty());
	}

	public void testNetworkQueueSuccess() {
		String url = "blah";

		ImageCacherListener imageCacherListener1 = getBlankImageCacherListener();
		ImageCacherListener imageCacherListener2 = getBlankImageCacherListener();

		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;

		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 2;

		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.queueListenerIfRequestPending(imageCacherListener1, url, scalingInfo1) == AsyncOperationState.NOT_QUEUED);

		mMaps.registerListenerForNetworkRequest(imageCacherListener1, url, scalingInfo1);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo1));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.queueListenerIfRequestPending(imageCacherListener2, url, scalingInfo2);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.onDownloadComplete(url);
		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo1));
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 2);

		assertFalse(mAsyncFailed);
	}

	public void testNetworkQueueFailure() {
		String url = "blah";

		ImageCacherListener imageCacherListener1 = getFailingImageCacherListener();
		ImageCacherListener imageCacherListener2 = getFailingImageCacherListener();

		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;

		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 2;

		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.queueListenerIfRequestPending(imageCacherListener1, url, scalingInfo1) == AsyncOperationState.NOT_QUEUED);

		mMaps.registerListenerForNetworkRequest(imageCacherListener1, url, scalingInfo1);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo1));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.queueListenerIfRequestPending(imageCacherListener2, url, scalingInfo2);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.onDownloadFailed(url, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo1));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);

		assertTrue(mMaps.areMapsEmpty());
	}

	public void testNetworkCancel() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateNetworkRequest(url, imageCacherListener, scalingInfo);
		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		mMaps.onDownloadComplete(url);
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		generateAndValidateNetworkRequest(url, imageCacherListener, scalingInfo);
		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		mMaps.onDownloadFailed(url, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertTrue(mMaps.areMapsEmpty());
	}

	public void testDecodeSuccess() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateDecodeRequest(url, imageCacherListener, scalingInfo);

		mMaps.onDecodeSuccess(null, url, scalingInfo.sampleSize, ImageReturnedFrom.DISK);
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);
	}

	public void testDecodeFailure() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateDecodeRequest(url, imageCacherListener, scalingInfo);

		mMaps.onDecodeFailed(url, scalingInfo.sampleSize, "Forced decode failure");
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);
	}
	
	public void testDecodeQueue() {
		String url1 = "blah1";
		String url2 = "blah2";
		
		ImageCacherListener imageCacherListener1 = getBlankImageCacherListener();
		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;
		
		ImageCacherListener imageCacherListener2 = getBlankImageCacherListener();
		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 2;
		
		ImageCacherListener imageCacherListener3 = getBlankImageCacherListener();
		ScalingInfo scalingInfo3 = new ScalingInfo();
		scalingInfo3.sampleSize = 4;
		
		ImageCacherListener imageCacherListener4 = getBlankImageCacherListener();
		ScalingInfo scalingInfo4 = new ScalingInfo();
		scalingInfo4.sampleSize = 1;
		
		ImageCacherListener imageCacherListener5 = getBlankImageCacherListener();
		ScalingInfo scalingInfo5 = new ScalingInfo();
		scalingInfo5.sampleSize = 1;
		
		generateAndValidateDecodeRequest(url1, imageCacherListener1, scalingInfo1);
		assertEquals(mMaps.getNumPendingDecodes(), 1);
		assertEquals(mMaps.getNumListenersForDecode(), 1);
		
		generateAndValidateDecodeRequest(url1, imageCacherListener2, scalingInfo2);
		assertEquals(mMaps.getNumPendingDecodes(), 2);
		assertEquals(mMaps.getNumListenersForDecode(), 2);
		
		generateAndValidateDecodeRequest(url2, imageCacherListener3, scalingInfo3);
		assertEquals(mMaps.getNumPendingDecodes(), 3);
		assertEquals(mMaps.getNumListenersForDecode(), 3);
		
		assertEquals(mMaps.queueListenerIfRequestPending(imageCacherListener4, url1, scalingInfo4), AsyncOperationState.QUEUED_FOR_DECODE_REQUEST);
		assertEquals(mMaps.getNumPendingDecodes(), 3);
		assertEquals(mMaps.getNumListenersForDecode(), 4);
		
		mMaps.onDecodeSuccess(null, url1, 1, ImageReturnedFrom.DISK);
		assertEquals(mMaps.getNumPendingDecodes(), 2);
		assertEquals(mMaps.getNumListenersForDecode(), 2);
		
		generateAndValidateDecodeRequest(url1, imageCacherListener5, scalingInfo5);
		assertEquals(mMaps.getNumPendingDecodes(), 3);
		assertEquals(mMaps.getNumListenersForDecode(), 3);
	}

	public void testDecodeCancel() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateDecodeRequest(url, imageCacherListener, scalingInfo);

		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));
		mMaps.onDecodeSuccess(null, url, scalingInfo.sampleSize, ImageReturnedFrom.DISK);
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));
		
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertEquals(mMaps.getNumListenersForNetwork(), 0);
		assertEquals(mMaps.getNumListenersForDecode(), 0);

		assertTrue(mMaps.areMapsEmpty());
	}
	
	public void testMovingFromNetworkToDisk() {
		String url = "blah";
		ImageCacherListener imageCacherListener1 = getBlankImageCacherListener();
		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;
		ImageCacherListener imageCacherListener2 = getBlankImageCacherListener();
		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 1;
		ImageCacherListener imageCacherListener3 = getBlankImageCacherListener();
		ScalingInfo scalingInfo3 = new ScalingInfo();
		scalingInfo3.sampleSize = 2;
		
		generateAndValidateNetworkRequest(url, imageCacherListener1, scalingInfo1);
		assertEquals(mMaps.queueListenerIfRequestPending(imageCacherListener2, url, scalingInfo2), AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST);
		assertEquals(mMaps.queueListenerIfRequestPending(imageCacherListener3, url, scalingInfo3), AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST);
		
		mMaps.onDownloadComplete(url);
		
		assertEquals(mMaps.getNumListenersForNetwork(), 0);
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumListenersForDecode(), 3);
		assertEquals(mMaps.getNumPendingDecodes(), 2);
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo1));
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo2));
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo3));
		
		mMaps.onDecodeSuccess(null, url, 1, ImageReturnedFrom.DISK);
		mMaps.onDecodeSuccess(null, url, 2, ImageReturnedFrom.DISK);
		assertTrue(mMaps.areMapsEmpty());
	}

	private ImageCacherListener getBlankImageCacherListener() {
		return new ImageCacherListener() {
			@Override
			public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onFailure(String message) {
				Log.d("BlankImageCacherListener", "on failure: " + message);
			}
		};
	}

	private ImageCacherListener getPassingImageCacherListener() {
		return new ImageCacherListener() {
			@Override
			public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				mAsyncPassed = true;
			}

			@Override
			public void onFailure(String message) {
				Log.d("PassingImageCacherListener", "on failure: " + message);
				mAsyncFailed = true;
			}
		};
	}

	private ImageCacherListener getFailingImageCacherListener() {
		return new ImageCacherListener() {
			@Override
			public void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				mAsyncFailed = true;
			}

			@Override
			public void onFailure(String message) {
				Log.d("FailingImageCacherListener", "on failure: " + message);
				mAsyncPassed = true;
			}
		};
	}

	private void generateAndValidateNetworkRequest(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		assertFalse(mMaps.isNetworkRequestPendingForUrl(url));
		assertTrue(mMaps.queueListenerIfRequestPending(imageCacherListener, url, scalingInfo) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(imageCacherListener, url, scalingInfo);
		assertTrue(mMaps.isNetworkRequestPendingForUrl(url));
		assertFalse(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));
	}

	private void generateAndValidateDecodeRequest(String url, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		generateAndValidateNetworkRequest(url, imageCacherListener, scalingInfo);
		mMaps.onDownloadComplete(url);
		assertFalse(mMaps.isListenerWaitingOnNetwork(imageCacherListener));
		assertTrue(mMaps.isDecodeRequestPendingForUrlAndScalingInfo(url, scalingInfo));
	}
}
