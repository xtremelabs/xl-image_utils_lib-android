package com.xtremelabs.imageutils;

import android.widget.ImageView;

/**
 * This class is responsible for mapping responses from the LifecycleReferenceManager back to the ImageViews that were originally passed in.
 */
class ImageViewReferenceMapper {
	private final TwoWayHashMap<ImageView, ImageManagerListener> map = new TwoWayHashMap<ImageView, ImageManagerListener>();

	public synchronized void registerImageViewToListener(ImageView view, ImageManagerListener listener) {
		map.put(view, listener);
	}

	public synchronized ImageView removeImageView(ImageManagerListener listener) {
		ImageView view = map.removeSecondaryItem(listener);
		return view;
	}

	public synchronized ImageManagerListener removeListener(ImageView view) {
		return map.removePrimaryItem(view);
	}
}
