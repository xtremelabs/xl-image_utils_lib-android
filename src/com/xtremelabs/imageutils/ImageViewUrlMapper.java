package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.graphics.Bitmap;
import android.widget.ImageView;
public class ImageViewUrlMapper {
	private HashMap<String, List<ImageView>> urlToImageViewsMap = new HashMap<String, List<ImageView>>();
	private HashMap<ImageView, String> imageViewToUrlMap = new HashMap<ImageView, String>();

	// TODO: Confirm that these methods are being called on the UI thread. That way, we can remove the synchronized keywords.
	public synchronized void drawAllImagesForBitmap(Bitmap bitmap, String url) {
		List<ImageView> list = getAndRemoveImageViewsForUrl(url);
		if (list != null) {
			for (ImageView view : list) {
				view.setImageBitmap(bitmap);
			}
		}
	}
	
	public synchronized List<ImageView> getAndRemoveImageViewsForUrl(String url) {
		List<ImageView> views = null;
		views = urlToImageViewsMap.remove(url);
		if (views != null) {
			for (ImageView view : views) {
				imageViewToUrlMap.remove(view);
			}
		}
		return views;
	}

	public synchronized void registerImageViewToUrl(ImageView view, String url) {
		removeFromPreviousUrl(view);
		registerViewAndUrl(view, url);
	}

	public synchronized void removeImageView(ImageView view) {
		removeFromPreviousUrl(view);
	}
	
	public synchronized boolean isImageViewCurrentlyRegistered(ImageView view) {
		if (imageViewToUrlMap.containsKey(view)) {
			return true;
		} else {
			return false;
		}
	}

	private void registerViewAndUrl(ImageView view, String url) {
		imageViewToUrlMap.put(view, url);
		List<ImageView> list = urlToImageViewsMap.get(url);
		if (list == null) {
			list = new ArrayList<ImageView>();
			urlToImageViewsMap.put(url, list);
		}
		list.add(view);
	}

	private void removeFromPreviousUrl(ImageView view) {
		String previousUrl = imageViewToUrlMap.remove(view);
		if (previousUrl != null) {
			List<ImageView> oldList = urlToImageViewsMap.get(previousUrl);
			if (oldList != null) {
				oldList.remove(view);
				if (oldList.isEmpty()) {
					urlToImageViewsMap.remove(previousUrl);
				}
			}
		}
	}
}
