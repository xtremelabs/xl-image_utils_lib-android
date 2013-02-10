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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.xtremelabs.imageutils.NetworkRequestCreator.InputStreamListener;

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final NetworkToDiskInterface mNetworkToDiskInterface;
	private final ImageDownloadObserver mImageDownloadObserver;
	private final HashMap<String, ImageDownloadingRunnable> mUrlToRunnableMap = new HashMap<String, ImageDownloadingRunnable>();
	private NetworkRequestCreator mNetworkRequestCreator = new DefaultNetworkRequestCreator();

	/*
	 * TODO: Research into lowering the number of available threads for the network
	 */
	// private final LifoThreadPool mThreadPool = new LifoThreadPool(3);
	private final AuxiliaryBlockingQueue mBlockingQueue = new AuxiliaryBlockingQueue(new PriorityAccessor[] { new StackPriorityAccessor() });
	private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(3, 3, 0, TimeUnit.MILLISECONDS, mBlockingQueue);

	public ImageDownloader(NetworkToDiskInterface networkToDiskInterface, ImageDownloadObserver imageDownloadObserver) {
		mNetworkToDiskInterface = networkToDiskInterface;
		mImageDownloadObserver = imageDownloadObserver;
	}

	@Override
	public synchronized void bump(String url) {
		ImageDownloadingRunnable runnable = mUrlToRunnableMap.get(url);
		if (runnable != null) {
			mBlockingQueue.bump(runnable);
		}
	}

	@Override
	public synchronized void downloadImageToDisk(final String url) {
		ImageDownloadingRunnable runnable = new ImageDownloadingRunnable(url);
		if (!mUrlToRunnableMap.containsKey(url)) {
			mUrlToRunnableMap.put(url, runnable);
			mExecutor.execute(runnable);
		}
	}

	@Override
	public synchronized void setNetworkRequestCreator(NetworkRequestCreator networkRequestCreator) {
		if (networkRequestCreator == null) {
			mNetworkRequestCreator = new DefaultNetworkRequestCreator();
		} else {
			mNetworkRequestCreator = networkRequestCreator;
		}
	}

	private synchronized void removeUrlFromMap(String url) {
		mUrlToRunnableMap.remove(url);
	}

	class ImageDownloadingRunnable implements Prioritizable {
		private final String mUrl;

		public ImageDownloadingRunnable(String url) {
			mUrl = url;
		}

		@Override
		public void run() {
			try {
				mNetworkRequestCreator.getInputStream(mUrl, new InputStreamListener() {
					@Override
					public void onInputStreamReady(InputStream inputStream) {
						String errorMessage = loadInputStreamToDisk(inputStream);
						removeUrlFromMap(mUrl);
						if (errorMessage != null) {
							mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
						} else {
							mImageDownloadObserver.onImageDownloaded(mUrl);
						}
					}

					@Override
					public void onFailure(String errorMessage) {
						removeUrlFromMap(mUrl);
						mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
					}
				});
			} catch (IllegalStateException e) {
				reportIllegalStateExceptionLoadFailure(e);
			}
		}

		private void reportIllegalStateExceptionLoadFailure(IllegalStateException e) {
			/*
			 * NOTE: If a bad URL is passed in (for example, mUrl = "N/A", the client.execute() call will throw an IllegalStateException. We do not want this exception to crash the app. Rather, we want to log the error
			 * and report a failure.
			 */
			Log.w(ImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URL: " + mUrl);
			e.printStackTrace();
			String errorMessage = "Failed to download image. A stack trace has been output to the logs. Message: " + e.getMessage();
			mImageDownloadObserver.onImageDownloadFailed(mUrl, errorMessage);
		}

		private String loadInputStreamToDisk(InputStream inputStream) {
			String errorMessage = null;
			if (inputStream != null) {
				try {
					mNetworkToDiskInterface.downloadImageFromInputStream(mUrl, inputStream);
				} catch (IOException e) {
					errorMessage = "IOException when downloading image: " + mUrl + ", Exception type: " + e.getClass().getName() + ", Exception message: " + e.getMessage();
				} catch (IllegalArgumentException e) {
					errorMessage = "Failed to download image with error message: " + e.getMessage();
				} catch (IllegalStateException e) {
					/*
					 * NOTE: If a bad URL is passed in (for example, mUrl = "N/A", the client.execute() call will throw an IllegalStateException. We do not want this exception to crash the app. Rather, we want to log the
					 * error and report a failure.
					 */
					Log.w(ImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URL: " + mUrl);
					e.printStackTrace();
					errorMessage = "Failed to download image. A stack trace has been output to the logs. Message: " + e.getMessage();
				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (IOException e) {
					}
				}
			}
			return errorMessage;
		}

		@Override
		public int getTargetPriorityAccessorIndex() {
			return 0;
		}
	}

	@Override
	public synchronized boolean isNetworkRequestPendingForUrl(String url) {
		return mUrlToRunnableMap.containsKey(url);
	}
}
