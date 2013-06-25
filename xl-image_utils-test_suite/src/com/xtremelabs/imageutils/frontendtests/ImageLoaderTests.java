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

package com.xtremelabs.imageutils.frontendtests;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageView;

import com.xtremelabs.imageutils.FileSystemManagerAccessUtil;
import com.xtremelabs.imageutils.ImageLoader;
import com.xtremelabs.imageutils.ImageLoaderListener;
import com.xtremelabs.imageutils.ImageReturnedFrom;
import com.xtremelabs.imageutils.test.R;
import com.xtremelabs.imageutils.testutils.GeneralTestUtils;
import com.xtremelabs.imageutils.testutils.GeneralTestUtils.DelayedLoopListener;
import com.xtremelabs.testactivity.MainActivity;

public class ImageLoaderTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private final String mTestUrl = "http://placekitten.com/500/300";

	private ImageView mImageView;
	private Bitmap mBitmap;
	private ImageReturnedFrom mImageReturnedFrom;
	private boolean mComplete, mFailed;
	private String mErrorMessage;
	private FileSystemManagerAccessUtil mDiskManagerAccessUtil;

	public ImageLoaderTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testLoadImageWithCallback() {
		try {
			runTestOnUiThread(new Runnable() {

				@Override
				public void run() {
					ImageLoader imageLoader = ImageLoader.buildImageLoaderForActivity(getActivity());

					mDiskManagerAccessUtil = new FileSystemManagerAccessUtil(getActivity().getApplicationContext());
					mDiskManagerAccessUtil.clearDiskCache();

					ImageView imageView = new ImageView(getActivity());

					resetFields();

					mFailed = false;
					mComplete = false;

					imageLoader.loadImage(imageView, mTestUrl, null, new ImageLoaderListener() {
						@Override
						public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
							mImageView = imageView;
							mBitmap = bitmap;
							mImageReturnedFrom = returnedFrom;
							mComplete = true;
						}

						@Override
						public void onImageLoadError(String error) {
							mFailed = true;
						}
					});

					GeneralTestUtils.delayedLoop(2000, new DelayedLoopListener() {
						@Override
						public boolean shouldBreak() {
							return mFailed || mComplete;
						}
					});

					assertFalse(mFailed);
					assertTrue(mComplete);

					assertNotNull(mImageView);
					assertNotNull(mBitmap);
					assertNotNull(mImageReturnedFrom);
					assertEquals(ImageReturnedFrom.NETWORK, mImageReturnedFrom);
					assertEquals(imageView, mImageView);
					assertNotSame(mBitmap.getWidth(), 0);
					assertEquals(getTestImageBitmap().getWidth(), mBitmap.getWidth());
					assertEquals(getTestImageBitmap().getHeight(), mBitmap.getHeight());

					imageLoader.destroy();
				}
			});
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void testNullUrlFailure() {
		try {
			runTestOnUiThread(new Runnable() {

				@Override
				public void run() {
					ImageLoader imageLoader = ImageLoader.buildImageLoaderForActivity(getActivity());

					mDiskManagerAccessUtil = new FileSystemManagerAccessUtil(getActivity().getApplicationContext());

					mDiskManagerAccessUtil.clearDiskCache();

					ImageView imageView = new ImageView(getActivity());

					resetFields();

					mFailed = false;
					mComplete = false;

					imageLoader.loadImage(imageView, null, null, new ImageLoaderListener() {
						@Override
						public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
							mComplete = true;
						}

						@Override
						public void onImageLoadError(String error) {
							mFailed = true;
							mErrorMessage = error;
						}
					});

					GeneralTestUtils.delayedLoop(2000, new DelayedLoopListener() {
						@Override
						public boolean shouldBreak() {
							return mFailed || mComplete;
						}
					});

					assertTrue(mFailed);
					assertFalse(mComplete);
					assertTrue(mErrorMessage.contains("Blank url"));

					imageLoader.destroy();
				}
			});
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void resetFields() {
		mBitmap = null;
		mComplete = false;
		mFailed = false;
		mImageReturnedFrom = null;
		mImageView = null;
	}

	private Bitmap getTestImageBitmap() {
		BitmapDrawable kittehDrawable = (BitmapDrawable) getActivity().getResources().getDrawable(R.drawable.kitteh_500_by_300);
		return kittehDrawable.getBitmap();
	}
}
