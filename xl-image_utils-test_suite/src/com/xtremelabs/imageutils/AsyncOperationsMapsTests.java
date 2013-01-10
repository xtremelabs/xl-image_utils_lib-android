/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.xtremelabs.imageutils.AsyncOperationsMaps.AsyncOperationState;
import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.RequestType;
import com.xtremelabs.testactivity.MainActivity;

public class AsyncOperationsMapsTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private AsyncOperationsMaps mMaps;
	private AsyncOperationsObserver mObserver;
	private boolean mAsyncPassed;
	private boolean mAsyncFailed;
	private boolean mDecodeRequiredCalled;

	public AsyncOperationsMapsTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mObserver = new AsyncOperationsObserver() {
			@Override
			public void onImageDecodeRequired(DecodeSignature decodeSignature) {
				mDecodeRequiredCalled = true;
			}

			@Override
			public boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
				return true;
			}

			@Override
			public int getSampleSize(ImageRequest imageRequest) {
				if (imageRequest.getScalingInfo() == null || imageRequest.getScalingInfo().sampleSize == null) {
					return 1;
				} else {
					return imageRequest.getScalingInfo().sampleSize;
				}
			}

			@Override
			public void onImageDetailsRequired(String uri) {
			}

			@Override
			public boolean isNetworkRequestPending(String uri) {
				return true;
			}
		};
		mMaps = new AsyncOperationsMaps(mObserver);

		mAsyncPassed = false;
		mAsyncFailed = false;
		mDecodeRequiredCalled = false;

		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertTrue(mMaps.areMapsEmpty());
	}

	public void testSuccessfulNetworkFlow() {
		String uri = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo), imageCacherListener) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(new ImageRequest(uri, scalingInfo), imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo, null));

		mMaps.onDownloadComplete(uri);
		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.isDetailsRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo, null));

		assertFalse(mAsyncFailed);
	}

	public void testNetworkFlowWithFailure() {
		String uri = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo), imageCacherListener) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(new ImageRequest(uri, scalingInfo), imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo, null));

		mMaps.onDownloadFailed(uri, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPending(uri));

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

		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo1), imageCacherListener1) == AsyncOperationState.NOT_QUEUED);

		mMaps.registerListenerForNetworkRequest(new ImageRequest(uri, scalingInfo1), imageCacherListener1);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo1, null));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo2), imageCacherListener2);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo2, null));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDetailsRequests(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.onDownloadComplete(uri);
		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.isDetailsRequestPending(uri));
		assertTrue(mMaps.isDetailsRequestPending(uri));
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

		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo1), imageCacherListener1) == AsyncOperationState.NOT_QUEUED);

		mMaps.registerListenerForNetworkRequest(new ImageRequest(uri, scalingInfo1), imageCacherListener1);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo1, null));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo2), imageCacherListener2);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo2, null));
		assertEquals(mMaps.getNumPendingDownloads(), 1);
		assertEquals(mMaps.getNumPendingDecodes(), 0);

		mMaps.onDownloadFailed(uri, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo1, null));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo2, null));
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
		assertTrue(mMaps.isNetworkRequestPending(uri));
		mMaps.onDownloadComplete(uri);
		assertEquals(0, mMaps.getNumPendingDownloads());
		assertEquals(1, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumPendingDecodes());

		mMaps.onDetailsRequestFailed(uri, "Forced details failure.");
		assertEquals(0, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumPendingDecodes());

		generateAndValidateNetworkRequest(uri, imageCacherListener, scalingInfo);
		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		mMaps.onDownloadFailed(uri, "Forced download failure");
		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertEquals(0, mMaps.getNumPendingDownloads());
		assertEquals(0, mMaps.getNumPendingDetailsRequests());
		assertEquals(0, mMaps.getNumPendingDecodes());
		assertTrue(mMaps.areMapsEmpty());
	}

	public void testImageDetailsRetrieval() {
		String uri = "blah";
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		assertFalse(mMaps.isDetailsRequestPending(uri));
		mMaps.registerListenerForDetailsRequest(new ImageRequest(uri, scalingInfo), new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		});
		assertTrue(mMaps.isDetailsRequestPending(uri));

		mMaps.onDetailsRequestComplete(uri);
		assertFalse(mMaps.isDetailsRequestPending(uri));
		assertTrue(mMaps.isDecodeRequestPending(uri, scalingInfo, null));
	}

	public void testDecodeSuccess() {
		String uri = "blah";
		ImageCacherListener imageCacherListener = getPassingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateDecodeRequest(uri, imageCacherListener, scalingInfo);

		mMaps.onDecodeSuccess(null, ImageReturnedFrom.DISK, new DecodeSignature(uri, scalingInfo.sampleSize, null));
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo, null));

		assertTrue(mAsyncPassed);
		assertFalse(mAsyncFailed);
	}

	public void testDecodeFailure() {
		String url = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateDecodeRequest(url, imageCacherListener, scalingInfo);

		mMaps.onDecodeFailed(new DecodeSignature(url, scalingInfo.sampleSize, null), "Forced decode failure");
		assertEquals(mMaps.getNumPendingDownloads(), 0);
		assertEquals(mMaps.getNumPendingDecodes(), 0);
		assertFalse(mMaps.isDecodeRequestPending(url, scalingInfo, null));

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

		mMaps.onDecodeSuccess(null, ImageReturnedFrom.DISK, new DecodeSignature(uri1, 1, null));
		assertEquals(mMaps.getNumPendingDecodes(), 2);
		assertEquals(mMaps.getNumListenersForDecode(), 2);

		generateAndValidateDecodeRequest(uri1, imageCacherListener5, scalingInfo5);
		assertEquals(mMaps.getNumPendingDecodes(), 3);
		assertEquals(mMaps.getNumListenersForDecode(), 3);
	}

	public void testDecodeCancel() {
		String uri = "blah";
		ImageCacherListener imageCacherListener = getFailingImageCacherListener();
		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.sampleSize = 1;

		generateAndValidateDecodeRequest(uri, imageCacherListener, scalingInfo);

		mMaps.cancelPendingRequest(imageCacherListener);
		assertTrue(mMaps.isDecodeRequestPending(uri, scalingInfo, null));
		mMaps.onDecodeSuccess(null, ImageReturnedFrom.DISK, new DecodeSignature(uri, scalingInfo.sampleSize, null));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo, null));

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
		assertTrue(mMaps.isDetailsRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo1, null));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo2, null));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo3, null));

		mMaps.onDetailsRequestComplete(uri);
		assertFalse(mMaps.areMapsEmpty());
		assertEquals(0, mMaps.getNumListenersForDetails());
		assertEquals(0, mMaps.getNumPendingDetailsRequests());
		assertEquals(3, mMaps.getNumListenersForDecode());
		assertEquals(2, mMaps.getNumPendingDecodes());
	}

	public void testIsDetailsRequestPending() {
		String uri = "uri";

		assertFalse(mMaps.isDetailsRequestPending(uri));

		mMaps.registerListenerForDetailsRequest(new ImageRequest(uri, new ScalingInfo()), new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
			}

			@Override
			public void onFailure(String message) {
			}
		});

		assertTrue(mMaps.isDetailsRequestPending(uri));
	}

	public void testDetailsRequestCompleteForDiskPrecacheRequest() {
		String uri = "Random URI";

		ImageRequest imageRequest = new ImageRequest(uri);
		imageRequest.setRequestType(RequestType.FULL_REQUEST);

		mMaps.registerListenerForDetailsRequest(imageRequest, getBlankImageCacherListener());
		mMaps.onDetailsRequestComplete(uri);

		assertTrue(mDecodeRequiredCalled);

		mDecodeRequiredCalled = false;

		imageRequest.setRequestType(RequestType.CACHE_TO_DISK);

		mMaps.registerListenerForDetailsRequest(imageRequest, getBlankImageCacherListener());
		mMaps.onDetailsRequestComplete(uri);

		assertFalse(mDecodeRequiredCalled);
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
		assertFalse(mMaps.isNetworkRequestPending(uri));
		assertTrue(mMaps.queueListenerIfRequestPending(new ImageRequest(uri, scalingInfo), imageCacherListener) == AsyncOperationState.NOT_QUEUED);
		mMaps.registerListenerForNetworkRequest(new ImageRequest(uri, scalingInfo), imageCacherListener);
		assertTrue(mMaps.isNetworkRequestPending(uri));
		assertFalse(mMaps.isDecodeRequestPending(uri, scalingInfo, null));
	}

	private void generateAndValidateDecodeRequest(String uri, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		mMaps.registerListenerForDecode(new DecodeSignature(uri, scalingInfo.sampleSize, null), imageCacherListener);
		assertFalse(mMaps.isListenerWaitingOnNetwork(imageCacherListener));
		assertFalse(mMaps.isListenerWaitingOnDetails(imageCacherListener));
		assertTrue(mMaps.isDecodeRequestPending(uri, scalingInfo, null));
	}
}
