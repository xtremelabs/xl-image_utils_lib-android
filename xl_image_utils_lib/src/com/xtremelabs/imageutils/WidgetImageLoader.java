package com.xtremelabs.imageutils;

import android.content.Context;
import android.util.Log;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageRequest.RequestType;

public class WidgetImageLoader extends ImageLoader {
	WidgetImageLoader(Object imageLoaderClass, Context context) {
		super(imageLoaderClass, context);
	}

	public ImageResponse loadImageSynchronouslyOrQueueNetworkRequest(String uri, Options options, ImageDownloadedListener listener) {
		if (!isDestroyed()) {
			if (options == null) {
				options = getDefaultOptions();
			}

			ScalingInfo scalingInfo = getScalingInfo(null, options);
			ImageRequest imageRequest = new ImageRequest(uri, scalingInfo, options);
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

	public static interface ImageDownloadedListener {
		public void onImageDownloaded();

		public void onImageDownloadFailure();
	}
}
