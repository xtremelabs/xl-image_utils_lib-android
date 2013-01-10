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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;
import com.xtremelabs.imageutils.test.R;
import com.xtremelabs.imageutils.testutils.DelayedLoop;
import com.xtremelabs.testactivity.MainActivity;

@SuppressLint("NewApi")
public class DiskLRUCacherTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private static final String IMAGE_FILE_NAME = "disk_cache_test_image.jpg";
	private static final String TEST_URI = "file:///my/image.jpg";

	private DiskLRUCacher mDiskCacher;
	private String mKittenImageUri = null;

	public DiskLRUCacherTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		if (mKittenImageUri == null) {
			mKittenImageUri = "file://" + getActivity().getCacheDir() + File.separator + IMAGE_FILE_NAME;
			loadKittenToFile();
		}

		mDiskCacher = new DiskLRUCacher(getActivity().getApplicationContext(), new BlankImageDiskObserver());
	}

	@Override
	protected void finalize() throws Throwable {
		deleteKitten();

		super.finalize();
	}

	public void testImageDetailsIsOffUIThread() {
		StrictMode.setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyDeath().build());
		mDiskCacher.retrieveImageDetails(TEST_URI);
	}

	public void testImageDetailRetrieval() {
		final DelayedLoop delayedLoop = new DelayedLoop(2000);

		mDiskCacher.stubImageDiskObserver(new ImageDiskObserver() {
			@Override
			public void onImageDetailsRetrieved(final String uri) {
				delayedLoop.flagSuccess();
			}

			@Override
			public void onImageDetailsRequestFailed(String uri, final String errorMessage) {
				delayedLoop.flagFailure();
			}

			@Override
			public void onImageDecoded(DecodeSignature decodeSignature, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onImageDecodeFailed(DecodeSignature decodeSignature, String error) {
			}
		});
		mDiskCacher.cacheImageDetails(mKittenImageUri);
		delayedLoop.startLoop();
		delayedLoop.assertPassed();

		assertNotNull(mDiskCacher.getImageDimensions(mKittenImageUri));
	}

	public void testGetSampleSizeForPermanentStorage() {
		mDiskCacher.stubImageDiskObserver(new BlankImageDiskObserver());
		mDiskCacher.cacheImageDetails(mKittenImageUri);

		final Dimensions dimensions = mDiskCacher.getImageDimensions(mKittenImageUri);

		int sampleSize = mDiskCacher.getSampleSize(new ImageRequest(mKittenImageUri, new ScalingInfo()));
		assertEquals(1, sampleSize);

		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.width = dimensions.width / 2;
		scalingInfo.height = dimensions.height / 2;
		sampleSize = mDiskCacher.getSampleSize(new ImageRequest(mKittenImageUri, scalingInfo));
		assertEquals(2, sampleSize);
	}

	public void testIsCached() {
		mDiskCacher.cacheImageDetails(mKittenImageUri);
		assertTrue(mDiskCacher.isCached(mKittenImageUri));
	}

	public void testGettingPermanentStorageBitmap() {
		Bitmap bitmap = null;
		try {
			bitmap = mDiskCacher.getBitmapSynchronouslyFromDisk(new DecodeSignature(mKittenImageUri, 1, null));
		} catch (FileNotFoundException e) {
			fail();
		} catch (FileFormatException e) {
			fail();
		}

		assertNotNull(bitmap);
	}

	private void loadKittenToFile() {
		StrictMode.setThreadPolicy(ThreadPolicy.LAX);
		try {
			URI uri = new URI(mKittenImageUri);
			final File imageFile = new File(uri.getPath());
			final FileOutputStream fos = new FileOutputStream(imageFile);
			Bitmap bitmap = ((BitmapDrawable) getActivity().getResources().getDrawable(R.drawable.cute_kitten)).getBitmap();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

			assertTrue(imageFile.exists());
		} catch (final FileNotFoundException e) {
			fail();
		} catch (URISyntaxException e) {
			fail();
		}
	}

	private void deleteKitten() {
		final File imageFile = new File(getActivity().getCacheDir() + File.separator + IMAGE_FILE_NAME);
		imageFile.delete();
	}

	private class BlankImageDiskObserver implements ImageDiskObserver {
		@Override
		public void onImageDetailsRequestFailed(String uri, final String errorMessage) {
		}

		@Override
		public void onImageDetailsRetrieved(final String uri) {
		}

		@Override
		public void onImageDecoded(DecodeSignature decodeSignature, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
		}

		@Override
		public void onImageDecodeFailed(DecodeSignature decodeSignature, String error) {
		}
	}
}
