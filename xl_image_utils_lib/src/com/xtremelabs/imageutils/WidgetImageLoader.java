package com.xtremelabs.imageutils;

import android.content.Context;
import android.util.Log;

public class WidgetImageLoader extends AbstractImageLoader {
	public WidgetImageLoader(Object imageLoaderClass, Context applicationContext) {
		super(imageLoaderClass, applicationContext);
	}

	public ImageResponse loadImageSynchronouslyFromDiskOrMemory(String uri, Options options) {
		if (!isDestroyed()) {
			if (options == null) {
				options = getDefaultOptions();
			}

			ScalingInfo scalingInfo = getScalingInfo(null, options);
			ImageRequest imageRequest = new ImageRequest(uri, scalingInfo, options);
			return ImageCacher.getInstance(getApplicationContext()).getBitmapSynchronouslyFromDiskOrMemory(imageRequest);
		} else {
			Log.w(TAG, "WARNING: loadImageSynchronouslyFromDiskOrMemory was called after the ImageLoader was destroyed.");
			return null;
		}
	}
}
