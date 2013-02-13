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

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.xtremelabs.imageutils.ImageRequest.RequestType;
import com.xtremelabs.imageutils.ThreadChecker.CalledFromWrongThreadException;

public abstract class AbstractImageLoader {
	public static final String TAG = "ImageLoader";

	private final ImageViewReferenceMapper mViewMapper = new ImageViewReferenceMapper();
	private ReferenceManager mReferenceManager;
	private Context mApplicationContext;
	private Object mKey;
	private boolean mDestroyed = false;

	private Options mDefaultOptions = new Options();

	// TODO Cancelled network calls should still save the downloaded image to disk.
	// TODO Have an API call that can get a bitmap without an ImageView.
	// TODO Make the disk thread pool a priority pool so that preloaded images take lower priority until directly requested.

	/**
	 * Instantiates a new {@link ImageLoader} that maps all requests to the provided {@link Activity}.
	 * 
	 * @param key
	 * @param applicationContext
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	protected AbstractImageLoader(Object key, Context applicationContext) {
		ThreadChecker.throwErrorIfOffUiThread();

		if (key == null) {
			throw new IllegalArgumentException("Key inside the ImageLoader cannot be null!");
		}
		initKeyAndAppContext(key, applicationContext);
		mReferenceManager = LifecycleReferenceManager.getInstance(applicationContext);
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

		mDestroyed = true;

		List<ImageManagerListener> listeners = mReferenceManager.cancelRequestsForKey(mKey);
		if (listeners != null) {
			for (ImageManagerListener listener : listeners) {
				mViewMapper.removeImageView(listener);
			}
		}
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

	// TODO Write a loadImage call that accepts a URI object, as well as a File object.
	// TODO All image requests coming in to the ImageLoader should be ImageRequest objects.

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
	/*
	 * FIXME Potential memory leak - This method is not synchronized. If onDestroy is called while this is running, it is possible that references will be retained to the Activity or Fragment. Review this for all
	 * loadImage calls.
	 */
	/*
	 * TODO This should just be calling the most advanced loadImage call possible.
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
		if (!mDestroyed) {
			if (options == null) {
				options = mDefaultOptions;
			}

			ImageManagerListener imageManagerListener;
			if (listener == null) {
				imageManagerListener = getDefaultImageManagerListener(options);
			} else {
				imageManagerListener = getImageManagerListenerWithCallback(listener, options);
			}

			performImageRequestOnUiThread(imageView, uri, options, imageManagerListener);
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
			if (!mDestroyed) {
				ThreadChecker.throwErrorIfOffUiThread();

				mViewMapper.removeListener(imageView);
				imageView.setImageResource(resourceId);
			}
		} else {
			new Handler(mApplicationContext.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					loadImageFromResource(imageView, resourceId);
				}
			});
		}
	}

	// TODO: Make an API call that will load the bitmap into place with an
	// animation

	// TODO: Return a boolean indicating whether an image load was actually
	// stopped.
	// TODO: Think about this call.
	// TODO: Have an isLoadingImage call.
	/**
	 * This call prevents any previous loadImage call from loading a Bitmap into the provided ImageView.
	 * 
	 * Please note it will not prevent any future loadImage calls from loading an image into that view.
	 * 
	 * This is useful if you have a ListView that occasionally needs images from the ImageLoader, and other times from a resources file. Before loading the resource file's image, you would call this method.
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

	// TODO Allow for the invalidation of URIs in general (not just file system URIs)
	/*
	 * TODO Allow a check for a modified file that may already be within the caching system
	 * 
	 * - Check headers for URLs
	 * 
	 * - Last modified time MAY be correct as of ICS.
	 */
	// TODO See if last modified time can be used in the disk system.

	/**
	 * This method will remove all information regarding this image from the cache. This includes any bitmaps currently saved in the memory cache.
	 * 
	 * @param uri
	 *            The file system URI to remove.
	 */
	public static void invalidateFileSystemUri(Context applicationContext, String uri) {
		if (!(applicationContext instanceof Application)) {
			applicationContext = applicationContext.getApplicationContext();
		}

		ImageCacher.getInstance(applicationContext).invalidateFileSystemUri(uri);
	}

	/**
	 * Forces the memory cache to release all references to bitmaps.
	 * 
	 * NOTE: The images in the memcache will not be garbage collected if the app still has references to the bitmaps. For example, if the bitmap is loaded to an {@link ImageView} and the ImageView is still being
	 * referenced.
	 */
	public void clearMemCache() {
		ImageCacher.getInstance(mApplicationContext).clearMemCache();
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
		ImageCacher.getInstance(mApplicationContext).setMaximumMemCacheSize(maxSizeInBytes);
	}

	/**
	 * Sets the maximum disk cache size. This value defaults to 50MB. Most applications will probably need much less space.
	 * 
	 * @param maxSizeInBytes
	 */
	public void setMaximumDiskCacheSize(long maxSizeInBytes) {
		ImageCacher.getInstance(mApplicationContext).setMaximumDiskCacheSize(maxSizeInBytes);
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
	public void precacheImageToDisk(final String uri) {
		// if (ThreadChecker.isOnUiThread()) {
		// ImageRequest imageRequest = new ImageRequest(uri);
		// imageRequest.setRequestType(RequestType.CACHE_TO_DISK);
		// ImageCacher.getInstance(mApplicationContext).precacheImageToDisk(imageRequest);
		// } else {
		// new Handler(mApplicationContext.getMainLooper()).post(new Runnable() {
		// @Override
		// public void run() {
		// precacheImageToDisk(uri, mApplicationContext);
		// }
		// });
		// }
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
		// if (!(applicationContext instanceof Application)) {
		// applicationContext = applicationContext.getApplicationContext();
		// }
		//
		// if (ThreadChecker.isOnUiThread()) {
		// ImageRequest imageRequest = new ImageRequest(uri);
		// imageRequest.setRequestType(RequestType.CACHE_TO_DISK);
		// ImageCacher.getInstance(applicationContext).precacheImageToDisk(imageRequest);
		// } else {
		// final Context finalContext = applicationContext;
		// new Handler(applicationContext.getMainLooper()).post(new Runnable() {
		// @Override
		// public void run() {
		// precacheImageToDisk(uri, finalContext);
		// }
		// });
		// }
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
	 */
	// TODO The URI/Bounds/Options should be packaged together as an ImageRequest object
	// FIXME Null bounds will crash the app.
	public void precacheImageToDiskAndMemory(String uri, Dimensions bounds, com.xtremelabs.imageutils.ImageLoader.Options options) {
		// TODO: Replace the width and height with options?
		ThreadChecker.throwErrorIfOffUiThread();

		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.height = bounds.height;
		scalingInfo.width = bounds.width;

		// ImageRequest imageRequest = new ImageRequest(uri, scalingInfo, options == null ? mDefaultOptions : options);
		// mReferenceManager.getBitmap(mApplicationContext, imageRequest, getBlankImageManagerListener());
	}

	/**
	 * Please use {@link #precacheImageToDiskAndMemory(String, Integer, Integer)}.
	 */
	@Deprecated
	public void precacheImageToDiskAndMemory(String uri, Context applicationContext, Integer width, Integer height) {
		// TODO: Replace the width and height with options?
		ThreadChecker.throwErrorIfOffUiThread();

		ScalingInfo scalingInfo = new ScalingInfo();
		scalingInfo.height = height;
		scalingInfo.width = width;

		ImageRequest imageRequest = new ImageRequest(uri, scalingInfo);
		mReferenceManager.getBitmap(applicationContext, imageRequest, getBlankImageManagerListener());
	}

	protected boolean isDestroyed() {
		return mDestroyed;
	}

	protected Options getDefaultOptions() {
		return mDefaultOptions;
	}

	protected Context getApplicationContext() {
		return mApplicationContext;
	}

	private void initKeyAndAppContext(Object key, Context applicationContext) {
		mApplicationContext = applicationContext;
		mKey = key;
	}

	void stubReferenceManager(ReferenceManager referenceManager) {
		mReferenceManager = referenceManager;
	}

	private void performImageRequestOnUiThread(final ImageView imageView, final String uri, final Options options, final ImageManagerListener imageManagerListener) {
		if (ThreadChecker.isOnUiThread())
			performImageRequest(imageView, uri, options, imageManagerListener);
		else {
			new Handler(mApplicationContext.getMainLooper()).post(new Runnable() {

				@Override
				public void run() {
					if (!mDestroyed)
						performImageRequest(imageView, uri, options, imageManagerListener);
				}
			});
		}
	}

	private void performImageRequest(ImageView imageView, String uri, Options options, ImageManagerListener imageManagerListener) {
		// mapImageView(imageView, imageManagerListener);
		// setPreLoadImage(imageView, options);

		// ScalingInfo scalingInfo = getScalingInfo(imageView, options);

		// ImageRequest imageRequest = new ImageRequest(uri, scalingInfo, options);
		// mReferenceManager.getBitmap(mKey, imageRequest, imageManagerListener);
	}

	private void setPreLoadImage(ImageView imageView, Options options) {
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
			Dimensions screenSize = DisplayUtility.getDisplaySize(mApplicationContext);
			width = Math.min(screenSize.width, width == null ? screenSize.width : width);
			height = Math.min(screenSize.height, height == null ? screenSize.height : height);
		}

		if (options.autoDetectBounds && imageView != null) {
			Point imageBounds = ViewDimensionsUtil.getImageViewDimensions(imageView);
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
			public void onImageReceived(ImageResponse imageResponse) {
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
		// TODO Research into the Android naming conventions for ScaleTypes.
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
		/*
		 * FIXME The SizeEstimatingMemCache does not take into account pixel format for size estimations.
		 */
		public Bitmap.Config preferedConfig = null;
	}
}
