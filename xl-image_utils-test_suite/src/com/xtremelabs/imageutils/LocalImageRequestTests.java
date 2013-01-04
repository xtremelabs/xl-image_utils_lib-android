package com.xtremelabs.imageutils;

import java.util.List;

import android.graphics.Bitmap;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.ImageView;

import com.xtreme.utilities.testing.DelayedLoop;
import com.xtremelabs.testactivity.MainActivity;

public class LocalImageRequestTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private static final String LOCAL_IMAGE_URI = "file:///some/location/of/an/image.jpg";
	private ImageLoader mImageLoader;

	public LocalImageRequestTests() {
		super(MainActivity.class);
	}

	private void initImageLoader() {
		mImageLoader = new ImageLoader(getActivity());
		mImageLoader.stubReferenceManager(new ReferenceManager() {
			@Override
			public void getBitmap(Object key, ImageRequest imageRequest, ImageManagerListener imageManagerListener) {
				imageManagerListener.onImageReceived(null, ImageReturnedFrom.DISK);
			}

			@Override
			public List<ImageManagerListener> removeListenersForKey(Object key) {
				return null;
			}

			@Override
			public void cancelRequest(ImageManagerListener imageManagerListener) {
			}
		});
	}

	@UiThreadTest
	public void testLocalImageRequest() {
		initImageLoader();

		final DelayedLoop delayedLoop = new DelayedLoop(5000);

		ImageView imageView = new ImageView(getActivity());
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
