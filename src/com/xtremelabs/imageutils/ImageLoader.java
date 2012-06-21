package com.xtremelabs.imageutils;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * All loadImage requests MUST BE MADE FROM THE UI THREAD. All calls are thread safe and ListView safe.
 * 
 * @author Jamie Halpern
 */
public class ImageLoader {
	private ImageViewReferenceMapper viewMapper = new ImageViewReferenceMapper();
	private ImageManager imageManager;
	private Context applicationContext;
	private Object key;

	private int screenWidth, screenHeight;

	public ImageLoader(Activity activity) {
		if (activity == null) {
			throw new IllegalArgumentException("Activity cannot be null!");
		}
		initKeyAndAppContext(activity, activity.getApplicationContext());
	}

	public ImageLoader(Fragment fragment) {
		if (fragment == null) {
			throw new IllegalArgumentException("Fragment cannot be null!");
		}
		initKeyAndAppContext(fragment, fragment.getActivity().getApplicationContext());
	}

	@SuppressWarnings("deprecation")
	private void initKeyAndAppContext(Object key, Context applicationContext) {
		this.applicationContext = applicationContext;
		this.key = key;
		imageManager = ImageManager.getInstance(applicationContext);
		Display display = ((WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		screenHeight = display.getHeight();
		screenWidth = display.getWidth();
	}

	/**
	 * This MUST BE CALLED when your activity/fragment/etc. is destroyed.
	 */
	public void onDestroy() {
		ImageManager.getInstance(applicationContext).removeListenersForKey(key);
	}

	public void loadImage(ImageView imageView, String url, Options options) {
		if (options == null) {
			options = Options.getRecommendedOptions();
		}

		ImageReceivedListener listener = getDefaultImageReceivedListener(options);

		registerImageView(imageView, listener);

		if (options.placeholderImageResourceId != null) {
			imageView.setImageResource(options.placeholderImageResourceId);
		} else {
			imageView.setImageBitmap(null);
		}

		if (!tryGetBitmapWithScaling(imageView, url, options, listener)) {
			imageManager.getBitmap(key, url, listener);
		}
	}

	public void loadImage(ImageView imageView, String url, Options options, final ImageLoadingListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("You cannot pass in a null ImageLoadingListener.");
		}

		if (options == null) {
			options = Options.getRecommendedOptions();
		}

		final Options listenerOptions = options;
		ImageReceivedListener imageReceivedListener = new ImageReceivedListener() {
			@Override
			public void onLoadImageFailed() {
				ImageView imageView = viewMapper.removeImageView(this);
				if (listenerOptions.unsuccessfulLoadResourceId != null) {
					imageView.setImageResource(listenerOptions.unsuccessfulLoadResourceId);
				}
				listener.onImageLoadError();
			}

			@Override
			public void onImageReceived(Bitmap bitmap) {
				ImageView imageView = viewMapper.removeImageView(this);
				if (imageView != null) {
					listener.onImageAvailable(imageView, bitmap);
				}
			}
		};

		registerImageView(imageView, imageReceivedListener);

		if (options.placeholderImageResourceId != null) {
			imageView.setImageResource(options.placeholderImageResourceId);
		} else {
			imageView.setImageBitmap(null);
		}

		if (!tryGetBitmapWithScaling(imageView, url, options, imageReceivedListener)) {
			imageManager.getBitmap(key, url, imageReceivedListener);
		}
	}

	public Point getImageDimensions(String url) throws FileNotFoundException {
		return ImageCacher.getInstance(applicationContext).getImageDimensions(url);
	}

	public void clearMemCache() {
		ImageCacher.getInstance(applicationContext).clearMemCache();
	}

	public void setMemCacheSize(int size) {
		ImageCacher.getInstance(applicationContext).setMemCacheSize(size);
	}

	public void precacheImageToMemory(String url, Context applicationContext, Integer width, Integer height) {
		imageManager.getBitmap(applicationContext, url, getBlankImageReceivedListener(), width, height);
	}

	public static void precacheImage(String url, Context applicationContext) {
		ImageCacher.getInstance(applicationContext).precacheImage(url);
	}

	private boolean tryGetBitmapWithScaling(ImageView imageView, String url, final Options options, ImageReceivedListener listener) {
		if (options.overrideSampleSize != null) {
			imageManager.getBitmap(key, url, listener, options.overrideSampleSize);
			return true;
		}

		Integer width = options.widthBounds;
		Integer height = options.heightBounds;

		if (options.useScreenSizeAsBounds) {
			width = Math.min(screenWidth, width == null ? screenWidth : width);
			height = Math.min(screenHeight, height == null ? screenHeight : height);
		}

		if (options.autoDetectBounds) {
			Point imageBounds = getImageViewDimensions(imageView);
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

		if (width != null || height != null) {
			imageManager.getBitmap(key, url, listener, width, height);
			return true;
		}

		return false;
	}

	private ImageReceivedListener getDefaultImageReceivedListener(final Options options) {
		ImageReceivedListener listener = new ImageReceivedListener() {
			@Override
			public void onLoadImageFailed() {
				ImageView imageView = viewMapper.removeImageView(this);
				if (imageView != null) {
					if (options.unsuccessfulLoadResourceId != null) {
						imageView.setImageResource(options.unsuccessfulLoadResourceId);
					}
				}
			}

			@Override
			public void onImageReceived(Bitmap bitmap) {
				ImageView imageView = viewMapper.removeImageView(this);
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		};
		return listener;
	}

	private void registerImageView(ImageView view, ImageReceivedListener listener) {
		ImageReceivedListener oldListener = viewMapper.removeListener(view);
		if (oldListener != null) {
			// TODO: Cancel old calls here!
			// imageManager.cancelRequest(oldListener);
		}
		viewMapper.registerImageViewToListener(view, listener);
	}

	private Point getImageViewDimensions(ImageView imageView) {
		Point dimensions = new Point();
		dimensions.x = getDimensions(imageView, true);
		dimensions.y = getDimensions(imageView, false);
		if (dimensions.x <= 0) {
			dimensions.x = -1;
		}
		if (dimensions.y <= 0) {
			dimensions.y = -1;
		}
		return dimensions;
	}

	private int getDimensions(ImageView imageView, boolean isWidth) {
		LayoutParams params = imageView.getLayoutParams();
		int length = isWidth ? params.width : params.height;
		if (length == LayoutParams.WRAP_CONTENT) {
			return -1;
		} else if (length == LayoutParams.MATCH_PARENT) {
			try {
				return getParentDimensions((ViewGroup) imageView.getParent(), isWidth);
			} catch (ClassCastException e) {
				return -1;
			}
		} else {
			return length;
		}
	}

	private int getParentDimensions(ViewGroup parent, boolean isWidth) {
		LayoutParams params;
		if (parent == null || (params = parent.getLayoutParams()) == null) {
			return -1;
		}
		int length = isWidth ? params.width : params.height;
		if (length == LayoutParams.WRAP_CONTENT) {
			return -1;
		} else if (length == LayoutParams.MATCH_PARENT) {
			try {
				return getParentDimensions((ViewGroup) parent.getParent(), isWidth);
			} catch (ClassCastException e) {
				return -1;
			}
		} else {
			return length;
		}
	}

	private ImageReceivedListener getBlankImageReceivedListener() {
		return new ImageReceivedListener() {
			@Override
			public void onLoadImageFailed() {
				viewMapper.removeImageView(this);
			}

			@Override
			public void onImageReceived(Bitmap bitmap) {
				viewMapper.removeImageView(this);
			}
		};
	}

	public static class Options {
		public Integer overrideSampleSize = null;
		public Integer heightBounds = null;
		public Integer widthBounds = null;
		public boolean autoDetectBounds = false;
		public boolean useScreenSizeAsBounds = false;
		public Integer placeholderImageResourceId = null;
		public Integer unsuccessfulLoadResourceId = null;

		public static Options getRecommendedOptions() {
			Options o = new Options();
			o.autoDetectBounds = true;
			o.useScreenSizeAsBounds = true;
			return o;
		}
	}
}
