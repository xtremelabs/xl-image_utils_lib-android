package com.xtremelabs.imageutils;

import java.util.HashMap;

import android.widget.ImageView;

/**
 * This class is responsible for mapping responses from the LifecycleReferenceManager back to the ImageViews that were originally passed in.
 * 
 * @author Jamie Halpern
 */
class ImageViewReferenceMapper {
	private HashMap<ImageView, ImageManagerListener> imageViewToListenerMap = new HashMap<ImageView, ImageManagerListener>();
	private HashMap<ImageManagerListener, ImageView> listenerToImageViewMap = new HashMap<ImageManagerListener, ImageView>();

	public synchronized void registerImageViewToListener(ImageView view, ImageManagerListener listener) {
		imageViewToListenerMap.put(view, listener);
		listenerToImageViewMap.put(listener, view);
	}

	public synchronized ImageView removeImageView(ImageManagerListener listener) {
		ImageView view = listenerToImageViewMap.remove(listener);
		if (view != null) {
			imageViewToListenerMap.remove(view);
		}
		return view;
	}

	public synchronized ImageManagerListener removeListener(ImageView view) {
		ImageManagerListener listener = imageViewToListenerMap.remove(view);
		if (listener != null) {
			listenerToImageViewMap.remove(listener);
		}
		return listener;
	}

	public synchronized boolean isImageViewCurrentlyRegistered(ImageView view) {
		return imageViewToListenerMap.containsKey(view);
	}
}
