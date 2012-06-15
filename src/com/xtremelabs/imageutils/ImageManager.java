package com.xtremelabs.imageutils;

import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.app.Fragment;

import com.xtremelabs.imageutils.ImageCacher.ImageRequestListener;

public class ImageManager {
	private ImageListenerMapper listenerHelper = new ImageListenerMapper();
	private Handler handler;
	private static ImageManager imageManager;
	private ImageCacher imageCacher;

	private ImageManager(Context context) {
		imageCacher = ImageCacher.getInstance(context);
		handler = new Handler(context.getMainLooper());
	}

	// TODO: Change from the "context" being passed in to the "mainLooper" being passed in.
	public synchronized static ImageManager getInstance(Context context) {
		if (!(context instanceof Application)) {
			throw new IllegalArgumentException("The context passed in must be an application context!");
		}
		
		if (imageManager == null) {
			imageManager = new ImageManager(context);
		}
		return imageManager;
	}
	
	public void getBitmap(Activity activity, final String url, ImageReceivedListener listener) {
		getBitmap((Object) activity, url, listener);
	}
	
	public void getBitmap(Fragment fragment, final String url, ImageReceivedListener listener) {
		getBitmap((Object) fragment, url, listener);
	}

	public void getBitmap(Object key, final String url, ImageReceivedListener listener) {
		if (GeneralUtils.isStringBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}

		CustomImageListener customImageListener = new CustomImageListener(url);

		synchronized (this) {
			if (!listenerHelper.isListenerRegistered(listener)) {
				listenerHelper.registerNewListener(listener, key);
			}
			listenerHelper.linkUrlToListener(url, customImageListener, listener);
		}

		Bitmap bitmap = imageCacher.getBitmap(url, customImageListener);
		if (bitmap != null) {
			replyToAllListenersForUrl(bitmap, url);
		}
	}

	public synchronized void removeListenersForKey(Object key) {
		listenerHelper.removeAllEntriesForObject(key);
	}

	private void onBitmapAvailableForUrl(final Bitmap bitmap, final String url) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				replyToAllListenersForUrl(bitmap, url);
			}
		});
	}

	private void onFailureForUrl(final String url) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				replyToAllListenersWithFailureForUrl(url);
			}
		});
	}

	private void replyToAllListenersForUrl(Bitmap bitmap, String url) {
		List<ImageReceivedListener> list = listenerHelper.getAndRemoveListenersForUrl(url);
		if (list != null) {
			for (ImageReceivedListener listener : list) {
				listener.onImageReceived(bitmap, url);
			}
		}
	}

	private void replyToAllListenersWithFailureForUrl(String url) {
		List<ImageReceivedListener> list = listenerHelper.getAndRemoveListenersForUrl(url);
		if (list != null) {
			for (ImageReceivedListener listener : list) {
				listener.onLoadImageFailed();
			}
		}
	}
	
	public class CustomImageListener extends ImageRequestListener {
		private String url;
		
		public CustomImageListener(String url) {
			this.url = url;
		}
		
		@Override
		public void onImageAvailable(Bitmap bitmap) {
			onBitmapAvailableForUrl(bitmap, url);
		}

		@Override
		public void onFailure(String message) {
			onFailureForUrl(url);
		}
		
		public void setCancelled(boolean cancelled) {
			if (cancelled) {
				imageCacher.cancelRequestForBitmap(url, this);
			}
		}
	}
}
