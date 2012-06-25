package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.graphics.Bitmap;
import android.util.Log;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;

class AsyncOperationsMaps {
	private static final String TAG = "AsyncOperationsMap";
	
	public enum AsyncOperationState {
		QUEUED_FOR_NETWORK_REQUEST, QUEUED_FOR_DECODE_REQUEST, NOT_QUEUED
	}

	private HashMap<String, List<NetworkRequestParameters>> mUrlToListenersMapForNetwork = new HashMap<String, List<NetworkRequestParameters>>();
	private HashMap<ImageCacherListener, String> mListenerToUrlMapForNetwork = new HashMap<ImageCacher.ImageCacherListener, String>();

	private HashMap<DecodeOperationParameters, List<ImageCacherListener>> mDecodeParamsToListenersMap = new HashMap<DecodeOperationParameters, List<ImageCacherListener>>();
	private HashMap<ImageCacherListener, DecodeOperationParameters> mListenerToDiskParamsMap = new HashMap<ImageCacherListener, DecodeOperationParameters>();

	private AsyncOperationsObserver mAsyncOperationsObserver;
	
	public AsyncOperationsMaps(AsyncOperationsObserver asyncOperationsObserver) {
		mAsyncOperationsObserver = asyncOperationsObserver;
	}
	
	public synchronized boolean isNetworkRequestPendingForUrl(String url) {
		return mUrlToListenersMapForNetwork.containsKey(url);
	}
	
	public synchronized AsyncOperationState queueListenerIfRequestPending(ImageCacherListener imageCacherListener, String url, ScalingInfo scalingInfo) {
		if (isNetworkRequestPendingForUrl(url)) {
			addToQueueForNetworkRequest(imageCacherListener, url, scalingInfo);
			return AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		}

		try {
			int sampleSize = mAsyncOperationsObserver.getSampleSize(url, scalingInfo);
			DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);
			if (isDecodeRequestPendingForParams(decodeOperationParameters)) {
				queueForDecodeRequest(imageCacherListener, decodeOperationParameters);
				return AsyncOperationState.QUEUED_FOR_DECODE_REQUEST;
			}
		} catch (FileNotFoundException e) {
		}
		return AsyncOperationState.NOT_QUEUED;
	}

	public synchronized void registerListenerForNetworkRequest(ImageCacherListener imageCacherListener, String url, ScalingInfo scalingInfo) {
		List<NetworkRequestParameters> networkRequestParametersList = mUrlToListenersMapForNetwork.get(url);
		if (networkRequestParametersList == null) {
			networkRequestParametersList = new ArrayList<NetworkRequestParameters>();
			mUrlToListenersMapForNetwork.put(url, networkRequestParametersList);
		}
		NetworkRequestParameters networkRequestParameters = new NetworkRequestParameters(imageCacherListener, scalingInfo);
		networkRequestParametersList.add(networkRequestParameters);

		mListenerToUrlMapForNetwork.put(imageCacherListener, url);
	}

	public synchronized void registerListenerForDecode(ImageCacherListener imageCacherListener, String url, int sampleSize) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);
		List<ImageCacherListener> imageCacherListenerList = mDecodeParamsToListenersMap.get(decodeOperationParameters);
		if (imageCacherListenerList == null) {
			imageCacherListenerList = new ArrayList<ImageCacherListener>();
			mDecodeParamsToListenersMap.put(decodeOperationParameters, imageCacherListenerList);
		}
		imageCacherListenerList.add(imageCacherListener);

		mListenerToDiskParamsMap.put(imageCacherListener, decodeOperationParameters);
	}

	public void onDecodeSuccess(Bitmap bitmap, String url, int sampleSize) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);

		ImageCacherListener imageCacherListener;
		while ((imageCacherListener = getListenerWaitingOnDecode(decodeOperationParameters)) != null) {
			synchronized (imageCacherListener) {
				if (removeQueuedListenerForDecode(decodeOperationParameters, imageCacherListener)) {
					imageCacherListener.onImageAvailable(bitmap);
				}
			}
		}
	}
	
	public void onDecodeFailed(String url, int sampleSize) {
		DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);

		ImageCacherListener imageCacherListener;
		while ((imageCacherListener = getListenerWaitingOnDecode(decodeOperationParameters)) != null) {
			synchronized (imageCacherListener) {
				if (removeQueuedListenerForDecode(decodeOperationParameters, imageCacherListener)) {
					imageCacherListener.onFailure("Disk decode failed.");
				}
			}
		}
	}

	public void onDownloadComplete(String url) {
		HashSet<DecodeOperationParameters> decodeRequestsToMake = moveNetworkListenersToDiskQueue(url);
		if (decodeRequestsToMake != null) {
			for (DecodeOperationParameters decodeOperationParameters : decodeRequestsToMake) {
				mAsyncOperationsObserver.onImageDecodeRequired(decodeOperationParameters.mUrl, decodeOperationParameters.mSampleSize);
			}
		}
	}
	
	public void onDownloadFailed(String url) {
		NetworkRequestParameters networkRequestParameters;
		while ((networkRequestParameters = getListenerWaitingOnDownload(url)) != null) {
			synchronized (networkRequestParameters.mImageCacherListener) {
				if (removeQueuedListenerForDownload(networkRequestParameters)) {
					networkRequestParameters.mImageCacherListener.onFailure("Failed to download image.");
				}
			}
		}
	}
	
	public synchronized void cancelPendingRequest(ImageCacherListener imageCacherListener) {
		String url = mListenerToUrlMapForNetwork.remove(imageCacherListener);
		if (url != null) {
			Log.d(TAG, "1");
			List<NetworkRequestParameters> networkRequestParametersList = mUrlToListenersMapForNetwork.get(url);
			Log.d(TAG, "2");
			if (networkRequestParametersList != null) {
				Log.d(TAG, "3");
				networkRequestParametersList.remove(imageCacherListener);
				Log.d(TAG, "4");
				if (networkRequestParametersList.isEmpty()) {
					Log.d(TAG, "5");
					mAsyncOperationsObserver.cancelNetworkRequest(url);
					
					mUrlToListenersMapForNetwork.remove(url);
				}
			}
		}
		
		DecodeOperationParameters decodeOperationParameters = mListenerToDiskParamsMap.remove(imageCacherListener);
		if (decodeOperationParameters != null) {
			List<ImageCacherListener> imageCacherListenersList = mDecodeParamsToListenersMap.get(decodeOperationParameters);
			if (imageCacherListenersList != null) {
				imageCacherListenersList.remove(imageCacherListener);
				if (imageCacherListenersList.size() == 0) {
					// TODO: Cancel the decode operation here by calling the observer.
					
					mDecodeParamsToListenersMap.remove(decodeOperationParameters);
				}
			}
		}
	}

	private synchronized HashSet<DecodeOperationParameters> moveNetworkListenersToDiskQueue(String url) {
		List<NetworkRequestParameters> networkRequestParametersList = mUrlToListenersMapForNetwork.remove(url);
		if (networkRequestParametersList != null) {
			HashSet<DecodeOperationParameters> diskRequestsToMake = new HashSet<DecodeOperationParameters>();
			
			for (NetworkRequestParameters networkRequestParameters : networkRequestParametersList) {
				mListenerToUrlMapForNetwork.remove(networkRequestParameters);

				try {
					int sampleSize;
					sampleSize = mAsyncOperationsObserver.getSampleSize(url, networkRequestParameters.mScalingInfo);
					DecodeOperationParameters decodeOperationParameters = new DecodeOperationParameters(url, sampleSize);
					queueForDecodeRequest(networkRequestParameters.mImageCacherListener, decodeOperationParameters);
					diskRequestsToMake.add(decodeOperationParameters);
				} catch (FileNotFoundException e) {
					// TODO: Handle error condition.
				}
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
	 * @return
	 */
	private synchronized boolean removeQueuedListenerForDecode(DecodeOperationParameters decodeOperationParameters, ImageCacherListener imageCacherListener) {
		List<ImageCacherListener> imageCacherListeners = mDecodeParamsToListenersMap.get(decodeOperationParameters);
		if (imageCacherListeners != null) {
			imageCacherListeners.remove(imageCacherListener);
			if (imageCacherListeners.size() == 0) {
				mDecodeParamsToListenersMap.remove(decodeOperationParameters);
			}
			
			mListenerToDiskParamsMap.remove(imageCacherListener);
			return true;
		}
		return false;
	}
	
	private synchronized boolean removeQueuedListenerForDownload(NetworkRequestParameters networkRequestParameters) {
		String url = mListenerToUrlMapForNetwork.remove(networkRequestParameters);
		if (url != null) {
			List<NetworkRequestParameters> networkRequestParametersList = mUrlToListenersMapForNetwork.get(url);
			if (networkRequestParametersList != null) {
				boolean result = networkRequestParametersList.remove(networkRequestParameters);
				if (networkRequestParametersList.size() == 0) {
					mUrlToListenersMapForNetwork.remove(url);
				}
				return result;
			}
		}
		return false;
	}

	private ImageCacherListener getListenerWaitingOnDecode(DecodeOperationParameters decodeOperationParameters) {
		List<ImageCacherListener> imageCacherListeners = mDecodeParamsToListenersMap.get(decodeOperationParameters);
		if (imageCacherListeners != null && imageCacherListeners.size() > 0) {
			return imageCacherListeners.get(0);
		}
		return null;
	}
	
	private NetworkRequestParameters getListenerWaitingOnDownload(String url) {
		List<NetworkRequestParameters> networkRequestParametersList = mUrlToListenersMapForNetwork.get(url);
		if (networkRequestParametersList != null && networkRequestParametersList.size() > 0) {
			return networkRequestParametersList.get(0);
		}
		return null;
	}

	private void queueForDecodeRequest(ImageCacherListener imageCacherListener, DecodeOperationParameters decodeOperationParameters) {
		List<ImageCacherListener> imageCacherListenerList = mDecodeParamsToListenersMap.get(decodeOperationParameters);
		if (imageCacherListenerList == null) {
			imageCacherListenerList = new ArrayList<ImageCacherListener>();
			mDecodeParamsToListenersMap.put(decodeOperationParameters, imageCacherListenerList);
		}
		imageCacherListenerList.add(imageCacherListener);

		mListenerToDiskParamsMap.put(imageCacherListener, decodeOperationParameters);
	}

	private boolean isDecodeRequestPendingForParams(DecodeOperationParameters decodeOperationParameters) {
		return mDecodeParamsToListenersMap.containsKey(decodeOperationParameters);
	}

	private void addToQueueForNetworkRequest(ImageCacherListener imageCacherListener, String url, ScalingInfo scalingInfo) {
		NetworkRequestParameters params = new NetworkRequestParameters(imageCacherListener, scalingInfo);
		mUrlToListenersMapForNetwork.get(url).add(params);
		mListenerToUrlMapForNetwork.put(imageCacherListener, url);
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

	private class DecodeOperationParameters {
		String mUrl;
		int mSampleSize;

		DecodeOperationParameters(String url, int sampleSize) {
			mUrl = url;
			mSampleSize = sampleSize;
		}

		@Override
		public int hashCode() {
			int hash;
			hash = 31 * mSampleSize + 17;
			hash += mUrl.hashCode();
			return hash;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DecodeOperationParameters)) {
				return false;
			}

			DecodeOperationParameters otherObject = (DecodeOperationParameters) o;
			if (otherObject.mSampleSize == mSampleSize && otherObject.mUrl.equals(mUrl)) {
				return true;
			}
			return false;
		}
	}
}
