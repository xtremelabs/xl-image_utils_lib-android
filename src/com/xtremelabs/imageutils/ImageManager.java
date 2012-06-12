package com.xtremelabs.imageutils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.functionx.viggle.interfaces.ImageReceivedListener;
import com.xtremelabs.utilities.network.ApiException;
import com.xtremelabs.utilities.network.ApiListener;
import com.xtremelabs.utilities.network.RequestException;

public class ImageManager {
	private ImageListenerMapper listenerHelper = new ImageListenerMapper();
	private Handler handler;
	private static ImageManager imageManager;

	private ImageManager(Looper mainLooper) {
		handler = new Handler(mainLooper);
	}

	// TODO: Change from the "context" being passed in to the "mainLooper" being passed in.
	public synchronized static ImageManager getInstance(Context context) {
		if (imageManager == null) {
			imageManager = new ImageManager(context.getMainLooper());
		}
		return imageManager;
	}

	public void getBitmap(Activity activity, final String url, ImageReceivedListener listener) {
		if (StringUtils.isBlank(url)) {
			listener.onLoadImageFailed();
			return;
		}
		
		CustomImageListener customImageListener = new CustomImageListener(url);
		
		synchronized (this) {
			if (!listenerHelper.isListenerRegistered(listener)) {
				listenerHelper.registerNewListener(listener, activity);
			}
			listenerHelper.linkUrlToListener(url, customImageListener, listener);
		}
		
		Bitmap bitmap = ImageCache.getBitmap(activity.getApplicationContext(), url, customImageListener);
		if (bitmap != null) {
			replyToAllListenersForUrl(bitmap, url);
		}
	}
	
	public synchronized void removeListenersForActivity(Activity activity) {
		listenerHelper.removeAllEntriesForActivity(activity);
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

	public synchronized Bitmap getCachedImage(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		} else {
			return ImageCache.getBitmapDirectlyFromCache(url.trim());
		}
	}

	public class CustomImageListener implements ApiListener<Bitmap> {
		private String url;
		private boolean cancelled = false;

		public CustomImageListener(String url) {
			this.url = url;
		}

		@Override
		public void onSuccess(Bitmap bitmap) {
			onBitmapAvailableForUrl(bitmap, url);
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void onFailure(RequestException e) {
			onFailureForUrl(url);
		}

		@Override
		public void onFailure(ApiException e) {
			onFailureForUrl(url);
		}
		
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}
	}
}
