package com.xtremelabs.imageutils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.widget.ImageView;

/**
 * All loadImage requests MUST BE MADE FROM THE UI THREAD. All calls are thread safe and ListView safe.
 * 
 * @author Jamie Halpern
 */
public class ImageLoader {
	private ImageViewUrlMapper imageViewUrlMapper = new ImageViewUrlMapper();
	private ImageManager imageManager;
	private Context applicationContext;
	private Object key;

	public ImageLoader(Activity activity) {
		if (activity == null) {
			throw new IllegalArgumentException("Activity cannot be null!");
		}
		initKeyAndAppContext(activity, activity.getApplicationContext());
	}

	public ImageLoader(Fragment fragment) {
		if (fragment == null) {
			throw new IllegalArgumentException("Activity cannot be null!");
		}
		initKeyAndAppContext(fragment, fragment.getActivity().getApplicationContext());
	}

	private void initKeyAndAppContext(Object key, Context applicationContext) {
		this.applicationContext = applicationContext;
		this.key = key;
		imageManager = ImageManager.getInstance(applicationContext);
	}

	/**
	 * This MUST BE CALLED when your activity/fragment/etc. is destroyed.
	 */
	public void onDestroy() {
		ImageManager.getInstance(applicationContext).removeListenersForKey(key);
	}

	/**
	 * Sets the {@link ImageView}'s bitmap to null, then fetches an image from the provided URL or from the cache and loads it to the bitmap.
	 * 
	 * @param imageView
	 *            The view to which the downloading or cached image will be loaded.
	 * @param url
	 *            The location of the image on the web. This value is also used as the key for caching the image.
	 */
	public void loadImage(ImageView imageView, String url) {
		imageView.setImageBitmap(null);
		imageViewUrlMapper.registerImageViewToUrl(imageView, url);
		imageManager.getBitmap(key, url, imageReceivedListener);
	}

	/**
	 * Sets the {@link ImageView}'s image to the provided placeholder resource (from the drawables folder), then fetches the image from the URL and loads it to the view.
	 * 
	 * @param imageView
	 *            The view to which the downloading or cached image will be loaded.
	 * @param url
	 *            The location of the image on the web. This value is also used as the key for caching the image.
	 * @param placeholderImageResourceId
	 *            The resource that will be loaded into the imageView before the image from the URL becomes available.
	 */
	public void loadImage(ImageView imageView, String url, int placeholderImageResourceId) {
		imageView.setImageResource(placeholderImageResourceId);
		imageViewUrlMapper.registerImageViewToUrl(imageView, url);
		imageManager.getBitmap(key, url, imageReceivedListener);
	}

	/**
	 * Sets the {@link ImageView}'s image to null, then loads in the image from the provided URL when it becomes available.
	 * 
	 * When the image from the URL is done loading, a call will be made to the provided {@link ImageLoadingListener}.
	 * 
	 * @param imageView
	 *            The view to which the downloading or cached image will be loaded.
	 * @param url
	 *            The location of the image on the web. This value is also used as the key for caching the image.
	 * @param listener
	 *            This listener will be called after the image is loaded to the imageView.
	 */
	public void loadImage(ImageView imageView, String url, final ImageLoadingListener listener) {
		imageView.setImageBitmap(null);
		imageViewUrlMapper.registerImageViewToUrl(imageView, url);
		imageManager.getBitmap(key, url, new ImageReceiverWithCallback(listener));
	}

	/**
	 * Sets the {@link ImageView}'s image to the placeholder resource (from the drawables folder), then loads the image from the URL to the ImageView when it becomes available.
	 * 
	 * When the image from the URL is done loading, a call will be made to the provided {@link ImageLoadingListener}.
	 * 
	 * @param imageView
	 *            The view to which the downloading or cached image will be loaded.
	 * @param url
	 *            The location of the image on the web. This value is also used as the key for caching the image.
	 * @param placeholderImageResourceId
	 *            The resource that will be loaded into the imageView before the image from the URL becomes available.
	 * @param listener
	 *            This listener will be called after the image is loaded to the imageView.
	 */
	public void loadImage(ImageView imageView, String url, int placeholderImageResourceId, final ImageLoadingListener listener) {
		imageView.setImageResource(placeholderImageResourceId);
		imageViewUrlMapper.registerImageViewToUrl(imageView, url);
		imageManager.getBitmap(key, url, new ImageReceiverWithCallback(listener));
	}

	// TODO: Add in a call that includes the ID of a "fail" image.

	private ImageReceivedListener imageReceivedListener = new ImageReceivedListener() {
		@Override
		public void onImageReceived(Bitmap bitmap, String url) {
			imageViewUrlMapper.drawAllImagesForBitmap(bitmap, url);
		}

		@Override
		public void onLoadImageFailed() {
			// TODO Auto-generated method stub

		}
	};

	private class ImageReceiverWithCallback implements ImageReceivedListener {
		private ImageLoadingListener listener;

		public ImageReceiverWithCallback(ImageLoadingListener listener) {
			this.listener = listener;
		}

		@Override
		public void onImageReceived(Bitmap bitmap, String url) {
			imageViewUrlMapper.drawAllImagesForBitmap(bitmap, url);
			listener.onImageLoadComplete();
		}

		@Override
		public void onLoadImageFailed() {
			// TODO Auto-generated method stub

		}
	}
}
