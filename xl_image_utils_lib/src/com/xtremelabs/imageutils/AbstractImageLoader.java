package com.xtremelabs.imageutils;

import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.xtremelabs.imageutils.ThreadChecker.CalledFromWrongThreadException;

public abstract class AbstractImageLoader {
	// TODO: Add optional logging levels.
	public static final String TAG = "ImageLoader";

	private ImageViewReferenceMapper mViewMapper = new ImageViewReferenceMapper();
	private LifecycleReferenceManager mReferenceManager;
	private Context mApplicationContext;
	private Object mKey;
	private boolean mDestroyed = false;

	private Options mDefaultOptions = new Options();

	// TODO: Have an API call that can get a bitmap without an ImageView.
	// TODO: Have an API call that can take a Bitmap without a network request?
	// TODO: Make a loadImage call that operates off the UI thread, or make the
	// loadImage calls compatible with non-UI threads.

	/**
	 * Instantiates a new {@link ImageLoader} that maps all requests to the
	 * provided {@link Activity}.
	 * 
	 * @param key
	 * @param applicationContext
	 * 
	 *  @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	protected AbstractImageLoader(Object key, Context applicationContext) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (key == null) {
			throw new IllegalArgumentException(
					"Key inside the ImageLoader cannot be null!");
		}
		initKeyAndAppContext(key, applicationContext);
	}

	/**
	 * When implementing the {@link ImageLoader} in an {@link Activity}, this
	 * method MUST BE CALLED from the Activity's onDestroy method.
	 * 
	 * When implementing the {@link ImageLoader} in a {@link Fragment}, this
	 * method MUST BE CALLED from the Fragment's onDestroyView method.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This is thrown if the method is called from off the UI
	 *             thread.
	 */
	public void destroy() {
		ThreadChecker.throwErrorIfOffUiThread();

		mDestroyed = true;

		List<ImageManagerListener> listeners = mReferenceManager
				.removeListenersForKey(mKey);
		if (listeners != null) {
			for (ImageManagerListener listener : listeners) {
				mViewMapper.removeImageView(listener);
			}
		}
	}

	/**
	 * The ImageLoader will default to the options provided here if no other
	 * options are provided.
	 * 
	 * @param options
	 *            If set to null, the ImageLoader will automatically select the
	 *            system's default options set.
	 */
	public void setDefaultOptions(Options options) {
		if (options == null) {
			mDefaultOptions = new Options();
		} else {
			mDefaultOptions = options;
		}
	}

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Loads the image located at the provided URL into the provided
	 * {@link ImageView}. The image will be cached in both the disk cache and in
	 * the RAM cache.
	 * 
	 * If called multiple times for the same ImageView, only the last requested
	 * image will be loaded into the view.
	 * 
	 * This method uses the ImageLoader's default options. The default options
	 * can be changed using the "setDefaultOptions" method.
	 * 
	 * @param imageView
	 *            The view object that will receive the image requested.
	 * @param url
	 *            Location of the image on the web.
	 */
	public void loadImage(ImageView imageView, String url) {
		if (!mDestroyed) {
			ImageManagerListener imageManagerListener = getDefaultImageManagerListener(mDefaultOptions);
			performImageRequestOnUiThread(imageView, url, mDefaultOptions,
					imageManagerListener);
		} else {
			Log.w(TAG,
					"WARNING: loadImage was called after the ImageLoader was destroyed.");
		}
	}

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Loads the image located at the provided URL into the provided
	 * {@link ImageView}. The image will be cached in both the disk cache and in
	 * the RAM cache.
	 * 
	 * If called multiple times for the same ImageView, only the last requested
	 * image will be loaded into the view.
	 * 
	 * @param imageView
	 *            The view object that will receive the image requested.
	 * @param url
	 *            Location of the image on the web.
	 * @param options
	 *            If options is set to null, the {@link ImageLoader} will use
	 *            the default options. See the {@link Options} docs for
	 *            additional details.
	 */
	public void loadImage(ImageView imageView, String url, Options options) {
		if (!mDestroyed) {
			if (options == null) {
				options = mDefaultOptions;
			}

			ImageManagerListener imageManagerListener = getDefaultImageManagerListener(options);
			performImageRequestOnUiThread(imageView, url, options,
					imageManagerListener);
		} else {
			Log.w(TAG,
					"WARNING: loadImage was called after the ImageLoader was destroyed.");
		}
	}

	/**
	 * Downloads and caches the image at the provided URL. The image will be
	 * cached in both the disk cache and in the RAM cache.
	 * 
	 * The image WILL NOT BE LOADED to the {@link ImageView}. Instead, the
	 * {@link ImageLoaderListener} will have its onImageAvailable() method
	 * called on the UI thread with a reference to both the {@link ImageView}
	 * and the {@link Bitmap}. You will have to load the bitmap to the view
	 * yourself
	 * 
	 * This method is useful if you want to perform some kind of animation when
	 * loading displaying the bitmap.
	 * 
	 * @param imageView
	 *            The view object that will receive the image requested.
	 * @param url
	 *            Location of the image on the web.
	 * @param options
	 *            If options is set to null, the {@link ImageLoader} will use
	 *            the default options. See the {@link Options} docs for
	 *            additional details.
	 */
	public void loadImage(ImageView imageView, String url, Options options,
			final ImageLoaderListener listener) {
		if (!mDestroyed) {
			if (listener == null) {
				throw new IllegalArgumentException(
						"You cannot pass in a null ImageLoadingListener.");
			}

			if (options == null) {
				options = mDefaultOptions;
			}

			ImageManagerListener imageManagerListener = getImageManagerListenerWithCallback(
					listener, options);
			performImageRequestOnUiThread(imageView, url, options,
					imageManagerListener);
		} else {
			Log.w(TAG,
					"WARNING: loadImage was called after the ImageLoader was destroyed.");
		}
	}

	/**
	 * This method will load the selected resource into the {@link ImageView}
	 * and cancel any previous requests that have been made with the provided
	 * {@link ImageView}.
	 * 
	 * @param imageView
	 * @param resourceId
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This is thrown if the method is called from off the UI
	 *             thread.
	 */
	public void loadImageFromResource(ImageView imageView, int resourceId) {
		if (!mDestroyed) {
			ThreadChecker.throwErrorIfOffUiThread();

			mViewMapper.removeListener(imageView);
			imageView.setImageResource(resourceId);
		}
	}

	// TODO: Make an API call that will load the bitmap into place with an
	// animation

	// TODO: Have a load image call that accepts a resource ID rather than
	// making the user stop the load manually.

	// TODO: Return a boolean indicating whether an image load was actually
	// stopped.
	// TODO: Think about this call.
	// TODO: Have an isLoadingImage call.
	/**
	 * This call prevents any previous loadImage call from loading a Bitmap into
	 * the provided ImageView.
	 * 
	 * Please note it will not prevent any future loadImage calls from loading
	 * an image into that view.
	 * 
	 * This is useful if you have a ListView that occasionally needs images from
	 * the ImageLoader, and other times from a resources file. Before loading
	 * the resource file's image, you would call this method.
	 * 
	 * @param imageView
	 * @returns True if an image load was stopped. False on failure.
	 */
	public boolean stopLoadingImage(ImageView imageView) {
		return mViewMapper.removeListener(imageView) != null;
	}

	/**
	 * Forces the memory cache to release all references to bitmaps.
	 * 
	 * NOTE: The images in the memcache will not be garbage collected if the app
	 * still has references to the bitmaps. For example, if the bitmap is loaded
	 * to an {@link ImageView} and the ImageView is still being referenced.
	 */
	public void clearMemCache() {
		ImageCacher.getInstance(mApplicationContext).clearMemCache();
	}

	/**
	 * COMPATIBILITY: API levels 11 and under
	 * 
	 * PLEASE SEE setMaximumMemCacheSize for adjusting the memcache size for
	 * devices API level 12+.
	 * 
	 * Sets the maximum number of images that will be contained within the
	 * memory cache.
	 * 
	 * WARNING: Setting the memory cache size value too high will result in
	 * OutOfMemory exceptions. Developers should test their apps thoroughly and
	 * modify the value set using this method based on memory consumption and
	 * app performance. A larger cache size = higher performance but worse
	 * memory usage. A smaller cache size means worse performance but better
	 * memory usage.
	 * 
	 * @param numImages
	 *            The number of images that can be stored within the memory
	 *            cache.
	 */
	public void setMemCacheSize(int numImages) {
		if (Build.VERSION.SDK_INT <= 11) {
			ImageCacher.getInstance(mApplicationContext).setMaximumCacheSize(
					numImages);
		}
	}

	/**
	 * COMPATIBILITY: API levels 12+
	 * 
	 * PLEASE SEE setMemCacheSize for adjusting the memcache size for devices
	 * API level 11 and under.
	 * 
	 * Sets the maximum size of the memory cache in bytes.
	 * 
	 * WARNING: Setting the memory cache size value too high will result in
	 * OutOfMemory exceptions. Developers should test their apps thoroughly and
	 * modify the value set using this method based on memory consumption and
	 * app performance. A larger cache size = higher performance but worse
	 * memory usage. A smaller cache size means worse performance but better
	 * memory usage.
	 * 
	 * @param maxSizeInBytes
	 *            The maximum size of the memory cache in bytes.
	 */
	public void setMaximumMemCacheSize(long maxSizeInBytes) {
		if (Build.VERSION.SDK_INT >= 12) {
			ImageCacher.getInstance(mApplicationContext).setMaximumCacheSize(
					maxSizeInBytes);
		}
	}

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Caches the image at the provided URL into the disk cache. This call is
	 * asynchronous and cannot be cancelled once called.
	 * 
	 * This call is useful when pre-caching large images, as they will not
	 * increase RAM usage, but will speed up image load times.
	 * 
	 * @param url
	 * @param applicationContext
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This is thrown if the method is called from off the UI
	 *             thread.
	 */
	public static void precacheImageToDisk(String url,
			Context applicationContext) {
		ThreadChecker.throwErrorIfOffUiThread();

		ImageCacher.getInstance(applicationContext).precacheImage(url);
	}

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Caches the image at the provided URL into both the disk cache and into
	 * the memory cache.
	 * 
	 * This method call is useful for pre-caching smaller images. If used for a
	 * ListView that has many small images, the quality of scrolling will be
	 * vastly improved.
	 * 
	 * The Width and Height allow you to specify the size of the view that the
	 * image will be loaded to. If the image is significantly larger than the
	 * provided width and/or height, the image will be scaled down in memory,
	 * allowing for significant improvements to memory usage and performance, at
	 * no cost to image detail.
	 * 
	 * @param url
	 * @param applicationContext
	 * @param width
	 *            See comment above. Pass in NULL if you want the width to be
	 *            ignored.
	 * @param height
	 *            See comment above. Pass in NULL if you want the width to be
	 *            ignored.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This is thrown if the method is called from off the UI
	 *             thread.
	 */
	public void precacheImageToDiskAndMemory(String url,
			Context applicationContext, Integer width, Integer height) {
		// TODO: Replace the width and height with options?
		ThreadChecker.throwErrorIfOffUiThread();

		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.height = height;
		scalingInfo.width = width;
		mReferenceManager.getBitmap(applicationContext, url,
				getBlankImageManagerListener(), scalingInfo);
	}

	protected void initKeyAndAppContext(Object key, Context applicationContext) {
		mApplicationContext = applicationContext;
		mKey = key;
		mReferenceManager = LifecycleReferenceManager
				.getInstance(applicationContext);
	}

	private void performImageRequestOnUiThread(final ImageView imageView,
			final String url, final Options options,
			final ImageManagerListener imageManagerListener) {
		if (ThreadChecker.isOnUiThread())
			performImageRequest(imageView, url, options, imageManagerListener);
		else {
			new Handler(mApplicationContext.getMainLooper())
					.post(new Runnable() {

						@Override
						public void run() {
							if (!mDestroyed)
								performImageRequest(imageView, url, options,
										imageManagerListener);
						}
					});
		}
	}

	private void performImageRequest(ImageView imageView, String url,
			Options options, ImageManagerListener imageManagerListener) {
		mapImageView(imageView, imageManagerListener);
		setPreLoadImage(imageView, options);

		ScalingInfo scalingInfo = getScalingInfo(imageView, options);
		mReferenceManager.getBitmap(mKey, url, imageManagerListener,
				scalingInfo);
	}

	private void setPreLoadImage(ImageView imageView, Options options) {
		if (options.wipeOldImageOnPreload) {
			if (options.placeholderImageResourceId != null) {
				imageView.setImageResource(options.placeholderImageResourceId);
			} else {
				imageView.setImageBitmap(null);
			}
		}
	}

	/**
	 * Calculates the scaling information needed by the ImageCacher. The scaling
	 * info contains either the bounds used for reducing image size, or an
	 * override sample size to force manual memory savings.
	 * 
	 * @param imageView
	 *            Depending on the options that are passed in, this imageView's
	 *            bounds may be auto detected and loaded into the ScalingInfo.
	 * @param options
	 *            The Options lets us know what scaling information we need to
	 *            retreive, if any.
	 * @return Returns the information the imageCacher needs to figure out how
	 *         to decode the downloaded image.
	 */
	public ScalingInfo getScalingInfo(ImageView imageView, final Options options) {
		ScalingInfo scalingInfo = new ScalingInfo();
		if (options.overrideSampleSize != null) {
			scalingInfo.sampleSize = options.overrideSampleSize;
			return scalingInfo;
		}

		/*
		 * FIXME: It appears as though the width and height bound constraints
		 * are not being followed exactly. Review this implementation.
		 */
		Integer width = options.widthBounds;
		Integer height = options.heightBounds;

		if (options.useScreenSizeAsBounds) {
			Dimensions screenSize = DisplayUtility
					.getDisplaySize(mApplicationContext);
			width = Math.min(screenSize.getWidth(),
					width == null ? screenSize.getWidth() : width);
			height = Math.min(screenSize.getHeight(),
					height == null ? screenSize.getHeight() : height);
		}

		if (options.autoDetectBounds) {
			Point imageBounds = ViewDimensionsUtil
					.getImageViewDimensions(imageView);
			if (imageBounds.x != -1) {
				if (width == null) {
					width = imageBounds.x;
				} else {
					width = Math.min(width, imageBounds.x);
				}
			}
			if (imageBounds.y != -1) {
				if (height == null) {
					height = imageBounds.y;
				} else {
					height = Math.min(height, imageBounds.y);
				}
			}
		}

		scalingInfo.width = width;
		scalingInfo.height = height;
		return scalingInfo;
	}

	private void mapImageView(ImageView view, ImageManagerListener listener) {
		ImageManagerListener oldListener = mViewMapper.removeListener(view);
		if (oldListener != null) {
			mReferenceManager.cancelRequest(oldListener);
		}
		mViewMapper.registerImageViewToListener(view, listener);
	}

	/**
	 * Handles loading the bitmap onto the ImageView upon it becoming available
	 * from the ImageCacher.
	 * 
	 * @param options
	 * @return
	 */
	private ImageManagerListener getDefaultImageManagerListener(
			final Options options) {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed(String error) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null
						&& options.unsuccessfulLoadResourceId != null) {
					imageView
							.setImageResource(options.unsuccessfulLoadResourceId);
				}
			}

			@Override
			public void onImageReceived(Bitmap bitmap,
					ImageReturnedFrom returnedFrom) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null) {
					if (Logger.isProfiling()) {
						Profiler.init("Setting image bitmap for default listener");
					}
					imageView.setImageBitmap(bitmap);
					if (Logger.isProfiling()) {
						Profiler.report("Setting image bitmap for default listener");
					}
				}
			}
		};
	}

	/**
	 * Sends the Bitmap back to the original caller without loading it to the
	 * ImageView first.
	 * 
	 * @param listener
	 * @param listenerOptions
	 * @return
	 */
	private ImageManagerListener getImageManagerListenerWithCallback(
			final ImageLoaderListener listener, final Options listenerOptions) {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed(String error) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null
						&& listenerOptions.unsuccessfulLoadResourceId != null) {
					imageView
							.setImageResource(listenerOptions.unsuccessfulLoadResourceId);
				}
				listener.onImageLoadError(error);
			}

			@Override
			public void onImageReceived(Bitmap bitmap,
					ImageReturnedFrom returnedFrom) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null) {
					listener.onImageAvailable(imageView, bitmap, returnedFrom);
				}
			}
		};
	}

	/**
	 * Stub ImageManagerListener used for pre-caching images.
	 * 
	 * @return
	 */
	private ImageManagerListener getBlankImageManagerListener() {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed(String error) {
			}

			@Override
			public void onImageReceived(Bitmap bitmap,
					ImageReturnedFrom returnedFrom) {
			}
		};
	}

	/**
	 * This class provides all the options that can be set when making loadImage
	 * calls.
	 * 
	 * See the Javadocs for the individual fields for more detail.
	 */
	public static class Options {
		/**
		 * Forces the image to be decoded with the specified sample size. This
		 * will override any other parameters that affect the sample size of the
		 * image.
		 * 
		 * NOTE: This value, if specified, should always be a positive power of
		 * 2. The higher the number provided, the further the image will be
		 * scaled down.
		 * 
		 * Example: A sample size of 2 will decrease the size of the image by 4.
		 * A sample size of 4 will decrease the size of the image by 16.
		 * 
		 * Default value: null.
		 */
		public Integer overrideSampleSize = null;

		/**
		 * If specified, this value allows the cacher to conserve memory by
		 * estimating the optimal sample size. This works in conjunction with
		 * the widthBounds field, so both can be specified at the same time.
		 * 
		 * Default value: null.
		 */
		public Integer heightBounds = null;

		/**
		 * If specified, this value allows the cacher to conserve memory by
		 * estimating the optimal sample size. This works in conjunction with
		 * the heightBounds field, so both can be specified at the same time.
		 * 
		 * Default value: null.
		 */
		public Integer widthBounds = null;

		/**
		 * If true, the ImageLoader will attempt to optimize the sample size for
		 * the image being returned.
		 * 
		 * Default value: true.
		 */
		public boolean autoDetectBounds = true;

		/**
		 * If true, the ImageLoader will select a sample size that will optimize
		 * the image size for the size of the screen.
		 * 
		 * Default value: true.
		 */
		public boolean useScreenSizeAsBounds = true;

		/**
		 * If set to true, the ImageLoader will, before getting the Bitmap,
		 * replace the current image within the ImageView with either a null
		 * Bitmap or the image resource indicated by the
		 * placeholderImageResourceId.
		 * 
		 * If set to false, the ImageLoader will only attempt to load the
		 * requested Bitmap to the view.
		 */
		public boolean wipeOldImageOnPreload = true;

		/**
		 * The ImageLoader will load the resource at this ID prior to making the
		 * image request.
		 * 
		 * Default value: null.
		 */
		public Integer placeholderImageResourceId = null;

		/**
		 * In the event that the image load fails, the resource at the provided
		 * ID will be loaded into the ImageView.
		 * 
		 * Default value: null.
		 */
		public Integer unsuccessfulLoadResourceId = null;
	}
}
