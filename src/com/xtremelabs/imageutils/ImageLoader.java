package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * This class simplifies the task of loading images from a URL into an {@link ImageView} on Android.
 * 
 * HOW TO USE:
 * 
 * Every {@link Activity} or {@link Fragment} that needs images must instantiate its own {@link ImageLoader} instance.
 * 
 * When used with an {@link Activity}, instantiate a new {@link ImageLoader} in the Activity's onCreate() method. Make sure you call the ImageLoader's onDestroy
 * method from within the Activity's onDestroy method.
 * 
 * When used with a {@link Fragment}, instantiate your ImageLoader in the onCreateView() method. Make sure you call the ImageLoader's onDestroy method in the
 * onDestroyView method.
 * 
 * @author Jamie Halpern
 */
public class ImageLoader {
	@SuppressWarnings("unused")
	private static final String TAG = "ImageLoader";

	private ImageViewReferenceMapper mViewMapper = new ImageViewReferenceMapper();
	private LifecycleReferenceManager mReferenceManager;
	private Context mApplicationContext;
	private Object mKey;

	private int mScreenWidth, mScreenHeight;

	/**
	 * Instantiate a new {@link ImageLoader} that maps all requests to the provided {@link Activity}.
	 * 
	 * This should be called from your Activity's onCreate method.
	 * 
	 * @param activity
	 *            All requests to the {@link ImageLoader} will be mapped to this {@link Activity}. All references are released when the ImageLoader's
	 *            onDestroy() method is called.
	 */
	public ImageLoader(Activity activity) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (activity == null) {
			throw new IllegalArgumentException("Activity cannot be null!");
		}
		initKeyAndAppContext(activity, activity.getApplicationContext());
	}

	/**
	 * Instantiate a new {@link ImageLoader} that maps all requests to the provided {@link Fragment}.
	 * 
	 * This should be called from your Fragment's onCreateView method.
	 * 
	 * @param fragment
	 *            All requests to the {@link ImageLoader} will be mapped to this {@link Fragment}. All references are released when the ImageLoader's
	 *            onDestroy() method is called.
	 */
	public ImageLoader(Fragment fragment) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (fragment == null) {
			throw new IllegalArgumentException("Fragment cannot be null!");
		}
		initKeyAndAppContext(fragment, fragment.getActivity().getApplicationContext());
	}

	/**
	 * When implementing the {@link ImageLoader} in an {@link Activity}, this method MUST BE CALLED from the Activity's onDestroy method.
	 * 
	 * When implementing the {@link ImageLoader} in a {@link Fragment}, this method MUST BE CALLED from the Fragment's onDestroyView method.
	 */
	public void onDestroy() {
		ThreadChecker.throwErrorIfOffUiThread();

		mReferenceManager.removeListenersForKey(mKey);
	}

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Loads the image located at the provided URL into the provided {@link ImageView}. The image will be cached in both the disk cache and in the RAM cache.
	 * 
	 * If called multiple times for the same ImageView, only the last requested image will be loaded into the view.
	 * 
	 * @param imageView
	 *            The view object that will receive the image requested.
	 * @param url
	 *            Location of the image on the web.
	 * @param options
	 *            If options is set to null, the {@link ImageLoader} will automatically try to optimize the image it returns with the default {@link Options}
	 *            settings. See the {@link Options} docs for additional details.
	 */
	public void loadImage(ImageView imageView, String url, Options options) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (options == null) {
			options = new Options();
		}

		ImageManagerListener imageManagerListener = getDefaultImageManagerListener(options);
		performImageRequest(imageView, url, options, imageManagerListener);
	}

	/**
	 * Downloads and caches the image at the provided URL. The image will be cached in both the disk cache and in the RAM cache.
	 * 
	 * The image WILL NOT BE LOADED to the {@link ImageView}. Instead, the {@link ImageLoaderListener} will have its onImageAvailable() method called on the UI
	 * thread with a reference to both the {@link ImageView} and the {@link Bitmap}. You will have to load the bitmap to the view yourself
	 * 
	 * This method is useful if you want to perform some kind of animation when loading displaying the bitmap.
	 * 
	 * @param imageView
	 *            The view object that will receive the image requested.
	 * @param url
	 *            Location of the image on the web.
	 * @param options
	 *            If options is set to null, the {@link ImageLoader} will automatically try to optimize the image it returns with the default {@link Options}
	 *            settings. See the {@link Options} docs for additional details.
	 */
	public void loadImage(ImageView imageView, String url, Options options, final ImageLoaderListener listener) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (listener == null) {
			throw new IllegalArgumentException("You cannot pass in a null ImageLoadingListener.");
		}

		if (options == null) {
			options = new Options();
		}

		ImageManagerListener imageManagerListener = getImageManagerListenerWithCallback(listener, options);
		performImageRequest(imageView, url, options, imageManagerListener);
	}

	/**
	 * If the image for the provided URL is on disk, this method will return a Point containing the dimensions of that image.
	 * 
	 * @param url
	 *            URL of the image. This is only used for figuring out where the image is on disk. This method will not pull the image from the web.
	 * @return Returns a point containing the dimensions of the image. Point.x is the width, Point.y is the height.
	 * @throws FileNotFoundException
	 *             If the image is not already cached, this method will throw this exception.
	 */
	public Dimensions getImageDimensions(String url) throws FileNotFoundException {
		return ImageCacher.getInstance(mApplicationContext).getImageDimensions(url);
	}

	/**
	 * Forces the memory cache to release all references to bitmaps.
	 * 
	 * NOTE: The images in the memcache will not be garbage collected if the app still has references to the bitmaps. For example, if the bitmap is loaded to an
	 * {@link ImageView} and the ImageView is still being referenced.
	 */
	public void clearMemCache() {
		ImageCacher.getInstance(mApplicationContext).clearMemCache();
	}

	/**
	 * COMPATIBILITY: API levels 11 and under
	 * 
	 * PLEASE SEE setMaximumMemCacheSize for adjusting the memcache size for devices API level 12+.
	 * 
	 * Sets the maximum number of images that will be contained within the memory cache.
	 * 
	 * WARNING: Setting the memory cache size value too high will result in OutOfMemory exceptions. Developers should test their apps thoroughly and modify the
	 * value set using this method based on memory consumption and app performance. A larger cache size = higher performance but worse memory usage. A smaller
	 * cache size means worse performance but better memory usage.
	 * 
	 * @param numImages
	 *            The number of images that can be stored within the memory cache.
	 */
	public void setMemCacheSize(int numImages) {
		if (Build.VERSION.SDK_INT <= 11) {
			ImageCacher.getInstance(mApplicationContext).setMaximumCacheSize(numImages);
		}
	}

	/**
	 * COMPATIBILITY: API levels 12+
	 * 
	 * PLEASE SEE setMemCacheSize for adjusting the memcache size for devices API level 11 and under.
	 * 
	 * Sets the maximum size of the memory cache in bytes.
	 * 
	 * WARNING: Setting the memory cache size value too high will result in OutOfMemory exceptions. Developers should test their apps thoroughly and modify the
	 * value set using this method based on memory consumption and app performance. A larger cache size = higher performance but worse memory usage. A smaller
	 * cache size means worse performance but better memory usage.
	 * 
	 * @param maxSizeInBytes
	 *            The maximum size of the memory cache in bytes.
	 */
	public void setMaximumMemCacheSize(long maxSizeInBytes) {
		if (Build.VERSION.SDK_INT >= 12) {
			ImageCacher.getInstance(mApplicationContext).setMaximumCacheSize(maxSizeInBytes);
		}
	}

	// TODO: Explain the different use cases for the precaching calls

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Caches the image at the provided URL into the disk cache. This call is asynchronous and cannot be cancelled once called.
	 * 
	 * This call is useful when pre-caching large images, as they will not increase RAM usage, but will speed up image load times.
	 * 
	 * @param url
	 * @param applicationContext
	 */
	public static void precacheImage(String url, Context applicationContext) {
		ThreadChecker.throwErrorIfOffUiThread();

		ImageCacher.getInstance(applicationContext).precacheImage(url);
	}

	/**
	 * This method must be called from the UI thread.
	 * 
	 * Caches the image at the provided URL into both the disk cache and into the memory cache.
	 * 
	 * This method call is useful for pre-caching smaller images. If used for a ListView that has many small images, the quality of scrolling will be vastly
	 * improved.
	 * 
	 * The Width and Height allow you to specify the size of the view that the image will be loaded to. If the image is significantly larger than the provided
	 * width and/or height, the image will be scaled down in memory, allowing for significant improvements to memory usage and performance, at no cost to image
	 * detail.
	 * 
	 * @param url
	 * @param applicationContext
	 * @param width
	 *            See comment above. Pass in NULL if you want the width to be ignored.
	 * @param height
	 *            See comment above. Pass in NULL if you want the width to be ignored.
	 */
	public void precacheImageToMemory(String url, Context applicationContext, Integer width, Integer height) {
		ThreadChecker.throwErrorIfOffUiThread();

		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.height = height;
		scalingInfo.width = width;
		mReferenceManager.getBitmap(applicationContext, url, getBlankImageManagerListener(), scalingInfo);
	}

	private void initKeyAndAppContext(Object key, Context applicationContext) {
		mApplicationContext = applicationContext;
		mKey = key;
		mReferenceManager = LifecycleReferenceManager.getInstance(applicationContext);
		Display display = ((WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		mScreenHeight = display.getHeight();
		mScreenWidth = display.getWidth();
	}

	private void performImageRequest(ImageView imageView, String url, Options options, ImageManagerListener imageManagerListener) {
		registerImageView(imageView, imageManagerListener);
		setPreLoadedImage(imageView, options);

		ScalingInfo scalingInfo = getScalingInfo(imageView, url, options, imageManagerListener);
		mReferenceManager.getBitmap(mKey, url, imageManagerListener, scalingInfo);
	}

	private void setPreLoadedImage(ImageView imageView, Options options) {
		if (options.placeholderImageResourceId != null) {
			imageView.setImageResource(options.placeholderImageResourceId);
		} else {
			imageView.setImageBitmap(null);
		}
	}

	private ScalingInfo getScalingInfo(ImageView imageView, String url, final Options options, ImageManagerListener listener) {
		ScalingInfo scalingInfo = new ScalingInfo();
		if (options.overrideSampleSize != null) {
			scalingInfo.sampleSize = options.overrideSampleSize;
			return scalingInfo;
		}

		Integer width = options.widthBounds;
		Integer height = options.heightBounds;

		if (options.useScreenSizeAsBounds) {
			width = Math.min(mScreenWidth, width == null ? mScreenWidth : width);
			height = Math.min(mScreenHeight, height == null ? mScreenHeight : height);
		}

		if (options.autoDetectBounds) {
			Point imageBounds = ViewDimensionsUtil.getImageViewDimensions(imageView);
			if (imageBounds.x != -1) {
				if (width == null) {
					width = imageBounds.x;
				} else {
					width = Math.min(width, imageBounds.x);
				}
			}
			if (imageBounds.y != -1) {
				height = imageBounds.y;
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

	private void registerImageView(ImageView view, ImageManagerListener listener) {
		ImageManagerListener oldListener = mViewMapper.removeListener(view);
		if (oldListener != null) {
			// FIXME: Get the cancel call working!
			// mReferenceManager.cancelRequest(oldListener);
		}
		mViewMapper.registerImageViewToListener(view, listener);
	}

	private ImageManagerListener getDefaultImageManagerListener(final Options options) {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed() {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null && options.unsuccessfulLoadResourceId != null) {
					imageView.setImageResource(options.unsuccessfulLoadResourceId);
				}
			}

			@Override
			public void onImageReceived(Bitmap bitmap, boolean isFromMemoryCache) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		};
	}

	private ImageManagerListener getImageManagerListenerWithCallback(final ImageLoaderListener listener, final Options listenerOptions) {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed() {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null && listenerOptions.unsuccessfulLoadResourceId != null) {
					imageView.setImageResource(listenerOptions.unsuccessfulLoadResourceId);
				}
				listener.onImageLoadError();
			}

			@Override
			public void onImageReceived(Bitmap bitmap, boolean isFromMemoryCache) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null) {
					listener.onImageAvailable(imageView, bitmap, isFromMemoryCache);
				}
			}
		};
	}

	private ImageManagerListener getBlankImageManagerListener() {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed() {
			}

			@Override
			public void onImageReceived(Bitmap bitmap, boolean isFromMemoryCache) {
			}
		};
	}

	/**
	 * This class provides all the options that can be set when making loadImage calls.
	 * 
	 * See the Javadocs for the individual fields for more detail.
	 * 
	 * @author Jamie Halpern
	 */
	public static class Options {
		/**
		 * Forces the image to be decoded with the specified sample size. This will override any other parameters that affect the sample size of the image.
		 * 
		 * NOTE: This value, if specified, should always be a positive power of 2. The higher the number provided, the further the image will be scaled down.
		 * 
		 * Example: A sample size of 2 will decrease the size of the image by 4. A sample size of 4 will decrease the size of the image by 16.
		 * 
		 * Default value: null.
		 */
		public Integer overrideSampleSize = null;

		/**
		 * If specified, this value allows the cacher to conserve memory by estimating the optimal sample size. This works in conjunction with the widthBounds
		 * field, so both can be specified at the same time.
		 * 
		 * Default value: null.
		 */
		public Integer heightBounds = null;

		/**
		 * If specified, this value allows the cacher to conserve memory by estimating the optimal sample size. This works in conjunction with the heightBounds
		 * field, so both can be specified at the same time.
		 * 
		 * Default value: null.
		 */
		public Integer widthBounds = null;

		/**
		 * If true, the ImageLoader will attempt to optimize the sample size for the image being returned.
		 * 
		 * Default value: true.
		 */
		public boolean autoDetectBounds = true;

		/**
		 * If true, the ImageLoader will select a sample size that will optimize the image size for the size of the screen.
		 * 
		 * Default value: true.
		 */
		public boolean useScreenSizeAsBounds = true;

		/**
		 * The ImageLoader will load the resource at this ID prior to making the image request.
		 * 
		 * Default value: null.
		 */
		public Integer placeholderImageResourceId = null;

		/**
		 * In the event that the image load fails, the resource at the provided ID will be loaded into the ImageView.
		 * 
		 * Default value: null.
		 */
		public Integer unsuccessfulLoadResourceId = null;
	}
}
