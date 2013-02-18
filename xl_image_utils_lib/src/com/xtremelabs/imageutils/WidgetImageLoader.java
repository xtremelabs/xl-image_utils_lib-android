package com.xtremelabs.imageutils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.xtremelabs.imageutils.ImageCacher.ImageCacherListener;
import com.xtremelabs.imageutils.ImageLoader.Options;

public class WidgetImageLoader {
	private final ImageLoader mImageLoader;
	private boolean mDestroyed = false;
	private final Context mContext;

	WidgetImageLoader(Object imageLoaderClass, Context context) {
		mImageLoader = new ImageLoader(imageLoaderClass, context);
		mContext = context;
	}

	// TODO This does not handle bad URIs. The system just crashes.
	public ImageResponse loadImageSynchronouslyOrQueueNetworkRequest(String uri, Options options, ImageDownloadedListener listener) {
		if (!isDestroyed()) {
			if (options == null) {
				options = mImageLoader.getDefaultOptions();
			}

			ScalingInfo scalingInfo = mImageLoader.getScalingInfo(null, options);
			CacheRequest imageRequest = new CacheRequest(uri, scalingInfo, options);
			imageRequest.setRequestType(ImageRequestType.PRECACHE_TO_DISK);
			return ImageCacher.getInstance(mContext).getBitmapSynchronouslyFromDiskOrMemory(imageRequest, getImageCacherListener(listener));
		} else {
			Log.w(ImageLoader.TAG, "WARNING: loadImageSynchronouslyFromDiskOrMemory was called after the ImageLoader was destroyed.");
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

	public synchronized void destroy() {
		mDestroyed = true;

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mImageLoader.destroy();
			}
		});
	}

	private synchronized boolean isDestroyed() {
		return mDestroyed;
	}

	public static interface ImageDownloadedListener {
		public void onImageDownloaded();

		public void onImageDownloadFailure();
	}
}
