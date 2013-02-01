/*
 * Copyright 2013 Xtreme Labs
 * import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.RequestType;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.OperationTracker.KeyReferenceProvider;
import com.xtremelabs.imageutils.OperationTracker.OperationTransferer;
ess or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Bitmap;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.RequestType;
import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.OperationTracker.KeyReferenceProvider;
import com.xtremelabs.imageutils.OperationTracker.OperationTransferer;

public class AsyncOperationsMaps {
	public enum AsyncOperationState {
		QUEUED_FOR_NETWORK_REQUEST, QUEUED_FOR_DETAILS_REQUEST, QUEUED_FOR_DECODE_REQUEST, NOT_QUEUED
	}

	private final OperationTracker<String, RequestParameters, ImageCacherListener> mNetworkOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<String, RequestParameters, ImageCacherListener> mDetailsOperationTracker = new OperationTracker<String, RequestParameters, ImageCacherListener>();
	private final OperationTracker<DecodeSignature, ImageCacherListener, ImageCacherListener> mDecodeOperationTracker = new OperationTracker<DecodeSignature, ImageCacherListener, ImageCacherListener>();

	private final AsyncOperationsObserver mAsyncOperationsObserver;

	private final KeyReferenceProvider<String, RequestParameters, ImageCacherListener> mNetworkAndDetailsKeyReferenceProvider = new KeyReferenceProvider<String, RequestParameters, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(String operationKey, RequestParameters operationListValue) {
			return operationListValue.mImageCacherListener;
		}
	};

	private final KeyReferenceProvider<DecodeSignature, ImageCacherListener, ImageCacherListener> mDecodeReferenceProvider = new KeyReferenceProvider<DecodeSignature, ImageCacherListener, ImageCacherListener>() {
		@Override
		public ImageCacherListener getKeyReference(DecodeSignature decodeSignature, ImageCacherListener imageCacherListener) {
			return imageCacherListener;
		}
	};

	public AsyncOperationsMaps(AsyncOperationsObserver asyncOperationsObserver) {
		mAsyncOperationsObserver = asyncOperationsObserver;
	}

	public synchronized boolean isNetworkRequestPending(String uri) {
		return mNetworkOperationTracker.hasPendingOperation(uri);
	}

	public synchronized boolean isDetailsRequestPending(String uri) {
		return mDetailsOperationTracker.hasPendingOperation(uri);
	}

	public synchronized boolean isDecodeRequestPending(String uri, ScalingInfo scalingInfo, Bitmap.Config bitmapConfig) {
		DecodeSignature decodeSignature = new DecodeSignature(uri, mAsyncOperationsObserver.getSampleSize(new ImageRequest(uri, scalingInfo)), bitmapConfig);
		return mDecodeOperationTracker.hasPendingOperation(decodeSignature);
	}

	public synchronized AsyncOperationState queueListenerIfRequestPending(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		String uri = imageRequest.getUri();
		Bitmap.Config bitmapConfig = imageRequest.getOptions().preferedConfig;
		if (isNetworkRequestPending(uri)) {
			registerListenerForNetworkRequest(imageRequest, imageCacherListener);
			return AsyncOperationState.QUEUED_FOR_NETWORK_REQUEST;
		}

		// FIXME We need to check for a details request here.

		int sampleSize = mAsyncOperationsObserver.getSampleSize(imageRequest);
		DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, bitmapConfig);
		if (isDecodeRequestPendingForParams(decodeSignature)) {
			queueForDecodeRequest(imageCacherListener, decodeSignature);
			return AsyncOperationState.QUEUED_FOR_DECODE_REQUEST;
		}

		return AsyncOperationState.NOT_QUEUED;
	}

	public synchronized void registerListenerForNetworkRequest(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, imageRequest);
		mNetworkOperationTracker.register(imageRequest.getUri(), networkRequestParameters, imageCacherListener);
	}

	// TODO Fix naming convention. The NetworkRequestParameter object is no longer specific to network requests.
	public void registerListenerForDetailsRequest(ImageRequest imageRequest, ImageCacherListener imageCacherListener) {
		RequestParameters networkRequestParameters = new RequestParameters(imageCacherListener, imageRequest);
		mDetailsOperationTracker.register(imageRequest.getUri(), networkRequestParameters, imageCacherListener);
	}

	// TODO Refactor all calls to queueForDecodeRequest to instead point to this method.
	public synchronized void registerListenerForDecode(DecodeSignature decodeSignature, ImageCacherListener imageCacherListener) {
		queueForDecodeRequest(imageCacherListener, decodeSignature);
	}

	public void onDecodeSuccess(Bitmap bitmap, ImageReturnedFrom returnedFrom, DecodeSignature decodeSignature) {
		List<ImageCacherListener> listeners = mDecodeOperationTracker.removeList(decodeSignature, mDecodeReferenceProvider);

		for (ImageCacherListener listener : listeners) {
			listener.onImageAvailable(new ImageResponse(bitmap, returnedFrom, ImageResponseStatus.SUCCESS));
		}
	}

	public void onDecodeFailed(DecodeSignature decodeSignature, String message) {
		List<ImageCacherListener> listeners = mDecodeOperationTracker.removeList(decodeSignature, mDecodeReferenceProvider);
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
		final Set<DecodeSignature> decodeRequestsToMake = new HashSet<DecodeSignature>();
		final List<ImageCacherListener> diskCacheRequestsToReportSuccess = new ArrayList<ImageCacherListener>();

		synchronized (this) {
			mDetailsOperationTracker.transferOperation(uri, new OperationTransferer<String, RequestParameters, ImageCacherListener>() {
				@Override
				public void transferOperation(String uri, RequestParameters networkRequestParameters, ImageCacherListener imageCacherListener) {
					RequestType requestType = networkRequestParameters.mImageRequest.getRequestType();

					switch (requestType) {
					case CACHE_TO_DISK:
						diskCacheRequestsToReportSuccess.add(imageCacherListener);
						return;
					case CACHE_TO_DISK_AND_MEMORY:
					case FULL_REQUEST:
						int sampleSize = mAsyncOperationsObserver.getSampleSize(new ImageRequest(uri, networkRequestParameters.mImageRequest.getScalingInfo()));
						DecodeSignature decodeSignature = new DecodeSignature(uri, sampleSize, networkRequestParameters.mImageRequest.getOptions().preferedConfig);

						queueForDecodeRequest(networkRequestParameters.mImageCacherListener, decodeSignature);
						decodeRequestsToMake.add(decodeSignature);
						break;
					}
				}
			}, mNetworkAndDetailsKeyReferenceProvider);
		}

		for (ImageCacherListener listener : diskCacheRequestsToReportSuccess) {
			listener.onImageAvailable(new ImageResponse(null, null, ImageResponseStatus.CACHED_ON_DISK));
		}

		for (DecodeSignature decodeSignature : decodeRequestsToMake) {
			mAsyncOperationsObserver.onImageDecodeRequired(decodeSignature);
		}
	}

	public void onDetailsRequestFailed(String uri, String message) {
		List<RequestParameters> list = mDetailsOperationTracker.removeList(uri, mNetworkAndDetailsKeyReferenceProvider);

		if (list != null) {
			for (RequestParameters networkRequestParameters : list) {
				networkRequestParameters.mImageCacherListener.onFailure(message);
			}
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

	private synchronized void queueForDecodeRequest(ImageCacherListener imageCacherListener, DecodeSignature decodeSignature) {
		mDecodeOperationTracker.register(decodeSignature, imageCacherListener, imageCacherListener);
	}

	private synchronized void moveNetworkListenersToDetailsQueue(String uri) {
		mNetworkOperationTracker.transferOperationToTracker(uri, mDetailsOperationTracker, mNetworkAndDetailsKeyReferenceProvider);
	}

	private synchronized boolean isDecodeRequestPendingForParams(DecodeSignature decodeSignature) {
		return mDecodeOperationTracker.hasPendingOperation(decodeSignature);
	}

	private class RequestParameters {
		ImageCacherListener mImageCacherListener;
		ImageRequest mImageRequest;

		RequestParameters(ImageCacherListener imageCacherListener, ImageRequest imageRequest) {
			mImageCacherListener = imageCacherListener;
			mImageRequest = imageRequest;
		}
	}
}
