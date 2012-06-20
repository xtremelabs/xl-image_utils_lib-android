package com.xtremelabs.imageutils;

import java.util.HashMap;

import android.widget.ImageView;

class ImageViewReferenceMapper {
	private HashMap<ImageView, ImageReceivedListener> imageViewToListenerMap = new HashMap<ImageView, ImageReceivedListener>();
	private HashMap<ImageReceivedListener, ImageView> listenerToImageViewMap = new HashMap<ImageReceivedListener, ImageView>();

	public synchronized void registerImageViewToListener(ImageView view, ImageReceivedListener listener) {
		imageViewToListenerMap.put(view, listener);
		listenerToImageViewMap.put(listener, view);
	}

	public synchronized ImageView removeImageView(ImageReceivedListener listener) {
		ImageView view = listenerToImageViewMap.remove(listener);
		if (view != null) {
			imageViewToListenerMap.remove(view);
		}
		return view;
	}

	public synchronized ImageReceivedListener removeListener(ImageView view) {
		ImageReceivedListener listener = imageViewToListenerMap.remove(view);
		if (listener != null) {
			listenerToImageViewMap.remove(listener);
		}
		return listener;
	}

	public synchronized boolean isImageViewCurrentlyRegistered(ImageView view) {
		return imageViewToListenerMap.containsKey(view);
	}
}
