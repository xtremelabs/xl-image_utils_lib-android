package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.RequestType;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.OperationTracker.KeyReferenceProvider;
import com.xtremelabs.imageutils.OperationTracker.OperationTransferer;

class AsyncOperationsMaps {

	public enum AsyncOperationState {
		QUEUED_FOR_NETWORK_REQUEST, QUEUED_FOR_DETAILS_REQUEST, QUEUED_FOR_DECODE_REQUEST, NOT_QUEUED
	}

	private final OperationTracker<String, RequestParameters, ImageCacherListener> mNetworkOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<String, RequestParameters, ImageCacherListener> mDetailsOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<DecodeSignature, ImageCacherListener, ImageCacherListener> mDecodeOperationTracker = new OperationTracker<DecodeSignature, ImageCacherListener, ImageCacherListener>();

	private final OperationsObserver mObserver;

	private final AuxiliaryExecutor mNetworkExecutor;
	private final AuxiliaryExecutor mDiskExecutor;

	public AsyncOperationsMaps(OperationsObserver observer) {
		mObserver = observer;
		mNetworkExecutor = new AuxiliaryExecutor.Builder(new PriorityAccessor[] { new StackPriorityAccessor() }).setCorePoolSize(3).create();
		mDiskExecutor = new AuxiliaryExecutor.Builder(new PriorityAccessor[] { new StackPriorityAccessor() }).setCorePoolSize(1).create();
	}

	private final KeyReferenceProvider<String, RequestParameters, ImageCacherListener> mNetworkAndDetailsKeyReferenceProvider = new KeyReferenceProvider<String, RequestParameters, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(String operationKey, RequestParameters operationListValue) {
			return operationListValue.imageCacherListener;
		}
	};

	private final KeyReferenceProvider<DecodeSignature, ImageCacherListener, ImageCacherListener> mDecodeReferenceProvider = new KeyReferenceProvider<DecodeSignature, ImageCacherListener, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(DecodeSignature decodeSignature, ImageCacherListener imageCacherListener) {
			return imageCacherListener;
		}
	};

	public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		AsyncOperationState state = AsyncOperationState.NOT_QUEUED;
		DecodeSignature decodeSignature;

		if (isNetworkRequestPending(imageRequest)) {
			registerNetworkRequest(imageRequest, imageCacherListener);
			state = AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		} else if (isDetailsRequestPending(imageRequest)) {
			registerDetailsRequest(imageRequest, imageCacherListener);
			state = AsyncOperationState.QUEUED_FOR_DETAILS_REQUEST;
		} else if (isDecodeRequestPending((decodeSignature = getDecodeSignature(imageRequest)))) {
			registerDecodeRequest(imageCacherListener, decodeSignature);
			state = AsyncOperationState.QUEUED_FOR_DECODE_REQUEST;
		}

		return state;
	}

	/*
	 * *****************************
	 * 
	 * Checks for pending requests
	 * 
	 * *****************************
	 */

	boolean isNetworkRequestPending(ImageRequest imageRequest) {
		return mNetworkOperationTracker.hasPendingOperation(imageRequest.getUri());
	}

	private boolean isDetailsRequestPending(ImageRequest imageRequest) {
		return mDetailsOperationTracker.hasPendingOperation(imageRequest.getUri());
	}

	private boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
		return mDecodeOperationTracker.hasPendingOperation(decodeSignature);
	}

	/*
	 * ********************
	 * 
	 * Registration Calls
	 * 
	 * ********************
	 */

	synchronized void registerNetworkRequest(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		Prioritizable prioritizable = mObserver.getNetworkRunnable(imageRequest);

		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, imageRequest, prioritizable);
		mNetworkOperationTracker.register(imageRequest.getUri(), networkRequestParameters, imageCacherListener);

		mNetworkExecutor.execute(prioritizable);
	}

	synchronized void registerDetailsRequest(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		Prioritizable prioritizable = mObserver.getDetailsRunnable(imageRequest);

		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, imageRequest, prioritizable);
		mDetailsOperationTracker.register(imageRequest.getUri(), networkRequestParameters, imageCacherListener);

		mDiskExecutor.execute(prioritizable);
	}

	synchronized void registerDecodeRequest(ImageCacherListener imageCacherListener, DecodeSignature decodeSignature) {
		// FIXME The decode operation requires a reference to the runnable...
		mDecodeOperationTracker.register(decodeSignature, imageCacherListener, imageCacherListener);

		Prioritizable prioritizable = mObserver.getDecodeRunnable(decodeSignature);
		mDiskExecutor.execute(prioritizable);
	}

	/*
	 * ********************************
	 * 
	 * Callbacks from the ImageCacher
	 * 
	 * ********************************
	 */

	public synchronized void onDownloadComplete(String uri) {
		// FIXME Do we want the decode to happen regardless of whether or not we have runnables to complete the decode?
		List<RequestParameters> requests = mNetworkOperationTracker.transferOperationToTracker(uri, mDetailsOperationTracker, mNetworkAndDetailsKeyReferenceProvider);
		mNetworkExecutor.notifyRequestComplete(new Request<String>(uri));
		for (RequestParameters request : requests) {
			Prioritizable prioritizable = mObserver.getDetailsRunnable(request.imageRequest);
			mDiskExecutor.execute(prioritizable);
		}
	}

	public void onDownloadFailed(String uri, String message) {
		List<RequestParameters> requestParametersList;
		synchronized (this) {
			requestParametersList = mNetworkOperationTracker.removeList(uri, mNetworkAndDetailsKeyReferenceProvider);
			mNetworkExecutor.notifyRequestComplete(new Request<String>(uri));
		}

		if (requestParametersList != null) {
			for (RequestParameters networkRequestParameters : requestParametersList) {
				networkRequestParameters.imageCacherListener.onFailure(message);
			}
		}
	}

	public void onDetailsRequestComplete(String uri) {
		final List<ImageCacherListener> diskCacheRequestsToReportSuccess = new ArrayList<ImageCacherListener>();

		synchronized (this) {
			mDiskExecutor.notifyRequestComplete(new Request<String>(uri));
			mDetailsOperationTracker.transferOperation(uri, new OperationTransferer<String, RequestParameters, ImageCacherListener>() {
				@Override
				public void transferOperation(String uri, RequestParameters networkRequestParameters, ImageCacherListener imageCacherListener) {
					RequestType requestType = networkRequestParameters.imageRequest.getRequestType();

					switch (requestType) {
					case CACHE_TO_DISK:
						diskCacheRequestsToReportSuccess.add(imageCacherListener);
						return;
					case CACHE_TO_DISK_AND_MEMORY:
					case FULL_REQUEST:
						int sampleSize = mObserver.getSampleSize(new ImageRequest(uri, networkRequestParameters.imageRequest.getScalingInfo()));
						DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, networkRequestParameters.imageRequest.getOptions().preferedConfig);

						registerDecodeRequest(imageCacherListener, decodeSignature);
						break;
					}
				}
			}, mNetworkAndDetailsKeyReferenceProvider);
		}

		for (ImageCacherListener listener : diskCacheRequestsToReportSuccess) {
			listener.onImageAvailable(new ImageResponse(null, null, ImageResponseStatus.CACHED_ON_DISK));
		}
	}

	public void onDetailsRequestFailed(String uri, String message) {
		List<RequestParameters> list;
		synchronized (this) {
			list = mDetailsOperationTracker.removeList(uri, mNetworkAndDetailsKeyReferenceProvider);
			mDiskExecutor.notifyRequestComplete(new Request<String>(uri));
		}

		if (list != null) {
			for (RequestParameters networkRequestParameters : list) {
				networkRequestParameters.imageCacherListener.onFailure(message);
			}
		}
	}

	public void onDecodeSuccess(Bitmap bitmap, ImageReturnedFrom returnedFrom, DecodeSignature decodeSignature) {
		List<ImageCacherListener> listeners = null;
		synchronized (this) {
			listeners = mDecodeOperationTracker.removeList(decodeSignature, mDecodeReferenceProvider);
			mDiskExecutor.notifyRequestComplete(new Request<DecodeSignature>(decodeSignature));
		}

		if (listeners != null) {
			for (ImageCacherListener listener : listeners) {
				listener.onImageAvailable(new ImageResponse(bitmap, returnedFrom, ImageResponseStatus.SUCCESS));
			}
		}
	}

	public void onDecodeFailed(DecodeSignature decodeSignature, String message) {
		List<ImageCacherListener> listeners = null;
		synchronized (this) {
			listeners = mDecodeOperationTracker.removeList(decodeSignature, mDecodeReferenceProvider);
			mDiskExecutor.notifyRequestComplete(new Request<DecodeSignature>(decodeSignature));
		}

		if (listeners != null) {
			for (ImageCacherListener listener : listeners) {
				listener.onFailure(message);
			}
		}
	}

	public synchronized void cancelPendingRequest(ImageCacherListener imageCacherListener) {
		if (isNetworkOperationPendingForListener(imageCacherListener))
			cancelNetworkPrioritizable(imageCacherListener);
		else if (isDetailsOperationPendingForListener(imageCacherListener))
			cancelDetailsPrioritizable(imageCacherListener);
		else if (isDecodeOperationPendingForListener(imageCacherListener))
			cancelDecodePrioritizable(imageCacherListener);
	}

	private boolean isNetworkOperationPendingForListener(ImageCacherListener imageCacherListener) {
		return mNetworkOperationTracker.isOperationPendingForReference(imageCacherListener);
	}

	private boolean isDetailsOperationPendingForListener(ImageCacherListener imageCacherListener) {
		return mDetailsOperationTracker.isOperationPendingForReference(imageCacherListener);
	}

	private boolean isDecodeOperationPendingForListener(ImageCacherListener imageCacherListener) {
		return mDecodeOperationTracker.isOperationPendingForReference(imageCacherListener);
	}

	private void cancelNetworkPrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mNetworkOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, true);
		requestParameters.prioritizable.cancel();
	}

	private void cancelDetailsPrioritizable(ImageCacherListener imageCacherListener) {

	}

	private void cancelDecodePrioritizable(ImageCacherListener imageCacherListener) {

	}

	private synchronized DecodeSignature getDecodeSignature(ImageRequest imageRequest) {
		int sampleSize = mObserver.getSampleSize(imageRequest);
		return new DecodeSignature(imageRequest.getUri(), sampleSize, imageRequest.getOptions().preferedConfig);
	}

	static interface OperationsObserver {
		public Prioritizable getNetworkRunnable(ImageRequest imageRequest);

		public Prioritizable getDecodeRunnable(DecodeSignature decodeSignature);

		public Prioritizable getDetailsRunnable(ImageRequest imageRequest);

		public int getSampleSize(ImageRequest imageRequest);
	}

	private class RequestParameters {
		ImageCacherListener imageCacherListener;
		ImageRequest imageRequest;
		Prioritizable prioritizable;

		RequestParameters(ImageCacherListener imageCacherListener, ImageRequest imageRequest, Prioritizable prioritizable) {
			this.imageCacherListener = imageCacherListener;
			this.imageRequest = imageRequest;
			this.prioritizable = prioritizable;
		}
	}
}
