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

import java.lang.ref.WeakReference;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ThreadChecker.CalledFromWrongThreadException;

/**
 * This class should be constructed using its static "build" methods. The class should be instantiated in either onCreate (for activities) or onCreateView (for fragments).<br>
 * <br>
 * The {@link ImageLoader#destroy()} method <i>must</i> be called from either the activity's onDestroy method, or the fragment's onDestroyView method. This class should not be accessed after destroy has been called.<br>
 * <br>
 * This class is implementing {@link AbstractImageLoader} for legacy purposes. The {@link AbstractImageLoader} interface will be removed in a later API version.
 */
@SuppressWarnings("deprecation")
public class ImageLoader implements AbstractImageLoader {
	public static final String TAG = "ImageLoader";

	private final ImageViewReferenceMapper mViewMapper = new ImageViewReferenceMapper();
	private ReferenceManager mReferenceManager;
	private final DisplayUtility mDisplayUtility = new DisplayUtility();
	private Context mContext;
	private Object mKey;
	private volatile boolean mDestroyed = false;

	private volatile Options mDefaultOptions = new Options();

	// TODO Cancelled network calls should still save the downloaded image to disk.

	/*
	 * TODO Add documentation that explains that this call should not be used for loading images into fragments.
	 */
	public static ImageLoader buildImageLoaderForActivity(Activity activity) {
		return new ImageLoader(activity, activity);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static ImageLoader buildImageLoaderForFragment(Fragment fragment) {
		return new ImageLoader(fragment, fragment.getActivity());
	}

	public static ImageLoader buildImageLoaderForSupportFragment(android.support.v4.app.Fragment fragment) {
		return new ImageLoader(fragment, fragment.getActivity());
	}

	public static WidgetImageLoader buildWidgetImageLoader(Object key, Context context) {
		return new WidgetImageLoader(key, context);
	}

	static ImageLoader buildImageLoaderForTesting(Object object, Context context) {
		return new ImageLoader(object, context);
	}

	@Deprecated
	public ImageLoader(Activity activity) {
		this(activity, activity);
	}

	@SuppressLint("NewApi")
	@Deprecated
	public ImageLoader(Fragment fragment) {
		this(fragment, fragment.getActivity());
	}

	/**
	 * Instantiates a new {@link ImageLoader} that maps all requests to the provided {@link Activity}.
	 * 
	 * @param key
	 * @param applicationContext
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	protected ImageLoader(Object key, Context context) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (key == null) {
			throw new IllegalArgumentException("Key inside the ImageLoader cannot be null!");
		}
		initKeyAndContext(key, context);
		mReferenceManager = LifecycleReferenceManager.getInstance(context);
	}

	/**
	 * When implementing the {@link ImageLoader} in an {@link Activity}, this method MUST BE CALLED from the Activity's onDestroy method.
	 * 
	 * When implementing the {@link ImageLoader} in a {@link Fragment}, this method MUST BE CALLED from the Fragment's onDestroyView method.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This is thrown if the method is called from off the UI thread.
	 */
	public void destroy() {
		ThreadChecker.throwErrorIfOffUiThread();

		synchronized (this) {
			mDestroyed = true;
		}

		List<ImageManagerListener> listeners = mReferenceManager.cancelRequestsForKey(mKey);
		if (listeners != null) {
			for (ImageManagerListener listener : listeners) {
				mViewMapper.removeImageView(listener);
			}
		}
	}

	public void notifyConfigurationChanged() {
		mDisplayUtility.notifyConfigurationChanged();
	}

	/**
	 * The ImageLoader will default to the options provided here if no other options are provided.
	 * 
	 * @param options
	 *            If set to null, the ImageLoader will automatically select the system's default options set.
	 */
	public void setDefaultOptions(Options options) {
		if (options == null) {
			mDefaultOptions = new Options();
		} else {
			mDefaultOptions = options;
		}
	}

	/**
	 * Allows the usage of custom network libraries. If a {@link NetworkRequestCreator} is provided to the image system, all network calls will go through that interface.
	 * 
	 * @param appContext
	 * @param networkRequestCreator
	 */
	public static void setNetworkRequestCreator(Context appContext, NetworkRequestCreator networkRequestCreator) {
		ImageCacher.getInstance(appContext).setNetworkRequestCreator(networkRequestCreator);
	}

	public void loadImage(ImageRequest imageRequest) {
		if (imageRequest == null) {
			throw new IllegalArgumentException("You may not call \"loadImage\" with a null ImageRequest object.");
		}

		if (!isDestroyed()) {
			ImageLoaderListener listener = imageRequest.getImageLoaderListener();
			Options options = imageRequest.getOptions();
			if (options == null) {
				options = mDefaultOptions;
			}

			ImageManagerListener imageManagerListener;
			if (listener == null) {
				imageManagerListener = getDefaultImageManagerListener(options);
			} else {
				imageManagerListener = getImageManagerListenerWithCallback(listener, options);
			}

			CacheRequest cacheRequest = new CacheRequest(imageRequest.getUri(), getScalingInfo(imageRequest.getImageView(), options), options);
			cacheRequest.setImageRequestType(imageRequest.getImageRequestType());
			cacheRequest.setCacheKey(imageRequest.getCacheKey());
			performImageRequestOnUiThread(imageRequest.getImageView(), cacheRequest, options, imageManagerListener);
		} else {
			Log.w(TAG, "WARNING: loadImage was called after the ImageLoader was destroyed.");
		}
	}

	/**
	 * Loads the image located at the provided URI into the provided {@link ImageView}.<br>
	 * <br>
	 * If the URI is referring to a location on the web, the image will be cached both on disk and in memory. If the URI is a local file system URI, the image will be cached in memory.<br>
	 * <br>
	 * If loadImage is called for an ImageView multiple times, only the most recently requested image will be loaded into the view.<br>
	 * <br>
	 * This method uses the ImageLoader's default options. The default options can be changed using the "setDefaultOptions" method.
	 * 
	 * @param imageView
	 *            The bitmap will automatically be loaded to this view.<br>
	 * @param uri
	 *            Location of the image. The URI can refer to an image located either on the local file system or on the web (URL).<br>
	 * <br>
	 *            The URI scheme for local file system requests is "file".<br>
	 *            File system URI example: "file:///this/is/the/image/path/image.jpg".<br>
	 *            If using a file system URI, the image will be cached in the memory cache.<br>
	 */
	public void loadImage(ImageView imageView, String uri) {
		loadImage(imageView, uri, null, null);
	}

	/**
	 * This call is identical to {@link #loadImage(ImageView, String)}, only it allows the developer to provide custom options for the request. See {@link Options}.
	 * 
	 * @param imageView
	 *            The bitmap will automatically be loaded to this view.<br>
	 * <br>
	 * @param uri
	 *            Location of the image. The URI can refer to an image located either on the local file system or on the web (URL).<br>
	 * <br>
	 *            The URI scheme for local file system requests is "file".<br>
	 *            File system URI example: "file:///this/is/the/image/path/image.jpg".<br>
	 *            If using a file system URI, the image will be cached in the memory cache.<br>
	 * <br>
	 * @param options
	 *            If options is set to null, the {@link ImageLoader} will use the default options. The default options can be modified by calling {@link #setDefaultOptions(Options)}. See the {@link Options} docs for
	 *            additional details.
	 */
	public void loadImage(ImageView imageView, String uri, Options options) {
		loadImage(imageView, uri, options, null);
	}

	/**
	 * Loads the image located at the provided URI. If the image is located on the web, it will be cached on disk and in memory. If the image is located on the file system, it will be cached in memory.<br>
	 * <br>
	 * The image WILL NOT BE AUTOMATICALLY LOADED to the {@link ImageView}. Instead, the {@link ImageLoaderListener} will have its onImageAvailable() method called on the UI thread with a reference to both the
	 * {@link ImageView} and the bitmap. It is up to the developer to load the bitmap to the view.<br>
	 * <br>
	 * This method should be used if the app needs to:<br>
	 * - perform additional logic when the bitmap is returned<br>
	 * - manually handle image failures<br>
	 * - animate the bitmap.<br>
	 * <br>
	 * 
	 * @param imageView
	 *            The view that will be displaying the image. The bitmap will not be loaded directly into this view. Rather, a reference to the bitmap and to the ImageView will be passed back to the
	 *            {@link ImageLoaderListener}.<br>
	 * <br>
	 * @param uri
	 *            Location of the image. The URI can refer to an image located either on the local file system or on the web (URL).<br>
	 * <br>
	 *            The URI scheme for local file system requests is "file".<br>
	 *            File system URI example: "file:///this/is/the/image/path/image.jpg".<br>
	 *            If using a file system URI, the image will be cached in the memory cache.<br>
	 * <br>
	 * @param listener
	 *            This listener will be called once the image request is complete. If the bitmap was retrieved successfully, the
	 *            {@link ImageLoaderListener#onImageAvailable(ImageView, android.graphics.Bitmap, ImageReturnedFrom)} method will be called.
	 */
	public void loadImage(ImageView imageView, String uri, final ImageLoaderListener listener) {
		loadImage(imageView, uri, null, listener);
	}

	public void loadImage(String uri, BitmapListener listener) {
		baseLoadImage(null, uri, null, listener.getImageLoaderListener());
	}

	public void loadImage(String uri, Options options, BitmapListener listener) {
		baseLoadImage(null, uri, options, listener.getImageLoaderListener());
	}

	/**
	 * Loads the image located at the provided URI. If the image is located on the web, it will be cached on disk and in memory. If the image is located on the file system, it will be cached in memory.<br>
	 * <br>
	 * The image WILL NOT BE AUTOMATICALLY LOADED to the {@link ImageView}. Instead, the {@link ImageLoaderListener} will have its onImageAvailable() method called on the UI thread with a reference to both the
	 * {@link ImageView} and the bitmap. It is up to the developer to load the bitmap to the view.<br>
	 * <br>
	 * This method should be used if the app needs to:<br>
	 * - perform additional logic when the bitmap is returned<br>
	 * - manually handle image failures<br>
	 * - animate the bitmap.<br>
	 * <br>
	 * 
	 * @param imageView
	 *            The view that will be displaying the image. The bitmap will not be loaded directly into this view. Rather, a reference to the bitmap and to the ImageView will be passed back to the
	 *            {@link ImageLoaderListener}.<br>
	 * <br>
	 * @param uri
	 *            Location of the image. The URI can refer to an image located either on the local file system or on the web (URL).<br>
	 * <br>
	 *            The URI scheme for local file system requests is "file".<br>
	 *            File system URI example: "file:///this/is/the/image/path/image.jpg".<br>
	 *            If using a file system URI, the image will be cached in the memory cache.<br>
	 * <br>
	 * @param options
	 *            If options is set to null, the {@link ImageLoader} will use the default options. The default options can be modified by calling {@link #setDefaultOptions(Options)}. See the {@link Options} docs for
	 *            additional details.<br>
	 * <br>
	 * @param listener
	 *            This listener will be called once the image request is complete. If the bitmap was retrieved successfully, the
	 *            {@link ImageLoaderListener#onImageAvailable(ImageView, android.graphics.Bitmap, ImageReturnedFrom)} method will be called.
	 */
	public void loadImage(ImageView imageView, String uri, Options options, final ImageLoaderListener listener) {
		if (imageView == null) {
			throw new IllegalArgumentException("The method \"loadImage(ImageView, String)\" requires a non-null ImageView to be passed in.");
		}
		baseLoadImage(imageView, uri, options, listener);
	}

	private void baseLoadImage(ImageView imageView, String uri, Options options, ImageLoaderListener listener) {
		if (!isDestroyed()) {
			if (options == null) {
				options = mDefaultOptions;
			}

			ImageManagerListener imageManagerListener;
			if (listener == null) {
				imageManagerListener = getDefaultImageManagerListener(options);
			} else {
				imageManagerListener = getImageManagerListenerWithCallback(listener, options);
			}

			CacheRequest cacheRequest = new CacheRequest(uri, getScalingInfo(imageView, options), options);
			performImageRequestOnUiThread(imageView, cacheRequest, options, imageManagerListener);
		} else {
			Log.w(TAG, "WARNING: loadImage was called after the ImageLoader was destroyed.");
		}
	}

	/**
	 * This method will load the selected resource into the {@link ImageView} and cancel any previous requests that have been made to the {@link ImageView}.
	 * 
	 * @param imageView
	 * @param resourceId
	 */
	public void loadImageFromResource(final ImageView imageView, final int resourceId) {
		if (ThreadChecker.isOnUiThread()) {
			if (!isDestroyed()) {
				ThreadChecker.throwErrorIfOffUiThread();

				mViewMapper.removeListener(imageView);
				imageView.setImageResource(resourceId);
			}
		} else {
			new Handler(mContext.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					loadImageFromResource(imageView, resourceId);
				}
			});
		}
	}

	// TODO: Make an API call that will load the bitmap into place with an
	// animation
	/**
	 * This call prevents any previous loadImage call from loading a Bitmap into the provided ImageView.
	 * 
	 * Please note it will not prevent any future loadImage calls from loading an image into that view.
	 * 
	 * @param imageView
	 * @returns True if an image load was stopped. False on failure.
	 */
	public boolean stopLoadingImage(ImageView imageView) {
		ImageManagerListener imageManagerListener = mViewMapper.removeListener(imageView);

		if (imageManagerListener != null) {
			mReferenceManager.cancelRequest(imageManagerListener);
			return true;
		}

		return false;
	}

	public void stopLoadingImage(ImageManagerListener imageManagerListener) {
		mViewMapper.removeImageView(imageManagerListener);
		mReferenceManager.cancelRequest(imageManagerListener);
	}

	/**
	 * This method will remove all information regarding this image from the cache. This includes any bitmaps currently saved in the memory cache.
	 * 
	 * @param uri
	 *            The file system URI to remove.
	 */
	public static void invalidateFileSystemUri(Context context, String uri) {
		context = context.getApplicationContext();
		ImageCacher.getInstance(context).invalidateFileSystemUri(uri);
	}

	/**
	 * Forces the memory cache to release all references to bitmaps.
	 * 
	 * NOTE: The images in the memcache will not be garbage collected if the app still has references to the bitmaps. For example, if the bitmap is loaded to an {@link ImageView} and the ImageView is still being
	 * referenced.
	 */
	public void clearMemCache() {
		ImageCacher.getInstance(mContext).clearMemCache();
	}

	/**
	 * Sets the maximum size of the memory cache in bytes.<br>
	 * <br>
	 * WARNING: Setting the memory cache size value too high will result in OutOfMemory exceptions. Developers should test their apps thoroughly and modify the value set using this method based on memory consumption and
	 * app performance. A larger cache size means better performance but worse memory usage. A smaller cache size means worse performance but better memory usage.<br>
	 * <br>
	 * The image system will only violate the maximum size specified if a single image is loaded that is larger than the specified maximum size.
	 * 
	 * @param maxSizeInBytes
	 */
	public void setMaximumMemCacheSize(long maxSizeInBytes) {
		ImageCacher.getInstance(mContext).setMaximumMemCacheSize(maxSizeInBytes);
	}

	/**
	 * Sets the maximum size of the memory cache in bytes.<br>
	 * <br>
	 * WARNING: Setting the memory cache size value too high will result in OutOfMemory exceptions. Developers should test their apps thoroughly and modify the value set using this method based on memory consumption and
	 * app performance. A larger cache size means better performance but worse memory usage. A smaller cache size means worse performance but better memory usage.<br>
	 * <br>
	 * The image system will only violate the maximum size specified if a single image is loaded that is larger than the specified maximum size.
	 * 
	 * @param maxSizeInBytes
	 */
	public static void setMaximumMemCacheSize(Context context, long maxSizeInBytes) {
		context = context.getApplicationContext();
		ImageCacher.getInstance(context).setMaximumMemCacheSize(maxSizeInBytes);
	}

	/**
	 * Sets the maximum disk cache size. This value defaults to 50MB. Most applications will probably need much less space.
	 * 
	 * @param maxSizeInBytes
	 * @deprecated The disk cache size should only ever be set on application startup, in your application's "onCreate" method. Please use {@link ImageLoader#setMaximumDiskCacheSize(Context, long)} instead.
	 */
	@Deprecated
	public void setMaximumDiskCacheSize(long maxSizeInBytes) {
		setMaximumDiskCacheSize(mContext, maxSizeInBytes);
	}

	/**
	 * Sets the maximum disk cache size. This value defaults to 50MB. Most applications will probably need much less space.<br>
	 * <br>
	 * You should only be using this method in the onCreate method of your Application object.
	 * 
	 * @param maxSizeInBytes
	 */
	public static void setMaximumDiskCacheSize(Context context, long maxSizeInBytes) {
		ImageCacher.getInstance(context).setMaximumDiskCacheSize(maxSizeInBytes);
	}

	/**
	 * Caches the image at the provided URI into the disk cache. This call is asynchronous and cannot be cancelled once called.<br>
	 * <br>
	 * Ideal use cases for this method:<br>
	 * - Pre-cache large images when the user is likely to display them shortly. This will not increase memory usage, but will drastically speed up image load times.<br>
	 * - Pre-cache ListView images.<br>
	 * <br>
	 * File system URIs will be ignored by the caching system, as these images are already on disk.
	 * 
	 * @param uri
	 * @param applicationContext
	 */
	public void precacheImageToDisk(String uri) {
		precacheImageToDisk(uri, mContext, ImageRequestType.PRECACHE_TO_DISK);
	}

	/**
	 * Caches the image at the provided URI into the disk cache. This call is asynchronous and cannot be cancelled once called.<br>
	 * <br>
	 * Ideal use cases for this method:<br>
	 * - Pre-cache large images when the user is likely to display them shortly. This will not increase memory usage, but will drastically speed up image load times.<br>
	 * - Pre-cache ListView images.<br>
	 * <br>
	 * File system URIs will be ignored by the caching system, as these images are already on disk.
	 * 
	 * @param uri
	 * @param applicationContext
	 */
	// TODO Test what happens if precache image to disk is called with a file system URI.
	public static void precacheImageToDisk(final String uri, Context applicationContext) {
		precacheImageToDisk(uri, applicationContext, ImageRequestType.PRECACHE_TO_DISK);
	}

	private static void precacheImageToDisk(String uri, Context context, ImageRequestType imageRequestType) {
		CacheRequest cacheRequest = new CacheRequest(uri);
		cacheRequest.setImageRequestType(imageRequestType);
		ImageCacher.getInstance(context).getBitmap(cacheRequest, new BlankImageCacherListener());
	}

	public static void precacheImageToDisk(Context context, String uri, PrecacheListener precacheListener) {
		final WeakReference<PrecacheListener> weakPrecacheListener = new WeakReference<PrecacheListener>(precacheListener);

		CacheRequest cacheRequest = new CacheRequest(uri);
		cacheRequest.setImageRequestType(ImageRequestType.PRECACHE_TO_DISK);

		ImageCacher.getInstance(context).getBitmap(cacheRequest, new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
				PrecacheListener precacheListener = weakPrecacheListener.get();
				if (precacheListener != null) {
					precacheListener.onPrecacheComplete();
				}
			}

			@Override
			public void onFailure(String message) {
				PrecacheListener precacheListener = weakPrecacheListener.get();
				if (precacheListener != null) {
					precacheListener.onPrecacheFailed(message);
				}
			}
		});
	}

	public void precacheImageToDiskAndMemory(PrecacheRequest precacheRequest) {
		Options options = precacheRequest.options;
		CacheRequest cacheRequest = new CacheRequest(precacheRequest.uri, getScalingInfo(null, options), options);
		cacheRequest.setImageRequestType(ImageRequestType.PRECACHE_TO_MEMORY);
		ImageCacher.getInstance(mContext).getBitmap(cacheRequest, new BlankImageCacherListener());
	}

	/**
	 * This method must be called from the UI thread.<br>
	 * <br>
	 * Caches the image at the provided URL into both the disk cache and into the memory cache.<br>
	 * <br>
	 * This method call is useful for pre-caching smaller images. If used for a ListView that has many small images, the quality of scrolling will be vastly improved.<br>
	 * <br>
	 * The Width and Height allow you to specify the size of the view that the image will be loaded to. If the image is significantly larger than the provided width and/or height, the image will be scaled down in memory,
	 * allowing for significant improvements to memory usage and performance, at no cost to image detail.
	 * 
	 * @param uri
	 * 
	 * @param bounds
	 *            The expected dimensions of the view in pixels. The width and/or height can be set to null.
	 * 
	 * @param options
	 *            The options used to customize how the view gets precached. Please note that all options relating the image bounds are ignored during this precaching call. The "bounds" object is used instead. Otherwise,
	 *            the options should be identical to the image request that will be performed by the app.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This is thrown if the method is called from off the UI thread.
	 * 
	 * @deprecated This method is now deprecated! Please use {@link ImageLoader#precacheImageToDiskAndMemory(ImageRequest)} instead. The bounds used is now the height and width parameters inside of the options object.
	 *             Please note that this method will not report back to any provided listeners when complete.
	 */
	@Deprecated
	public void precacheImageToDiskAndMemory(String uri, Dimensions bounds, Options options) {
		Options o = new Options();
		o.autoDetectBounds = options.autoDetectBounds;
		o.overrideSampleSize = options.overrideSampleSize;
		o.preferedConfig = options.preferedConfig;
		o.scalingPreference = options.scalingPreference;
		o.useScreenSizeAsBounds = options.useScreenSizeAsBounds;
		o.widthBounds = bounds.width;
		o.heightBounds = bounds.height;

		PrecacheRequest precacheRequest = new PrecacheRequest(uri, o);
		precacheImageToDiskAndMemory(precacheRequest);
	}

	/**
	 * Please use {@link #precacheImageToDiskAndMemory(String, Integer, Integer)}.
	 */
	@Deprecated
	public void precacheImageToDiskAndMemory(String uri, Context applicationContext, Integer width, Integer height) {
		Options options = new Options();
		options.widthBounds = width;
		options.heightBounds = height;

		PrecacheRequest request = new PrecacheRequest(uri, options);
		precacheImageToDiskAndMemory(request);
	}

	public static int convertDpToPixels(Context context, int dp) {
		Resources r = context.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	protected synchronized boolean isDestroyed() {
		return mDestroyed;
	}

	protected Options getDefaultOptions() {
		return mDefaultOptions;
	}

	private void initKeyAndContext(Object key, Context context) {
		mContext = context.getApplicationContext();
		mKey = key;
	}

	void notifyDirectionSwapped(CacheKey cacheKey) {
		ImageCacher.getInstance(mContext).notifyDirectionSwapped(cacheKey);
	}

	void stubReferenceManager(ReferenceManager referenceManager) {
		mReferenceManager = referenceManager;
	}

	private void performImageRequestOnUiThread(final ImageView imageView, final CacheRequest cacheRequest, final Options options, final ImageManagerListener imageManagerListener) {
		if (ThreadChecker.isOnUiThread())
			performImageRequest(imageView, cacheRequest, options, imageManagerListener);
		else {
			new Handler(mContext.getMainLooper()).post(new Runnable() {

				@Override
				public void run() {
					if (!mDestroyed)
						performImageRequest(imageView, cacheRequest, options, imageManagerListener);
				}
			});
		}
	}

	private void performImageRequest(ImageView imageView, CacheRequest cacheRequest, Options options, ImageManagerListener imageManagerListener) {
		if (!cacheRequest.isPrecacheRequest())
			mapImageView(imageView, imageManagerListener);
		setPreLoadImage(imageView, options);

		mReferenceManager.getBitmap(mKey, cacheRequest, imageManagerListener);
	}

	private static void setPreLoadImage(ImageView imageView, Options options) {
		if (imageView != null && options.wipeOldImageOnPreload) {
			if (options.placeholderImageResourceId != null) {
				imageView.setImageResource(options.placeholderImageResourceId);
			} else {
				imageView.setImageBitmap(null);
			}
		}
	}

	/**
	 * Calculates the scaling information needed by the ImageCacher. The scaling info contains either the bounds used for reducing image size, or an override sample size to force manual memory savings.
	 * 
	 * @param imageView
	 *            Depending on the options that are passed in, this imageView's bounds may be auto detected and loaded into the ScalingInfo.
	 * @param options
	 *            The Options lets us know what scaling information we need to retreive, if any.
	 * @return Returns the information the imageCacher needs to figure out how to decode the downloaded image.
	 */
	ScalingInfo getScalingInfo(ImageView imageView, final Options options) {
		ScalingInfo scalingInfo = new ScalingInfo();
		if (options.overrideSampleSize != null) {
			scalingInfo.sampleSize = options.overrideSampleSize;
			return scalingInfo;
		}

		/*
		 * FIXME: It appears as though the width and height bound constraints are not being followed exactly. Review this implementation.
		 */
		Integer width = options.widthBounds;
		Integer height = options.heightBounds;

		if (options.useScreenSizeAsBounds) {
			Dimensions screenSize = mDisplayUtility.getDisplaySize(mContext);
			width = Math.min(screenSize.width, width == null ? screenSize.width : width);
			height = Math.min(screenSize.height, height == null ? screenSize.height : height);
		}

		if (options.autoDetectBounds && imageView != null) {
			Point viewBounds = ViewDimensionsUtil.getImageViewDimensions(imageView);

			width = getBounds(width, viewBounds.x);
			height = getBounds(height, viewBounds.y);
		}

		scalingInfo.width = width;
		scalingInfo.height = height;
		return scalingInfo;
	}

	private static Integer getBounds(Integer currentDimension, int viewDimension) {
		if (viewDimension != -1) {
			if (currentDimension == null) {
				currentDimension = viewDimension;
			} else {
				currentDimension = Math.min(currentDimension, viewDimension);
			}
		}
		return currentDimension;
	}

	private void mapImageView(ImageView view, ImageManagerListener listener) {
		ImageManagerListener oldListener = mViewMapper.removeListener(view);
		if (oldListener != null) {
			mReferenceManager.cancelRequest(oldListener);
		}
		mViewMapper.registerRequest(view, listener);
	}

	/**
	 * Handles loading the bitmap onto the ImageView upon it becoming available from the ImageCacher.
	 * 
	 * @param options
	 * @return
	 */
	private ImageManagerListener getDefaultImageManagerListener(final Options options) {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed(String error) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null && options.unsuccessfulLoadResourceId != null) {
					imageView.setImageResource(options.unsuccessfulLoadResourceId);
				}
			}

			@Override
			public void onImageReceived(ImageResponse imageResponse) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null) {
					imageView.setImageBitmap(imageResponse.getBitmap());
				}
			}
		};
	}

	/**
	 * Sends the Bitmap back to the original caller without loading it to the ImageView first.
	 * 
	 * @param listener
	 * @param listenerOptions
	 * @return
	 */
	private ImageManagerListener getImageManagerListenerWithCallback(final ImageLoaderListener listener, final Options listenerOptions) {
		return new ImageManagerListener() {
			@Override
			public void onLoadImageFailed(String error) {
				ImageView imageView = mViewMapper.removeImageView(this);
				if (imageView != null && listenerOptions.unsuccessfulLoadResourceId != null) {
					imageView.setImageResource(listenerOptions.unsuccessfulLoadResourceId);
				}
				listener.onImageLoadError(error);
			}

			@Override
			public void onImageReceived(ImageResponse imageResponse) {
				ImageView imageView = mViewMapper.removeImageView(this);
				listener.onImageAvailable(imageView, imageResponse.getBitmap(), imageResponse.getImageReturnedFrom());
			}
		};
	}

	/**
	 * This class provides all the options that can be set when making loadImage calls.
	 * 
	 * See the Javadocs for the individual fields for more detail.
	 */
	public static class Options {
		/**
		 * {@link ScalingPreference#LARGER_THAN_VIEW_OR_FULL_SIZE}<br>
		 * This option guarantees that the image being returned will be larger than the view's bounds, or it's maximum size. The image may be scaled down if it is possible to do so without becoming smaller than either of
		 * the provided bounds. The image will not be scaled unless both a width and height bounds are specified.<br>
		 * <br>
		 * {@link ScalingPreference#MATCH_TO_LARGER_DIMENSION}<br>
		 * This option is nearly identical to {@link ScalingPreference#LARGER_THAN_VIEW_OR_FULL_SIZE}. The only difference is that if bounds are provided for only one dimension of the ImageView (ie. width OR height), the
		 * image may be scaled according to that dimension.<br>
		 * <br>
		 * {@link ScalingPreference#MATCH_TO_SMALLER_DIMENSION}<br>
		 * This option is nearly identical to {@link ScalingPreference#LARGER_THAN_VIEW_OR_FULL_SIZE}. They differ in that if bounds are provided for only one dimension of the ImageView (ie. width OR height), the image
		 * may be scaled according to that dimension, and if both width and height are provided, the image will scale to best fit within the bounds (as opposed to the other two options above, which will scale to the
		 * larger of the two dimensions only).<br>
		 * <br>
		 * {@link ScalingPreference#ROUND_TO_CLOSEST_MATCH}<br>
		 * The dimensions of the image returned will be as close to the dimension of the bounds as possible. The bitmap returned may be scaled down to be smaller than the view. This option may degrade image quality, but
		 * often will consume less memory. This option will give preference to the smaller of the two bounds.<br>
		 * <br>
		 * {@link ScalingPreference#SMALLER_THAN_VIEW}<br>
		 * The dimensions of the image being returned is guaranteed to be equal to or smaller than the size of the bounds provided. This guarantees memory savings in the event that images are larger than the ImageViews
		 * they are being loaded into.
		 */
		public static enum ScalingPreference {
			SMALLER_THAN_VIEW, ROUND_TO_CLOSEST_MATCH, LARGER_THAN_VIEW_OR_FULL_SIZE, MATCH_TO_LARGER_DIMENSION, MATCH_TO_SMALLER_DIMENSION
		}

		/**
		 * Forces the image to be decoded with the specified sample size. This will override any other parameters that affect the sample size of the image.<br>
		 * <br>
		 * NOTE: This value, if specified, should always be a positive power of 2. The higher the number provided, the further the image will be scaled down.<br>
		 * <br>
		 * Example: A sample size of 2 will decrease the size of the image by 4. A sample size of 4 will decrease the size of the image by 16.<br>
		 * <br>
		 * Default value: null.
		 */
		public Integer overrideSampleSize = null;

		/**
		 * If specified, this value allows the cacher to conserve memory by estimating the optimal sample size. This works in conjunction with the widthBounds field, so both can be specified at the same time.<br>
		 * <br>
		 * Default value: null.
		 */
		public Integer heightBounds = null;

		/**
		 * If specified, this value allows the cacher to conserve memory by estimating the optimal sample size. This works in conjunction with the heightBounds field, so both can be specified at the same time.<br>
		 * <br>
		 * Default value: null.
		 */
		public Integer widthBounds = null;

		/**
		 * If true, the ImageLoader will attempt to optimize the sample size for the image being returned.<br>
		 * <br>
		 * Default value: true.
		 */
		public boolean autoDetectBounds = true;

		/**
		 * If true, the ImageLoader will select a sample size that will optimize the image size for the size of the screen.<br>
		 * <br>
		 * Default value: true.
		 */
		public boolean useScreenSizeAsBounds = true;

		/**
		 * The ImageLoader has the ability to automatically scale down images according to the bounds of the ImageView provided, or the bounds specified within this options object. This parameter is a flag for the sample
		 * size calculation logic that changes how it chooses sample sizes. See {@link ScalingPreference} for further details.
		 */
		public ScalingPreference scalingPreference = ScalingPreference.MATCH_TO_LARGER_DIMENSION;

		/**
		 * If set to true, the ImageLoader will, before getting the Bitmap, replace the current image within the ImageView with either a null Bitmap or the image resource indicated by the placeholderImageResourceId.<br>
		 * <br>
		 * If set to false, the ImageLoader will only attempt to load the requested Bitmap to the view.
		 */
		public boolean wipeOldImageOnPreload = true;

		/**
		 * The ImageLoader will load the resource at this ID prior to making the image request.<br>
		 * <br>
		 * Default value: null.
		 */
		public Integer placeholderImageResourceId = null;

		/**
		 * In the event that the image load fails, the resource at the provided ID will be loaded into the ImageView.<br>
		 * <br>
		 * Default value: null.
		 */
		public Integer unsuccessfulLoadResourceId = null;

		/**
		 * Modify this value to change the colour format of decoded bitmaps. If set to null, the BitmapFactory will automatically select a colour format.<br>
		 * <br>
		 * This options can be used to manually raise or lower the bit depth of images, which may result in memory savings.<br>
		 * <br>
		 * Default value: null.
		 */
		public Bitmap.Config preferedConfig = null;
	}
}
