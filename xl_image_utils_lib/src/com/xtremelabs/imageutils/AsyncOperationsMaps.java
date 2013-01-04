package com.xtremelabs.imageutils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.OperationTracker.KeyReferenceProvider;
import com.xtremelabs.imageutils.OperationTracker.OperationTransferer;

public class AsyncOperationsMaps {
	private static final String PREFIX = "MAPS - ";

	public enum AsyncOperationState {
		QUEUED_FOR_NETWORK_REQUEST, QUEUED_FOR_DETAILS_REQUEST, QUEUED_FOR_DECODE_REQUEST, NOT_QUEUED
	}

	private final OperationTracker<String, RequestParameters, ImageCacherListener> mNetworkOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<String, RequestParameters, ImageCacherListener> mDetailsOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<DecodeOperationParameters, ImageCacherListener, ImageCacherListener> mDecodeOperationTracker = new OperationTracker<DecodeOperationParameters, ImageCacherListener, ImageCacherListener>();

	private final AsyncOperationsObserver mAsyncOperationsObserver;

	private final KeyReferenceProvider<String, RequestParameters, ImageCacherListener> mNetworkAndDetailsKeyReferenceProvider = new KeyReferenceProvider<String, RequestParameters, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(String operationKey, RequestParameters operationListValue) {
			return operationListValue.mImageCacherListener;
		}
	};

	private final KeyReferenceProvider<DecodeOperationParameters, ImageCacherListener, ImageCacherListener> mDecodeReferenceProvider = new KeyReferenceProvider<DecodeOperationParameters, ImageCacherListener, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(DecodeOperationParameters decodeOperationParameters, ImageCacherListener imageCacherListener) {
			return imageCacherListener;
		}
	};

	public AsyncOperationsMaps(AsyncOperationsObserver asyncOperationsObserver) {
		mAsyncOperationsObserver = asyncOperationsObserver;
	}

	public synchronized boolean isNetworkRequestPendingForUri(String uri) {
		return mNetworkOperationTracker.hasPendingOperation(uri);
	}

	public synchronized boolean isDetailsRequestPendingForUri(String uri) {
		return mDetailsOperationTracker.hasPendingOperation(uri);
	}

	public synchronized boolean isDecodeRequestPendingForUriAndScalingInfo(String uri, ScalingInfo scalingInfo) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(uri, mAsyncOperationsObserver.getSampleSize(new ImageRequest(uri, scalingInfo)));
		return mDecodeOperationTracker.hasPendingOperation(decodeOperationParameters);
	}

	public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		String uri = imageRequest.getUri();
		ScalingInfo scalingInfo = imageRequest.getScalingInfo();
		if (isNetworkRequestPendingForUri(uri)) {
			registerListenerForNetworkRequest(imageCacherListener, uri, scalingInfo);
			return AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		}

		int sampleSize = mAsyncOperationsObserver.getSampleSize(imageRequest);
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(uri, sampleSize);
		if (isDecodeRequestPendingForParams(decodeOperationParameters)) {
			queueForDecodeRequest(imageCacherListener, decodeOperationParameters);
			return AsyncOperationState.QUEUED_FOR_DECODE_REQUEST;
		}

		return AsyncOperationState.NOT_QUEUED;
	}

	public synchronized void registerListenerForNetworkRequest(ImageCacherListener imageCacherListener, String uri, ScalingInfo scalingInfo) {
		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, scalingInfo);
		mNetworkOperationTracker.register(uri, networkRequestParameters, imageCacherListener);
	}

	// TODO Fix naming convention. The NetworkRequestParameter object is no longer specific to network requests.
	public void registerListenerForDetailsRequest(ImageCacherListener imageCacherListener, String uri, ScalingInfo scalingInfo) {
		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, scalingInfo);
		mDetailsOperationTracker.register(uri, networkRequestParameters, imageCacherListener);
	}

	public synchronized void registerListenerForDecode(ImageCacherListener imageCacherListener, String uri, int sampleSize) {
		if (Logger.logMaps()) {
			Logger.d(PREFIX + "Registering listener for decode: " + uri);
		}

		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(uri, sampleSize);
		queueForDecodeRequest(imageCacherListener, decodeOperationParameters);
	}

	public void onDecodeSuccess(Bitmap bitmap, String url, int sampleSize, ImageReturnedFrom returnedFrom) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);
		List<ImageCacherListener> listeners = mDecodeOperationTracker.removeList(decodeOperationParameters, mDecodeReferenceProvider);

		for (ImageCacherListener listener : listeners) {
			listener.onImageAvailable(new ImageResponse(bitmap, returnedFrom, ImageResponseStatus.SUCCESS));
		}
	}

	public void onDecodeFailed(String url, int sampleSize, String message) {
		List<ImageCacherListener> listeners = mDecodeOperationTracker.removeList(new DecodeOperationParameters(url, sampleSize), mDecodeReferenceProvider);
		for (ImageCacherListener listener : listeners) {
			listener.onFailure(message);
		}
	}

	public void onDownloadComplete(String uri) {
		moveNetworkListenersToDetailsQueue(uri);
		mAsyncOperationsObserver.onImageDetailsRequired(uri);
	}

	public void onDownloadFailed(String uri, String message) {
		List<RequestParameters> requestParametersList = mNetworkOperationTracker.removeList(uri, mNetworkAndDetailsKeyReferenceProvider);

		for (RequestParameters networkRequestParameters : requestParametersList) {
			networkRequestParameters.mImageCacherListener.onFailure(message);
		}
	}

	public void onDetailsRequestComplete(String uri) {
		final Set<DecodeOperationParameters> decodeRequestsToMake = new HashSet<DecodeOperationParameters>();

		synchronized (this) {
			mDetailsOperationTracker.transferOperation(uri, new OperationTransferer<String, RequestParameters, ImageCacherListener>() {
				@Override
				public void transferOperation(String uri, RequestParameters networkRequestParameters, ImageCacherListener imageCacherListener) {
					int sampleSize = mAsyncOperationsObserver.getSampleSize(new ImageRequest(uri, networkRequestParameters.mScalingInfo));
					DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(uri, sampleSize);

					queueForDecodeRequest(networkRequestParameters.mImageCacherListener, decodeOperationParameters);
					decodeRequestsToMake.add(decodeOperationParameters);
				}
			}, mNetworkAndDetailsKeyReferenceProvider);
		}

		for (DecodeOperationParameters decodeOperationParameters : decodeRequestsToMake) {
			mAsyncOperationsObserver.onImageDecodeRequired(decodeOperationParameters.mUrl, decodeOperationParameters.mSampleSize);
		}
	}

	public void onDetailsRequestFailed(String uri, String message) {
		List<RequestParameters> list = mDetailsOperationTracker.removeList(uri, mNetworkAndDetailsKeyReferenceProvider);

		for (RequestParameters networkRequestParameters : list) {
			networkRequestParameters.mImageCacherListener.onFailure(message);
		}
	}

	public synchronized void cancelPendingRequest(ImageCacherListener imageCacherListener) {
		if (mNetworkOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, false) || mDetailsOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, false)
				|| mDecodeOperationTracker.removeRequest(imageCacherListener, mDecodeReferenceProvider, false)) {
			return;
		}
	}

	public synchronized int getNumPendingDownloads() {
		return mNetworkOperationTracker.getNumPendingOperations();
	}

	public synchronized int getNumPendingDetailsRequests() {
		return mDetailsOperationTracker.getNumPendingOperations();
	}

	public synchronized int getNumPendingDecodes() {
		return mDecodeOperationTracker.getNumPendingOperations();
	}

	public synchronized int getNumListenersForNetwork() {
		return mNetworkOperationTracker.getNumListValues();
	}

	public synchronized int getNumListenersForDetails() {
		return mDetailsOperationTracker.getNumListValues();
	}

	public synchronized int getNumListenersForDecode() {
		return mDecodeOperationTracker.getNumListValues();
	}

	public synchronized boolean isListenerWaitingOnNetwork(ImageCacherListener imageCacherListener) {
		return mNetworkOperationTracker.isOperationPendingForReference(imageCacherListener);
	}

	public synchronized boolean isListenerWaitingOnDetails(ImageCacherListener imageCacherListener) {
		return mDetailsOperationTracker.isOperationPendingForReference(imageCacherListener);
	}

	public synchronized boolean isListenerWaitingOnDecode(ImageCacherListener imageCacherListener) {
		return mDecodeOperationTracker.isOperationPendingForReference(imageCacherListener);
	}

	public synchronized boolean areMapsEmpty() {
		return mNetworkOperationTracker.getNumPendingOperations() == 0 && mNetworkOperationTracker.getNumListValues() == 0 && mDecodeOperationTracker.getNumPendingOperations() == 0 && mDecodeOperationTracker.getNumListValues() == 0
				&& mDetailsOperationTracker.getNumPendingOperations() == 0 && mDetailsOperationTracker.getNumListValues() == 0;
	}

	private synchronized void queueForDecodeRequest(ImageCacherListener imageCacherListener, DecodeOperationParameters decodeOperationParameters) {
		mDecodeOperationTracker.register(decodeOperationParameters, imageCacherListener, imageCacherListener);
	}

	private synchronized void moveNetworkListenersToDetailsQueue(String uri) {
		mNetworkOperationTracker.transferOperationToTracker(uri, mDetailsOperationTracker, mNetworkAndDetailsKeyReferenceProvider);
	}

	private synchronized boolean isDecodeRequestPendingForParams(DecodeOperationParameters decodeOperationParameters) {
		return mDecodeOperationTracker.hasPendingOperation(decodeOperationParameters);
	}

	private class RequestParameters {
		ImageCacherListener mImageCacherListener;
		ScalingInfo mScalingInfo;

		RequestParameters(ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
			mImageCacherListener = imageCacherListener;
			mScalingInfo = scalingInfo;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}

			if (!(o instanceof RequestParameters)) {
				return false;
			}

			RequestParameters params = (RequestParameters) o;
			if (params.mScalingInfo != mScalingInfo) {
				return false;
			}

			if (params.mImageCacherListener != mImageCacherListener) {
				return false;
			}

			return true;
		}
	}
}
