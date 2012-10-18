/*
 * Copyright 2012 Xtreme Labs
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

public class AsyncOperationsMaps {
	private static final String PREFIX = "MAPS - ";

	public enum AsyncOperationState {
		QUEUED_FOR_NETWORK_REQUEST, QUEUED_FOR_DECODE_REQUEST, NOT_QUEUED
	}

	private final HashMap<RequestIdentifier, List<NetworkRequestParameters>> mRequestIdentifierToListenersMapForNetwork = new HashMap<RequestIdentifier, List<NetworkRequestParameters>>();
	private final HashMap<ImageCacherListener, RequestIdentifier> mListenerToRequestIdentifierMapForNetwork = new HashMap<ImageCacher.ImageCacherListener, RequestIdentifier>();

	private final HashMap<DecodeOperationParameters, List<ImageCacherListener>> mDecodeParamsToListenersMap = new HashMap<DecodeOperationParameters, List<ImageCacherListener>>();
	private final HashMap<ImageCacherListener, DecodeOperationParameters> mListenerToDecodeParamsMap = new HashMap<ImageCacherListener, DecodeOperationParameters>();

	private final AsyncOperationsObserver mAsyncOperationsObserver;

	public AsyncOperationsMaps(AsyncOperationsObserver asyncOperationsObserver) {
		mAsyncOperationsObserver = asyncOperationsObserver;
	}

	public synchronized boolean isNetworkRequestPendingForUrl(RequestIdentifier requestIdentifier) {
		return mRequestIdentifierToListenersMapForNetwork.containsKey(requestIdentifier);
	}

	public synchronized boolean isDecodeRequestPendingForUrlAndScalingInfo(RequestIdentifier requestIdentifier, ScalingInfo scalingInfo) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, mAsyncOperationsObserver.getSampleSize(requestIdentifier, scalingInfo));
		return mDecodeParamsToListenersMap.containsKey(decodeOperationParameters);
	}

	public synchronized AsyncOperationState queueListenerIfRequestPending(RequestIdentifier requestIdentifier, ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
		if (isNetworkRequestPendingForUrl(requestIdentifier)) {
			registerListenerForNetworkRequest(imageCacherListener, requestIdentifier, scalingInfo);
			return AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		}

		int sampleSize = mAsyncOperationsObserver.getSampleSize(requestIdentifier, scalingInfo);
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, sampleSize);
		if (isDecodeRequestPendingForParams(decodeOperationParameters)) {
			queueForDecodeRequest(imageCacherListener, decodeOperationParameters);
			return AsyncOperationState.QUEUED_FOR_DECODE_REQUEST;
		}

		return AsyncOperationState.NOT_QUEUED;
	}

	public synchronized void registerListenerForNetworkRequest(ImageCacherListener imageCacherListener, RequestIdentifier requestIdentifier, ScalingInfo scalingInfo) {
		if (Logger.logMaps()) {
			Logger.d(PREFIX + "Registering listener for URL: " + requestIdentifier);
		}

		NetworkRequestParameters networkRequestParameters = new NetworkRequestParameters(imageCacherListener, scalingInfo);

		List<NetworkRequestParameters> networkRequestParametersList = mRequestIdentifierToListenersMapForNetwork.get(requestIdentifier);
		if (networkRequestParametersList == null) {
			networkRequestParametersList = new ArrayList<NetworkRequestParameters>();
			mRequestIdentifierToListenersMapForNetwork.put(requestIdentifier, networkRequestParametersList);
		}
		networkRequestParametersList.add(networkRequestParameters);

		mListenerToRequestIdentifierMapForNetwork.put(imageCacherListener, requestIdentifier);
	}

	public synchronized void registerListenerForDecode(ImageCacherListener imageCacherListener, RequestIdentifier requestIdentifier, int sampleSize) {
		if (Logger.logMaps()) {
			Logger.d(PREFIX + "Registering listener for decode: " + requestIdentifier);
		}

		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, sampleSize);
		queueForDecodeRequest(imageCacherListener, decodeOperationParameters);
	}

	public void onDecodeSuccess(Bitmap bitmap, RequestIdentifier requestIdentifier, int sampleSize, ImageReturnedFrom returnedFrom) {
		if (Logger.logMaps()) {
			Logger.d(PREFIX + "Image decoded: " + requestIdentifier + ", sample size: " + sampleSize);
		}

		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, sampleSize);

		ImageCacherListener imageCacherListener;
		while ((imageCacherListener = getListenerWaitingOnDecode(decodeOperationParameters)) != null) {
			synchronized (imageCacherListener) {
				if (removeQueuedListenerForDecode(decodeOperationParameters, imageCacherListener, true)) {
					imageCacherListener.onImageAvailable(bitmap, returnedFrom);
				}
			}
		}
	}

	public void onDecodeFailed(RequestIdentifier requestIdentifier, int sampleSize, String message) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, sampleSize);

		ImageCacherListener imageCacherListener;
		while ((imageCacherListener = getListenerWaitingOnDecode(decodeOperationParameters)) != null) {
			synchronized (imageCacherListener) {
				if (removeQueuedListenerForDecode(decodeOperationParameters, imageCacherListener, true)) {
					imageCacherListener.onFailure(message);
				}
			}
		}
	}

	public void onDownloadComplete(RequestIdentifier requestIdentifier) {
		HashSet<DecodeOperationParameters> decodeRequestsToMake = moveNetworkListenersToDiskQueue(requestIdentifier);
		if (decodeRequestsToMake != null) {
			for (DecodeOperationParameters decodeOperationParameters : decodeRequestsToMake) {
				mAsyncOperationsObserver.onImageDecodeRequired(decodeOperationParameters.mRequestIdentifier, decodeOperationParameters.mSampleSize);
			}
		}
	}

	public void onDownloadFailed(RequestIdentifier requestIdentifier, String message) {
		NetworkRequestParameters networkRequestParameters;
		while ((networkRequestParameters = getListenerWaitingOnDownload(requestIdentifier)) != null) {
			synchronized (networkRequestParameters.mImageCacherListener) {
				if (removeQueuedListenerForDownload(networkRequestParameters, true)) {
					networkRequestParameters.mImageCacherListener.onFailure(message);
				}
			}
		}
	}

	public void cancelPendingRequest(ImageCacherListener imageCacherListener) {
		NetworkRequestParameters targetParameters = null;
		synchronized (this) {
			if (Logger.logMaps()) {
				Logger.d(PREFIX + "Cancelling a request.");
			}

			RequestIdentifier requestIdentifier = mListenerToRequestIdentifierMapForNetwork.get(imageCacherListener);
			if (requestIdentifier != null) {
				List<NetworkRequestParameters> parametersList = mRequestIdentifierToListenersMapForNetwork.get(requestIdentifier);
				if (parametersList != null) {
					for (NetworkRequestParameters networkRequestParameters : parametersList) {
						if (networkRequestParameters.mImageCacherListener == imageCacherListener) {
							targetParameters = networkRequestParameters;
							break;
						}
					}
				}
			}
		}

		if (targetParameters != null) {
			synchronized (targetParameters.mImageCacherListener) {
				if (!removeQueuedListenerForDownload(targetParameters, false)) {
					if (Logger.logMaps()) {
						Logger.w(PREFIX + "Was unable to remove the request!");
					}
				}
			}
			return;
		}

		DecodeOperationParameters decodeOperationParameters = null;
		synchronized (this) {
			decodeOperationParameters = mListenerToDecodeParamsMap.get(imageCacherListener);
		}

		if (decodeOperationParameters != null) {
			synchronized (imageCacherListener) {
				removeQueuedListenerForDecode(decodeOperationParameters, imageCacherListener, false);
			}
		}
	}

	public synchronized int getNumPendingDownloads() {
		return mRequestIdentifierToListenersMapForNetwork.size();
	}

	public synchronized int getNumPendingDecodes() {
		return mDecodeParamsToListenersMap.size();
	}

	public synchronized int getNumListenersForNetwork() {
		return mListenerToRequestIdentifierMapForNetwork.size();
	}

	public synchronized int getNumListenersForDecode() {
		return mListenerToDecodeParamsMap.size();
	}

	public synchronized boolean isListenerWaitingOnNetwork(ImageCacherListener imageCacherListener) {
		return mListenerToRequestIdentifierMapForNetwork.containsKey(imageCacherListener);
	}

	public synchronized boolean isListenerWaitingOnDecode(ImageCacherListener imageCacherListener) {
		return mListenerToDecodeParamsMap.containsKey(imageCacherListener);
	}

	public synchronized boolean areMapsEmpty() {
		return mRequestIdentifierToListenersMapForNetwork.size() == 0 && mListenerToRequestIdentifierMapForNetwork.size() == 0 && mDecodeParamsToListenersMap.size() == 0 && mListenerToDecodeParamsMap.size() == 0;
	}

	private synchronized void queueForDecodeRequest(ImageCacherListener imageCacherListener, DecodeOperationParameters decodeOperationParameters) {
		List<ImageCacherListener> imageCacherListenerList = mDecodeParamsToListenersMap.get(decodeOperationParameters);
		if (imageCacherListenerList == null) {
			imageCacherListenerList = new ArrayList<ImageCacherListener>();
			mDecodeParamsToListenersMap.put(decodeOperationParameters, imageCacherListenerList);
		}
		imageCacherListenerList.add(imageCacherListener);

		mListenerToDecodeParamsMap.put(imageCacherListener, decodeOperationParameters);
	}

	private synchronized HashSet<DecodeOperationParameters> moveNetworkListenersToDiskQueue(RequestIdentifier requestIdentifier) {
		List<NetworkRequestParameters> networkRequestParametersList = mRequestIdentifierToListenersMapForNetwork.remove(requestIdentifier);
		if (networkRequestParametersList != null) {
			HashSet<DecodeOperationParameters> diskRequestsToMake = new HashSet<DecodeOperationParameters>();

			for (NetworkRequestParameters networkRequestParameters : networkRequestParametersList) {
				mListenerToRequestIdentifierMapForNetwork.remove(networkRequestParameters.mImageCacherListener);

				int sampleSize;
				sampleSize = mAsyncOperationsObserver.getSampleSize(requestIdentifier, networkRequestParameters.mScalingInfo);
				DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(requestIdentifier, sampleSize);
				queueForDecodeRequest(networkRequestParameters.mImageCacherListener, decodeOperationParameters);
				diskRequestsToMake.add(decodeOperationParameters);
			}

			return diskRequestsToMake;
		}
		return null;
	}

	/**
	 * You must be synchronized on the ImageCacherListener that is being passed in before calling this method.
	 * 
	 * @param decodeOperationParameters
	 * @param imageCacherListener
	 * @param deleteMapIfEmpty
	 * @return
	 */
	private synchronized boolean removeQueuedListenerForDecode(DecodeOperationParameters decodeOperationParameters, ImageCacherListener imageCacherListener, boolean deleteMapIfEmpty) {
		List<ImageCacherListener> imageCacherListeners = mDecodeParamsToListenersMap.get(decodeOperationParameters);
		if (imageCacherListeners != null) {
			imageCacherListeners.remove(imageCacherListener);
			if (deleteMapIfEmpty && imageCacherListeners.size() == 0) {
				if (mDecodeParamsToListenersMap.remove(decodeOperationParameters) == null) {
					if (Logger.logMaps()) {
						Logger.w(PREFIX + "Did not remove entry from the decode map.");
					}
				}
			}

			mListenerToDecodeParamsMap.remove(imageCacherListener);
			return true;
		}
		return false;
	}

	private synchronized boolean removeQueuedListenerForDownload(NetworkRequestParameters networkRequestParameters, boolean deleteMapIfEmpty) {
		RequestIdentifier requestIdentifier = mListenerToRequestIdentifierMapForNetwork.remove(networkRequestParameters.mImageCacherListener);
		if (requestIdentifier != null) {
			List<NetworkRequestParameters> networkRequestParametersList = mRequestIdentifierToListenersMapForNetwork.get(requestIdentifier);
			if (networkRequestParametersList != null) {
				boolean result = networkRequestParametersList.remove(networkRequestParameters);
				if (deleteMapIfEmpty && networkRequestParametersList.size() == 0) {
					mRequestIdentifierToListenersMapForNetwork.remove(requestIdentifier);
				}
				return result;
			} else {
				if (Logger.logMaps()) {
					Logger.i(PREFIX + "No list was available for the URL.");
				}
			}
		} else {
			if (Logger.logMaps()) {
				Logger.i(PREFIX + "URL was null when trying to remove a listener.");
			}
		}
		return false;
	}

	private synchronized ImageCacherListener getListenerWaitingOnDecode(DecodeOperationParameters decodeOperationParameters) {
		List<ImageCacherListener> imageCacherListeners = mDecodeParamsToListenersMap.get(decodeOperationParameters);

		if (imageCacherListeners == null || imageCacherListeners.size() == 0) {
			mDecodeParamsToListenersMap.remove(decodeOperationParameters);
		}

		if (imageCacherListeners != null && imageCacherListeners.size() > 0) {
			return imageCacherListeners.get(0);
		}
		return null;
	}

	private synchronized NetworkRequestParameters getListenerWaitingOnDownload(RequestIdentifier requestIdentifier) {
		List<NetworkRequestParameters> networkRequestParametersList = mRequestIdentifierToListenersMapForNetwork.get(requestIdentifier);
		if (networkRequestParametersList == null || networkRequestParametersList.size() == 0) {
			mRequestIdentifierToListenersMapForNetwork.remove(requestIdentifier);
			return null;
		}

		if (networkRequestParametersList != null && networkRequestParametersList.size() > 0) {
			return networkRequestParametersList.get(0);
		}
		return null;
	}

	private synchronized boolean isDecodeRequestPendingForParams(DecodeOperationParameters decodeOperationParameters) {
		return mDecodeParamsToListenersMap.containsKey(decodeOperationParameters);
	}

	private class NetworkRequestParameters {
		ImageCacherListener mImageCacherListener;
		ScalingInfo mScalingInfo;

		NetworkRequestParameters(ImageCacherListener imageCacherListener, ScalingInfo scalingInfo) {
			mImageCacherListener = imageCacherListener;
			mScalingInfo = scalingInfo;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}

			if (!(o instanceof NetworkRequestParameters)) {
				return false;
			}

			NetworkRequestParameters params = (NetworkRequestParameters) o;
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
