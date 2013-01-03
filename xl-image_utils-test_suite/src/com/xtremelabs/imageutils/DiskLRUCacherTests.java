package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.test.ActivityInstrumentationTestCase2;

import com.example.xl_image_utils_android_testactivity.test.R;
import com.xtreme.utilities.testing.DelayedLoop;
import com.xtremelabs.imageutils.DiskLRUCacher.FileFormatException;
import com.xtremelabs.testactivity.MainActivity;

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

		mDiskCacher = new DiskLRUCacher(getActivity().getApplicationContext(), new ImageDiskObserver() {
			@Override
			public void onImageDecoded(final Bitmap bitmap, final String url, final int sampleSize, final ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onImageDecodeFailed(final String url, final int sampleSize, final String error) {
			}

			@Override
			public void onImageDetailsRequestFailed(String uri, final String errorMessage) {
			}

			@Override
			public void onImageDetailsRetrieved(final String uri) {
			}
		});
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
			public void onImageDecoded(final Bitmap bitmap, final String uri, final int sampleSize, final ImageReturnedFrom returnedFrom) {
			}

			@Override
			public void onImageDecodeFailed(final String uri, final int sampleSize, final String error) {
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

		int sampleSize = mDiskCacher.getSampleSize(mKittenImageUri, null, null);
		assertEquals(1, sampleSize);

		sampleSize = mDiskCacher.getSampleSize(mKittenImageUri, dimensions.getWidth() / 2, dimensions.getHeight() / 2);
		assertEquals(2, sampleSize);
	}

	public void testIsCached() {
		mDiskCacher.cacheImageDetails(mKittenImageUri);
		assertTrue(mDiskCacher.isCached(mKittenImageUri));
	}

	public void testGettingPermanentStorageBitmap() {
		Bitmap bitmap = null;
		try {
			bitmap = mDiskCacher.getBitmapSynchronouslyFromDisk(mKittenImageUri, 1);
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
		public void onImageDecoded(final Bitmap bitmap, final String uri, final int sampleSize, final ImageReturnedFrom returnedFrom) {
		}

		@Override
		public void onImageDecodeFailed(final String uri, final int sampleSize, final String error) {
		}

		@Override
		public void onImageDetailsRequestFailed(String uri, final String errorMessage) {
		}

		@Override
		public void onImageDetailsRetrieved(final String uri) {
		}
	}
}
