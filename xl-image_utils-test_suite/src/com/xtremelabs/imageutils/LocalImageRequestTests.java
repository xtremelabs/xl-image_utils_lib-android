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

import java.util.List;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import android.test.UiThreadTest;
import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageResponse.ImageResponseStatus;
import com.xtremelabs.imageutils.testutils.DelayedLoop;

public class LocalImageRequestTests extends AndroidTestCase {
	private static final String LOCAL_IMAGE_URI = "file:///some/location/of/an/image.jpg";
	private ImageLoader mImageLoader;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ThreadChecker.disableUiThreadCheck();
	}

	@Override
	protected void tearDown() throws Exception {
		ThreadChecker.enableUiThreadCheck();

		super.tearDown();
	}

	private void initImageLoader() {
		mImageLoader = ImageLoader.buildImageLoaderForTesting(this, getContext().getApplicationContext());
		mImageLoader.stubReferenceManager(new ReferenceManager() {
			@Override
			public void getBitmap(Object key, CacheRequest imageRequest, ImageManagerListener imageManagerListener) {
				imageManagerListener.onImageReceived(new ImageResponse(null, ImageReturnedFrom.DISK, ImageResponseStatus.SUCCESS));
			}

			@Override
			public void cancelRequest(ImageManagerListener imageManagerListener) {
			}

			@Override
			public List<ImageManagerListener> cancelRequestsForKey(Object arg0) {
				return null;
			}
		});
	}

	@UiThreadTest
	public void testLocalImageRequest() {
		initImageLoader();

		final DelayedLoop delayedLoop = new DelayedLoop(5000);

		ImageView imageView = new ImageView(getContext());
		mImageLoader.loadImage(imageView, LOCAL_IMAGE_URI, null, new ImageLoaderListener() {
			@Override
			public void onImageLoadError(String error) {
				delayedLoop.flagFailure();
			}

			@Override
			public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				delayedLoop.flagSuccess();
			}
		});

		delayedLoop.startLoop();
		delayedLoop.assertPassed();
	}
}
