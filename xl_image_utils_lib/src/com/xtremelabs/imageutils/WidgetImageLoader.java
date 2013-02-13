package com.xtremelabs.imageutils;

import android.content.Context;
import android.util.Log;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.CacheRequest.RequestType;

public class WidgetImageLoader extends ImageLoader {
	WidgetImageLoader(Object imageLoaderClass, Context context) {
		super(imageLoaderClass, context);
	}

	// TODO This does not handle bad URIs. The system just crashes.
	public ImageResponse loadImageSynchronouslyOrQueueNetworkRequest(String uri, Options options, ImageDownloadedListener listener) {
		if (!isDestroyed()) {
			if (options == null) {
				options = getDefaultOptions();
			}

			ScalingInfo scalingInfo = getScalingInfo(null, options);
			CacheRequest imageRequest = new CacheRequest(uri, scalingInfo, options);
			imageRequest.setRequestType(RequestType.CACHE_TO_DISK);
			return ImageCacher.getInstance(getApplicationContext()).getBitmapSynchronouslyFromDiskOrMemory(imageRequest, getImageCacherListener(listener));
		} else {
			Log.w(TAG, "WARNING: loadImageSynchronouslyFromDiskOrMemory was called after the ImageLoader was destroyed.");
			return null;
		}
	}

	private ImageCacherListener getImageCacherListener(final ImageDownloadedListener listener) {
		return new ImageCacherListener() {
			@Override
			public void onImageAvailable(ImageResponse imageResponse) {
				listener.onImageDownloaded();
			}

			@Override
			public void onFailure(String message) {
				listener.onImageDownloadFailure();
			}
		};
	}

	@Override
	public void destroy() {
		// FIXME We need to implement the destroy method for widgets, and this needs to work off the UI thread.
	}

	public static interface ImageDownloadedListener {
		public void onImageDownloaded();

		public void onImageDownloadFailure();
	}
}
