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

import android.util.Log;

import com.xtremelabs.imageutils.NetworkRequestCreator.InputStreamListener;

class ImageDownloader implements ImageNetworkInterface {
	@SuppressWarnings("unused")
	private static final String TAG = "DefaultImageDownloader";

	private final NetworkToDiskInterface mNetworkToDiskInterface;
	private final ImageDownloadObserver mImageDownloadObserver;
	private NetworkRequestCreator mNetworkRequestCreator = new DefaultNetworkRequestCreator();

	public ImageDownloader(NetworkToDiskInterface networkToDiskInterface, ImageDownloadObserver imageDownloadObserver) {
		mNetworkToDiskInterface = networkToDiskInterface;
		mImageDownloadObserver = imageDownloadObserver;
	}

	@Override
	public Prioritizable getNetworkPrioritizable(ImageRequest imageRequest) {
		return new ImageDownloadingRunnable(imageRequest.getUri());
	}

	@Override
	public synchronized void setNetworkRequestCreator(NetworkRequestCreator networkRequestCreator) {
		if (networkRequestCreator == null) {
			mNetworkRequestCreator = new DefaultNetworkRequestCreator();
		} else {
			mNetworkRequestCreator = networkRequestCreator;
		}
	}

	class ImageDownloadingRunnable extends Prioritizable {
		private final String mUri;

		public ImageDownloadingRunnable(String uri) {
			mUri = uri;
		}

		@Override
		public void execute() {
			try {
				mNetworkRequestCreator.getInputStream(mUri, new InputStreamListener() {
					@Override
					public void onInputStreamReady(InputStream inputStream) {
						String errorMessage = loadInputStreamToDisk(inputStream);
						if (errorMessage != null) {
							mImageDownloadObserver.onImageDownloadFailed(mUri, errorMessage);
						} else {
							mImageDownloadObserver.onImageDownloaded(mUri);
						}
					}

					@Override
					public void onFailure(String errorMessage) {
						mImageDownloadObserver.onImageDownloadFailed(mUri, errorMessage);
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
			Log.w(ImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URL: " + mUri);
			e.printStackTrace();
			String errorMessage = "Failed to download image. A stack trace has been output to the logs. Message: " + e.getMessage();
			mImageDownloadObserver.onImageDownloadFailed(mUri, errorMessage);
		}

		private String loadInputStreamToDisk(InputStream inputStream) {
			String errorMessage = null;
			if (inputStream != null) {
				try {
					mNetworkToDiskInterface.downloadImageFromInputStream(mUri, inputStream);
				} catch (IOException e) {
					errorMessage = "IOException when downloading image: " + mUri + ", Exception type: " + e.getClass().getName() + ", Exception message: " + e.getMessage();
				} catch (IllegalArgumentException e) {
					errorMessage = "Failed to download image with error message: " + e.getMessage();
				} catch (IllegalStateException e) {
					/*
					 * NOTE: If a bad URL is passed in (for example, mUrl = "N/A", the client.execute() call will throw an IllegalStateException. We do not want this exception to crash the app. Rather, we want to log the
					 * error and report a failure.
					 */
					Log.w(ImageLoader.TAG, "IMAGE LOAD FAILED - An error occurred while performing the network request for the image. Stack trace below. URI: " + mUri);
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

		@Override
		public Request<?> getRequest() {
			return new Request<String>(mUri);
		}
	}
}
