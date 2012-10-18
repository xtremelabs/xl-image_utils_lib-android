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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final NetworkToDiskInterface mNetworkToDiskInterface;
	private final ImageDownloadObserver mImageDownloadObserver;
	private final HashMap<RequestIdentifier, ImageDownloadingRunnable> mRequestIdentifierToRunnableMap = new HashMap<RequestIdentifier, ImageDownloadingRunnable>();

	/*
	 * TODO: Research into lowering the number of available threads for the network
	 */
	private final LifoThreadPool mThreadPool = new LifoThreadPool(3);

	public ImageDownloader(NetworkToDiskInterface networkToDiskInterface, ImageDownloadObserver imageDownloadObserver) {
		mNetworkToDiskInterface = networkToDiskInterface;
		mImageDownloadObserver = imageDownloadObserver;
	}

	@Override
	public synchronized void bump(RequestIdentifier requestIdentifier) {
		ImageDownloadingRunnable runnable = mRequestIdentifierToRunnableMap.get(requestIdentifier);
		if (runnable != null) {
			mThreadPool.bump(runnable);
		}
	}

	@Override
	public synchronized void downloadImageToDisk(final RequestIdentifier requestIdentifier) {
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(requestIdentifier);
		if (!mRequestIdentifierToRunnableMap.containsKey(requestIdentifier)) {
			mRequestIdentifierToRunnableMap.put(requestIdentifier, runnable);
			mThreadPool.execute(runnable);
		}
	}

	private synchronized void removeUrlFromMap(RequestIdentifier requestIdentifier) {
		mRequestIdentifierToRunnableMap.remove(requestIdentifier);
	}

	class ImageDownloadingRunnable implements Runnable {
		private final RequestIdentifier mRequestIdentifier;
		private boolean mFailed = false;
		private InputStream mInputStream = null;
		private HttpEntity mEntity;

		public ImageDownloadingRunnable(RequestIdentifier requestIdentifier) {
			mRequestIdentifier = requestIdentifier;
		}

		@Override
		public void run() {
			String errorMessage = null;
			try {
				executeNetworkRequest();
				passInputStreamToImageLoader();
			} catch (IOException e) {
				mFailed = true;
				errorMessage = "Failed to download image with error message: " + e.getMessage();
			} catch (IllegalArgumentException e) {
				mFailed = true;
				errorMessage = "Failed to download image with error message: " + e.getMessage();
			} finally {
				try {
					if (mEntity != null) {
						mEntity.consumeContent();
					}
				} catch (IOException e) {
				}
				try {
					if (mInputStream != null) {
						mInputStream.close();
					}
				} catch (IOException e) {
				}
			}
			checkLoadCompleteAndRemoveListeners(errorMessage);
		}

		private synchronized void checkLoadCompleteAndRemoveListeners(String errorMessage) {
			removeUrlFromMap(mRequestIdentifier);
			if (mFailed) {
				assert (errorMessage != null);
				mImageDownloadObserver.onImageDownloadFailed(mRequestIdentifier, errorMessage);
			} else {
				mImageDownloadObserver.onImageDownloaded(mRequestIdentifier);
			}
		}

		public synchronized void executeNetworkRequest() throws ClientProtocolException, IOException {
			HttpClient client = new DefaultHttpClient();
			client.getConnectionManager().closeExpiredConnections();
			HttpUriRequest request = new HttpGet(mRequestIdentifier.getUrlOrFilename());
			HttpResponse response = client.execute(request);
			mEntity = response.getEntity();
			if (mEntity != null) {
				mInputStream = new BufferedInputStream(mEntity.getContent());
			}
			if (mEntity == null || mInputStream == null) {
				mFailed = true;
			}
			client.getConnectionManager().closeExpiredConnections();
		}

		public void passInputStreamToImageLoader() throws IOException {
			if (mInputStream != null) {
				mNetworkToDiskInterface.downloadImageFromInputStream(mRequestIdentifier, mInputStream);
			}
		}
	}

	@Override
	public synchronized boolean isNetworkRequestPendingForUrl(RequestIdentifier requestIdentifier) {
		return mRequestIdentifierToRunnableMap.containsKey(requestIdentifier);
	}
}
