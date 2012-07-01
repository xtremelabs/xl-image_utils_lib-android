package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface ImageLoaderListener {
	// TODO: Consider making the onImageAvailable return a boolean indicating whether or not the loader should place the bitmap into the imageview.
	/**
	 * This method provides you with the {@link ImageView} and {@link Bitmap} from your ImageLoader's loadImage request.
	 * 
	 * When this method is called, the bitmap has not yet been loaded into your {@link ImageView}.
	 * 
	 * @param imageView
	 *            The {@link ImageView} that was used for your original request.
	 * @param bitmap
	 *            The bitmap that was retreived.
	 * @param isFromMemoryCache
	 *            A flag indicating whether or not the bitmap was retreived synchronously from the cache.
	 */
	public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom);

	/**
	 * Called in the event the bitmap could not be retreived.
	 */
	public void onImageLoadError();
}
