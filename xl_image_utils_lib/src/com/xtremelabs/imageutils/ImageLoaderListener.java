/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface ImageLoaderListener {
	/*
	 * TODO The onImageAvailable should return an ImageResponse containing important data from the request. That object should include the "returnedFrom" parameter.
	 */
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

	// TODO: Include an error type enum in the error response.
	/**
	 * Called in the event the bitmap could not be retreived.
	 */
	public void onImageLoadError(String error);
}
