package com.xtremelabs.imageutils;

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
			public boolean isNetworkRequestPendingForUri(String uri) {
				return true;
			}

			@Override
			public boolean isDecodeRequestPending(DecodeOperationParameters decodeOperationParameters) {
				return true;
			}

			@Override
			public int getSampleSize(ImageRequest imageRequest) {
				return imageRequest.getScalingInfo().sampleSize;
			}

			@Override
			public void onImageDetailsRequired(String uri) {
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
		String uri = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo), imageCacherListener) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(imageCacherListener, uri, scalingInfo);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo));

		mMaps.onDownloadComplete(uri);
		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.isDetailsRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo));

		assertFalse(mAsyncFailed);
	}

	public void testNetworkFlowWithFailure() {
		String uri = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo), imageCacherListener) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(imageCacherListener, uri, scalingInfo);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo));

		mMaps.onDownloadFailed(uri, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);

		assertTrue(mMaps.areMapsEmpty());
	}

	public void testNetworkQueueSuccess() {
		String uri = "blah";

		ImageCacherListener imageCacherListener1 = getBlankImageCacherListener();
		ImageCacherListener imageCacherListener2 = getBlankImageCacherListener();

		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;

		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 2;

		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo1), imageCacherListener1) == AsyncOperationState.NOT_QUEUED);

		mMaps.registerListenerForNetworkRequest(imageCacherListener1, uri, scalingInfo1);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo1));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo2), imageCacherListener2);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDetailsRequests(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.onDownloadComplete(uri);
		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.isDetailsRequestPendingForUri(uri));
		assertTrue(mMaps.isDetailsRequestPendingForUri(uri));
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDetailsRequests(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		assertFalse(mAsyncFailed);
	}

	public void testNetworkQueueFailure() {
		String uri = "blah";

		ImageCacherListener imageCacherListener1 = getFailingImageCacherListener();
		ImageCacherListener imageCacherListener2 = getFailingImageCacherListener();

		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;

		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 2;

		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo1), imageCacherListener1) == AsyncOperationState.NOT_QUEUED);

		mMaps.registerListenerForNetworkRequest(imageCacherListener1, uri, scalingInfo1);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo1));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo2), imageCacherListener2);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.onDownloadFailed(uri, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo1));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo2));
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);

		assertTrue(mMaps.areMapsEmpty());
	}

	public void testNetworkCancel() {
		String uri = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateNetworkRequest(uri, imageCacherListener, scalingInfo);
		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		mMaps.onDownloadComplete(uri);
		assertEquals(0, mMaps.getNumPendingDownloads());
		assertEquals(1, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumPendingDecodes());

		mMaps.onDetailsRequestFailed(uri, "Forced details failure.");
		assertEquals(0, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumPendingDecodes());

		generateAndValidateNetworkRequest(uri, imageCacherListener, scalingInfo);
		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		mMaps.onDownloadFailed(uri, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertEquals(0, mMaps.getNumPendingDownloads());
		assertEquals(0, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumPendingDecodes());
		assertTrue(mMaps.areMapsEmpty());
	}

	public void testImageDetailsRetrieval() {
		String uri = "blah";
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isDetailsRequestPendingForUri(uri));
		mMaps.registerListenerForDetailsRequest(new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		}, uri, scalingInfo);
		assertTrue(mMaps.isDetailsRequestPendingForUri(uri));

		mMaps.onDetailsRequestComplete(uri);
		assertFalse(mMaps.isDetailsRequestPendingForUri(uri));
		assertTrue(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo));
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
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(url, scalingInfo));

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
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(url, scalingInfo));

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);
	}

	public void testDecodeQueue() {
		String uri1 = "blah1";
		String uri2 = "blah2";

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

		generateAndValidateDecodeRequest(uri1, imageCacherListener1, scalingInfo1);
		assertEquals(mMaps.getNumPendingDecodes(), 1);
		assertEquals(mMaps.getNumListenersForDecode(), 1);

		generateAndValidateDecodeRequest(uri1, imageCacherListener2, scalingInfo2);
		assertEquals(mMaps.getNumPendingDecodes(), 2);
		assertEquals(mMaps.getNumListenersForDecode(), 2);

		generateAndValidateDecodeRequest(uri2, imageCacherListener3, scalingInfo3);
		assertEquals(mMaps.getNumPendingDecodes(), 3);
		assertEquals(mMaps.getNumListenersForDecode(), 3);

		assertEquals(mMaps.queueListenerIfRequestPending(new ImageRequest(uri1, scalingInfo4), imageCacherListener4), AsyncOperationState.QUEUED_FOR_DECODE_REQUEST);
		assertEquals(mMaps.getNumPendingDecodes(), 3);
		assertEquals(mMaps.getNumListenersForDecode(), 4);

		mMaps.onDecodeSuccess(null, uri1, 1, ImageReturnedFrom.DISK);
		assertEquals(mMaps.getNumPendingDecodes(), 2);
		assertEquals(mMaps.getNumListenersForDecode(), 2);

		generateAndValidateDecodeRequest(uri1, imageCacherListener5, scalingInfo5);
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
		assertTrue(mMaps.isDecodeRequestPendingForUriAndScalingInfo(url, scalingInfo));
		mMaps.onDecodeSuccess(null, url, scalingInfo.sampleSize, ImageReturnedFrom.DISK);
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(url, scalingInfo));

		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertEquals(mMaps.getNumListenersForNetwork(), 0);
		assertEquals(mMaps.getNumListenersForDecode(), 0);

		assertTrue(mMaps.areMapsEmpty());
	}

	public void testMovingFromNetworkToDetails() {
		String uri = "blah";
		ImageCacherListener imageCacherListener1 = getBlankImageCacherListener();
		ScalingInfo scalingInfo1 = new ScalingInfo();
		scalingInfo1.sampleSize = 1;
		ImageCacherListener imageCacherListener2 = getBlankImageCacherListener();
		ScalingInfo scalingInfo2 = new ScalingInfo();
		scalingInfo2.sampleSize = 1;
		ImageCacherListener imageCacherListener3 = getBlankImageCacherListener();
		ScalingInfo scalingInfo3 = new ScalingInfo();
		scalingInfo3.sampleSize = 2;

		generateAndValidateNetworkRequest(uri, imageCacherListener1, scalingInfo1);
		assertEquals(AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST, mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo2), imageCacherListener2));
		assertEquals(AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST, mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo3), imageCacherListener3));

		mMaps.onDownloadComplete(uri);

		assertEquals(0, mMaps.getNumListenersForNetwork());
		assertEquals(0, mMaps.getNumPendingDownloads());
		assertEquals(3, mMaps.getNumListenersForDetails());
		assertEquals(1, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumListenersForDecode());
		assertEquals(0, mMaps.getNumPendingDecodes());
		assertTrue(mMaps.isDetailsRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo1));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo2));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo3));

		mMaps.onDetailsRequestComplete(uri);
		assertFalse(mMaps.areMapsEmpty());
		assertEquals(0, mMaps.getNumListenersForDetails());
		assertEquals(0, mMaps.getNumPendingDetailsRequests());
		assertEquals(3, mMaps.getNumListenersForDecode());
		assertEquals(2, mMaps.getNumPendingDecodes());
	}

	public void testIsDetailsRequestPending() {
		String uri = "uri";

		assertFalse(mMaps.isDetailsRequestPendingForUri(uri));

		mMaps.registerListenerForDetailsRequest(new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		}, uri, new ScalingInfo());

		assertTrue(mMaps.isDetailsRequestPendingForUri(uri));
	}

	private ImageCacherListener getBlankImageCacherListener() {
		return new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
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
			public void onImageAvailable(ImageResponse imageResponse) {
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
			public void onImageAvailable(ImageResponse imageResponse) {
				mAsyncFailed = true;
			}

			@Override
			public void onFailure(String message) {
				Log.d("FailingImageCacherListener", "on failure: " + message);
				mAsyncPassed = true;
			}
		};
	}

	private void generateAndValidateNetworkRequest(String uri, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		assertFalse(mMaps.isNetworkRequestPendingForUri(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo), imageCacherListener) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(imageCacherListener, uri, scalingInfo);
		assertTrue(mMaps.isNetworkRequestPendingForUri(uri));
		assertFalse(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo));
	}

	private void generateAndValidateDecodeRequest(String uri, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		mMaps.registerListenerForDecode(imageCacherListener, uri, scalingInfo.sampleSize);
		assertFalse(mMaps.isListenerWaitingOnNetwork(imageCacherListener));
		assertFalse(mMaps.isListenerWaitingOnDetails(imageCacherListener));
		assertTrue(mMaps.isDecodeRequestPendingForUriAndScalingInfo(uri, scalingInfo));
	}
}
