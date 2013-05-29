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

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.AdapterAccessor.AdapterAccessorType;
import com.xtremelabs.imageutils.AdapterAccessor.RequestObserver;
import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.OperationTracker.KeyReferenceProvider;
import com.xtremelabs.imageutils.OperationTracker.OperationTransferer;
import com.xtremelabs.imageutils.OperationTracker.ValueMatcher;

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
		mNetworkExecutor = new AuxiliaryExecutor.Builder(generateAccessors(mNetworkRequestObserver)).setCorePoolSize(3).create();
		mDiskExecutor = new AuxiliaryExecutor.Builder(generateAccessors(mDiskRequestObserver)).setCorePoolSize(1).create();
	}

	private static PriorityAccessor[] generateAccessors(RequestObserver requestObserver) {
		PriorityAccessor[] accessors = new PriorityAccessor[7];
		accessors[0] = new StackPriorityAccessor();
		accessors[1] = new StackPriorityAccessor();
		accessors[2] = new AdapterAccessor(AdapterAccessorType.PRECACHE_MEMORY, requestObserver);
		accessors[3] = new AdapterAccessor(AdapterAccessorType.PRECACHE_DISK, requestObserver);
		accessors[4] = new AdapterAccessor(AdapterAccessorType.DEPRIORITIZED, requestObserver);
		accessors[5] = new QueuePriorityAccessor();
		accessors[6] = new QueuePriorityAccessor();
		return accessors;
	}

	public void notifyDirectionSwapped(final CacheKey cacheKey) {
		mNetworkExecutor.notifySwap(cacheKey, 4, 2, 3);
		mDiskExecutor.notifySwap(cacheKey, 4, 2, 3);
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

		ImageRequestType imageRequestType = cacheRequest.getImageRequestType();

		if (isNetworkRequestPending(cacheRequest)) {
			registerNetworkRequest(cacheRequest, imageCacherListener);
			state = AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		} else if (isDetailsRequestPending(cacheRequest)) {
			registerDetailsRequest(cacheRequest, imageCacherListener, ImageReturnedFrom.DISK);
			state = AsyncOperationState.QUEUED_FOR_DETAILS_REQUEST;
		} else if (imageRequestType != ImageRequestType.PRECACHE_TO_DISK && isDecodeRequestPending((decodeSignature = getDecodeSignature(cacheRequest)))) { // FIXME We should be doing this check for all disk requests.
			registerDecodeRequest(cacheRequest, decodeSignature, imageCacherListener, ImageReturnedFrom.DISK);
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

		RequestParameters requestParameters = new RequestParameters(imageCacherListener, imageRequest, prioritizable, ImageReturnedFrom.NETWORK);
		mNetworkOperationTracker.register(imageRequest.getUri(), requestParameters, imageCacherListener);

		mNetworkExecutor.execute(prioritizable);
	}

	synchronized void registerDetailsRequest(CacheRequest imageRequest, ImageCacherListener imageCacherListener, ImageReturnedFrom imageReturnedFrom) {
		Prioritizable prioritizable = mObserver.getDetailsRunnable(imageRequest);

		RequestParameters requestParameters = new RequestParameters(imageCacherListener, imageRequest, prioritizable, imageReturnedFrom);
		mDetailsOperationTracker.register(imageRequest.getUri(), requestParameters, imageCacherListener);

		mDiskExecutor.execute(prioritizable);
	}

	synchronized void registerDecodeRequest(CacheRequest cacheRequest, DecodeSignature decodeSignature, ImageCacherListener imageCacherListener, ImageReturnedFrom imageReturnedFrom) {
		Prioritizable prioritizable = mObserver.getDecodeRunnable(cacheRequest, decodeSignature, imageReturnedFrom);

		RequestParameters requestParameters = new RequestParameters(imageCacherListener, cacheRequest, prioritizable, imageReturnedFrom);
		requestParameters.decodeSignature = decodeSignature;
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
		// FIXME Do we want the decode to happen regardless of whether or not we have runnables to complete the decode?
		// TODO Create a queue for details requests and force them to happen first on disk?
		List<RequestParameters> requests = mNetworkOperationTracker.transferOperationToTracker(uri, mDetailsOperationTracker, mNetworkAndDetailsKeyReferenceProvider);
		mNetworkExecutor.notifyRequestComplete(new Request<String>(uri));
		if (requests != null) {
			for (RequestParameters request : requests) {
				Prioritizable prioritizable = mObserver.getDetailsRunnable(request.cacheRequest);
				mDiskExecutor.execute(prioritizable);
			}
		}
	}

	public void onDownloadFailed(String uri, String message) {
		List<RequestParameters> requestParametersList;
		synchronized (this) {
			requestParametersList = mNetworkOperationTracker.removeList(uri, mNetworkAndDetailsKeyReferenceProvider);
			mNetworkExecutor.notifyRequestComplete(new Request<String>(uri));
		}

		if (requestParametersList != null) {
			for (RequestParameters requestParameters : requestParametersList) {
				requestParameters.imageCacherListener.onFailure(message);
			}
		}
	}

	public void onDetailsRequestComplete(String uri) {
		final List<ImageCacherListener> diskCacheRequestsToReportSuccess = new ArrayList<ImageCacherListener>();

		synchronized (this) {
			mDiskExecutor.notifyRequestComplete(new Request<String>(uri));
			mDetailsOperationTracker.transferOperation(uri, new OperationTransferer<String, RequestParameters, ImageCacherListener>() {
				@Override
				public void transferOperation(String uri, RequestParameters requestParameters, ImageCacherListener imageCacherListener) {
					ImageRequestType requestType = requestParameters.cacheRequest.getImageRequestType();

					switch (requestType) {
					case PRECACHE_TO_DISK:
					case PRECACHE_TO_DISK_FOR_ADAPTER:
					case DEPRIORITIZED_PRECACHE_TO_DISK_FOR_ADAPTER:
						diskCacheRequestsToReportSuccess.add(imageCacherListener);
						return;
					case PRECACHE_TO_MEMORY:
					case PRECACHE_TO_MEMORY_FOR_ADAPTER:
					case ADAPTER_REQUEST:
					case DEPRIORITIZED:
					case DEPRIORITIZED_PRECACHE_TO_MEMORY_FOR_ADAPTER:
					case DEFAULT:
						CacheRequest cacheRequest = requestParameters.cacheRequest;
						int sampleSize = mObserver.getSampleSize(cacheRequest);
						DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, requestParameters.cacheRequest.getOptions().preferedConfig);

						registerDecodeRequest(cacheRequest, decodeSignature, imageCacherListener, requestParameters.imageReturnedFrom);
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
			for (RequestParameters requestParameters : list) {
				requestParameters.imageCacherListener.onFailure(message);
			}
		}
	}

	public void onDecodeSuccess(Bitmap bitmap, ImageReturnedFrom returnedFrom, DecodeSignature decodeSignature) {
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

	private final RequestObserver mNetworkRequestObserver = new RequestObserver() {
		@Override
		public void onRequestsCancelled(List<DefaultPrioritizable> cancelledPrioritizables) {
			cancelNetworkRequestsFromTracker(cancelledPrioritizables);
		}
	};

	private final RequestObserver mDiskRequestObserver = new RequestObserver() {
		@Override
		public void onRequestsCancelled(List<DefaultPrioritizable> cancelledPrioritizables) {
			cancelDiskRequestsFromTracker(cancelledPrioritizables);
		}
	};

	private void cancelNetworkRequestsFromTracker(List<DefaultPrioritizable> cancelledPrioritizables) {
		for (final DefaultPrioritizable cancelledPrioritizable : cancelledPrioritizables) {
			mNetworkExecutor.cancel(cancelledPrioritizable);
			String cancelledRequest = (String) cancelledPrioritizable.getRequest().getData();
			mNetworkOperationTracker.removeRequest(cancelledRequest, new ValueMatcher<AsyncOperationsMaps.RequestParameters>() {
				@Override
				public boolean shouldRemoveValue(RequestParameters value) {
					Prioritizable p = value.prioritizable;
					if (p == cancelledPrioritizable)
						return true;
					else
						return false;
				}
			}, mNetworkAndDetailsKeyReferenceProvider);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cancelDiskRequestsFromTracker(List<DefaultPrioritizable> cancelledPrioritizables) {
		for (final DefaultPrioritizable cancelledPrioritizable : cancelledPrioritizables) {
			mDiskExecutor.cancel(cancelledPrioritizable);
			Object cancelledRequest = cancelledPrioritizable.getRequest().getData();
			OperationTracker tracker;
			KeyReferenceProvider keyReferenceProvider;
			if (cancelledRequest instanceof DecodeSignature) {
				tracker = mDecodeOperationTracker;
				keyReferenceProvider = mDecodeReferenceProvider;
			} else {
				tracker = mDetailsOperationTracker;
				keyReferenceProvider = mNetworkAndDetailsKeyReferenceProvider;
			}
			tracker.removeRequest(cancelledRequest, new ValueMatcher<AsyncOperationsMaps.RequestParameters>() {
				@Override
				public boolean shouldRemoveValue(RequestParameters value) {
					Prioritizable p = value.prioritizable;
					if (p == cancelledPrioritizable)
						return true;
					else
						return false;
				}
			}, keyReferenceProvider);
		}
	}

	// FIXME This operation is low performance. Need to see if this can be sped up.
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

	private synchronized void cancelNetworkPrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mNetworkOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, true);
		if (requestParameters != null) {
			mNetworkExecutor.cancel(requestParameters.prioritizable);

			// We want to re-schedule cancelled adapter requests.
			CacheRequest cacheRequest = requestParameters.cacheRequest;
			if (cacheRequest.getImageRequestType() == ImageRequestType.ADAPTER_REQUEST) {
				cacheRequest.setImageRequestType(ImageRequestType.DEPRIORITIZED);
				mNetworkExecutor.execute(mObserver.getNetworkRunnable(requestParameters.cacheRequest));
			}
		}
	}

	private void cancelDetailsPrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mDetailsOperationTracker.removeRequest(imageCacherListener, mNetworkAndDetailsKeyReferenceProvider, true);
		if (requestParameters != null) {
			mDiskExecutor.cancel(requestParameters.prioritizable);

			// We want to re-schedule cancelled adapter requests.
			CacheRequest cacheRequest = requestParameters.cacheRequest;
			if (cacheRequest.getImageRequestType() == ImageRequestType.ADAPTER_REQUEST) {
				cacheRequest.setImageRequestType(ImageRequestType.DEPRIORITIZED);
				mDiskExecutor.execute(mObserver.getDetailsRunnable(requestParameters.cacheRequest));
			}
		}
	}

	private void cancelDecodePrioritizable(ImageCacherListener imageCacherListener) {
		RequestParameters requestParameters = mDecodeOperationTracker.removeRequest(imageCacherListener, mDecodeReferenceProvider, true);
		if (requestParameters != null) {
			mDiskExecutor.cancel(requestParameters.prioritizable);

			// We want to re-schedule cancelled adapter requests.
			CacheRequest cacheRequest = requestParameters.cacheRequest;
			if (cacheRequest.getImageRequestType() == ImageRequestType.ADAPTER_REQUEST) {
				cacheRequest.setImageRequestType(ImageRequestType.DEPRIORITIZED);
				mDiskExecutor.execute(mObserver.getDecodeRunnable(requestParameters.cacheRequest, requestParameters.decodeSignature, requestParameters.imageReturnedFrom));
			}
		}
	}

	private synchronized DecodeSignature getDecodeSignature(CacheRequest cacheRequest) {
		int sampleSize = mObserver.getSampleSize(cacheRequest);
		return new DecodeSignature(cacheRequest.getUri(), sampleSize, cacheRequest.getOptions().preferedConfig);
	}

	static interface OperationsObserver {
		public Prioritizable getNetworkRunnable(CacheRequest cacheRequest);

		public Prioritizable getDecodeRunnable(CacheRequest cacheRequest, DecodeSignature decodeSignature, ImageReturnedFrom imageReturnedFrom);

		public Prioritizable getDetailsRunnable(CacheRequest cacheRequest);

		public int getSampleSize(CacheRequest cacheRequest);
	}

	private class RequestParameters {
		final ImageCacherListener imageCacherListener;
		final CacheRequest cacheRequest;
		final Prioritizable prioritizable;
		final ImageReturnedFrom imageReturnedFrom;
		DecodeSignature decodeSignature;

		RequestParameters(ImageCacherListener imageCacherListener, CacheRequest imageRequest, Prioritizable prioritizable, ImageReturnedFrom imageReturnedFrom) {
			this.imageCacherListener = imageCacherListener;
			this.cacheRequest = imageRequest;
			this.prioritizable = prioritizable;
			this.imageReturnedFrom = imageReturnedFrom;
		}
	}
}
