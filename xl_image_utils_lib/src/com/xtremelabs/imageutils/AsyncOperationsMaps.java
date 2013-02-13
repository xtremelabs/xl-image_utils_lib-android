package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.util.Log;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.ImageRequestType;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.OperationTracker.KeyReferenceProvider;
import com.xtremelabs.imageutils.OperationTracker.OperationTransferer;

class AsyncOperationsMaps {

	public enum AsyncOperationState {
		QUEUED_FOR_NETWORK_REQUEST, QUEUED_FOR_DETAILS_REQUEST, QUEUED_FOR_DECODE_REQUEST, NOT_QUEUED
	}

	private final OperationTracker<String, RequestParameters, ImageCacherListener> mNetworkOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<String, RequestParameters, ImageCacherListener> mDetailsOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<DecodeSignature, RequestParameters, ImageCacherListener> mDecodeOperationTracker = new OperationTracker<DecodeSignature, RequestParameters, ImageCacherListener>();

	private final OperationsObserver mObserver;

	private final AuxiliaryExecutor mNetworkExecutor;
	private final AuxiliaryExecutor mDiskExecutor;

	public AsyncOperationsMaps(OperationsObserver observer) {
		mObserver = observer;
		mNetworkExecutor = new AuxiliaryExecutor.Builder(generateAccessors()).setCorePoolSize(3).create();
		mDiskExecutor = new AuxiliaryExecutor.Builder(generateAccessors()).setCorePoolSize(1).create();
	}

	private PriorityAccessor[] generateAccessors() {
		PriorityAccessor[] accessors = new PriorityAccessor[1];
		accessors[0] = new StackPriorityAccessor();
		// accessors[1] = new StackPriorityAccessor();
		// accessors[2] = new QueuePriorityAccessor();
		// accessors[3] = new QueuePriorityAccessor();
		// accessors[4] = new QueuePriorityAccessor();
		// accessors[5] = new QueuePriorityAccessor();
		return accessors;
	}

	private final KeyReferenceProvider<String, RequestParameters, ImageCacherListener> mNetworkAndDetailsKeyReferenceProvider = new KeyReferenceProvider<String, RequestParameters, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(String operationKey, RequestParameters operationListValue) {
			return operationListValue.imageCacherListener;
		}
	};

	private final KeyReferenceProvider<DecodeSignature, RequestParameters, ImageCacherListener> mDecodeReferenceProvider = new KeyReferenceProvider<DecodeSignature, RequestParameters, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(DecodeSignature decodeSignature, RequestParameters requestParameters) {
			return requestParameters.imageCacherListener;
		}
	};

	public synchronized AsyncOperationState queueListenerIfRequestPending(CacheRequest cacheRequest, ImageCacherListener imageCacherListener) {
		AsyncOperationState state = AsyncOperationState.NOT_QUEUED;
		DecodeSignature decodeSignature;

		if (isNetworkRequestPending(cacheRequest)) {
			registerNetworkRequest(cacheRequest, imageCacherListener);
			state = AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		} else if (isDetailsRequestPending(cacheRequest)) {
			registerDetailsRequest(cacheRequest, imageCacherListener);
			state = AsyncOperationState.QUEUED_FOR_DETAILS_REQUEST;
		} else if (cacheRequest.getRequestType() != ImageRequestType.PRECACHE_TO_DISK && isDecodeRequestPending((decodeSignature = getDecodeSignature(cacheRequest)))) {
			registerDecodeRequest(cacheRequest, decodeSignature, imageCacherListener);
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

	synchronized boolean isNetworkRequestPending(CacheRequest imageRequest) {
		return mNetworkOperationTracker.hasPendingOperation(imageRequest.getUri());
	}

	private synchronized boolean isDetailsRequestPending(CacheRequest imageRequest) {
		return mDetailsOperationTracker.hasPendingOperation(imageRequest.getUri());
	}

	private synchronized boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
		return mDecodeOperationTracker.hasPendingOperation(decodeSignature);
	}

	/*
	 * ********************
	 * 
	 * Registration Calls
	 * 
	 * ********************
	 */

	synchronized void registerNetworkRequest(CacheRequest imageRequest, ImageCacherListener imageCacherListener) {
		Prioritizable prioritizable = mObserver.getNetworkRunnable(imageRequest);

		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, imageRequest, prioritizable);
		mNetworkOperationTracker.register(imageRequest.getUri(), networkRequestParameters, imageCacherListener);

		Log.d("JAMIE", "JAMIE - Downloading: " + imageRequest.getUri());
		mNetworkExecutor.execute(prioritizable);
	}

	synchronized void registerDetailsRequest(CacheRequest imageRequest, ImageCacherListener imageCacherListener) {
		Prioritizable prioritizable = mObserver.getDetailsRunnable(imageRequest);

		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, imageRequest, prioritizable);
		mDetailsOperationTracker.register(imageRequest.getUri(), networkRequestParameters, imageCacherListener);

		mDiskExecutor.execute(prioritizable);
	}

	synchronized void registerDecodeRequest(CacheRequest cacheRequest, DecodeSignature decodeSignature, ImageCacherListener imageCacherListener) {
		Log.d("JAMIE", "JAMIE - Decoding: " + cacheRequest.getUri());
		Prioritizable prioritizable = mObserver.getDecodeRunnable(cacheRequest, decodeSignature);

		RequestParameters requestParameters = new RequestParameters(imageCacherListener, cacheRequest, prioritizable);
		mDecodeOperationTracker.register(decodeSignature, requestParameters, imageCacherListener);

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
		Log.d("JAMIE", "JAMIE - Download complete: " + uri);

		// FIXME Do we want the decode to happen regardless of whether or not we have runnables to complete the decode?
		// TODO Create a queue for details requests and force them to happen first on disk?
		List<RequestParameters> requests = mNetworkOperationTracker.transferOperationToTracker(uri, mDetailsOperationTracker, mNetworkAndDetailsKeyReferenceProvider);
		mNetworkExecutor.notifyRequestComplete(new Request<String>(uri));
		for (RequestParameters request : requests) {
			Prioritizable prioritizable = mObserver.getDetailsRunnable(request.cacheRequest);
			mDiskExecutor.execute(prioritizable);
		}
	}

	public void onDownloadFailed(String uri, String message) {
		Log.d("JAMIE", "JAMIE - Download failed: " + uri);
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
					ImageRequestType requestType = networkRequestParameters.cacheRequest.getRequestType();

					switch (requestType) {
					case PRECACHE_TO_DISK:
					case PRECACHE_TO_DISK_FOR_ADAPTER:
						diskCacheRequestsToReportSuccess.add(imageCacherListener);
						return;
					case PRECACHE_TO_MEMORY:
					case PRECACHE_TO_MEMORY_FOR_ADAPTER:
					case ADAPTER_REQUEST:
					case DEFAULT:
						int sampleSize = mObserver.getSampleSize(new CacheRequest(uri, networkRequestParameters.cacheRequest.getScalingInfo()));
						DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, networkRequestParameters.cacheRequest.getOptions().preferedConfig);

						registerDecodeRequest(networkRequestParameters.cacheRequest, decodeSignature, imageCacherListener);
						break;
					default:
						throw new IllegalStateException("The request type of the cache request was not present!");
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
		Log.d("JAMIE", "JAMIE - Decode successful: " + decodeSignature.mUri);
		List<RequestParameters> requestParametersList = null;
		synchronized (this) {
			requestParametersList = mDecodeOperationTracker.removeList(decodeSignature, mDecodeReferenceProvider);
			mDiskExecutor.notifyRequestComplete(new Request<DecodeSignature>(decodeSignature));
		}

		if (requestParametersList != null) {
			for (RequestParameters params : requestParametersList) {
				params.imageCacherListener.onImageAvailable(new ImageResponse(bitmap, returnedFrom, ImageResponseStatus.SUCCESS));
			}
		}
	}

	public void onDecodeFailed(DecodeSignature decodeSignature, String message) {
		Log.d("JAMIE", "JAMIE - Decode failed! " + decodeSignature.mUri);
		List<RequestParameters> requestParametersList = null;
		synchronized (this) {
			requestParametersList = mDecodeOperationTracker.removeList(decodeSignature, mDecodeReferenceProvider);
			mDiskExecutor.notifyRequestComplete(new Request<DecodeSignature>(decodeSignature));
		}

		if (requestParametersList != null) {
			for (RequestParameters params : requestParametersList) {
				params.imageCacherListener.onFailure(message);
			}
		}
	}

	public synchronized void cancelPendingRequest(ImageCacherListener imageCacherListener) {
		// if (isNetworkOperationPendingForListener(imageCacherListener))
		// cancelNetworkPrioritizable(imageCacherListener);
		// else if (isDetailsOperationPendingForListener(imageCacherListener))
		// cancelDetailsPrioritizable(imageCacherListener);
		// else if (isDecodeOperationPendingForListener(imageCacherListener))
		// cancelDecodePrioritizable(imageCacherListener);
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

	private synchronized void cancelNetworkPrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mNetworkOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, true);
		Log.d("JAMIE", "JAMIE - Cancelled: " + requestParameters.cacheRequest.getUri());
		mNetworkExecutor.cancel(requestParameters.prioritizable);
	}

	private void cancelDetailsPrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mDetailsOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, true);
		mDiskExecutor.cancel(requestParameters.prioritizable);
	}

	private void cancelDecodePrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mDecodeOperationTracker.removeRequest(imageCacherListener, mDecodeReferenceProvider, true);
		mDiskExecutor.cancel(requestParameters.prioritizable);
	}

	private synchronized DecodeSignature getDecodeSignature(CacheRequest cacheRequest) {
		int sampleSize = mObserver.getSampleSize(cacheRequest);
		return new DecodeSignature(cacheRequest.getUri(), sampleSize, cacheRequest.getOptions().preferedConfig);
	}

	static interface OperationsObserver {
		public Prioritizable getNetworkRunnable(CacheRequest cacheRequest);

		public Prioritizable getDecodeRunnable(CacheRequest cacheRequest, DecodeSignature decodeSignature);

		public Prioritizable getDetailsRunnable(CacheRequest cacheRequest);

		public int getSampleSize(CacheRequest cacheRequest);
	}

	private class RequestParameters {
		ImageCacherListener imageCacherListener;
		CacheRequest cacheRequest;
		Prioritizable prioritizable;

		RequestParameters(ImageCacherListener imageCacherListener, CacheRequest imageRequest, Prioritizable prioritizable) {
			this.imageCacherListener = imageCacherListener;
			this.cacheRequest = imageRequest;
			this.prioritizable = prioritizable;
		}
	}
}
