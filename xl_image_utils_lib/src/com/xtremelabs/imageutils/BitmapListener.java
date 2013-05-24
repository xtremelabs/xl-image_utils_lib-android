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

/**
 * This class can be registered with image requests if the status of an image request is required.
 */
public abstract class BitmapListener {
	private final ImageLoaderListener mImageLoaderListener;

	public BitmapListener() {
		mImageLoaderListener = new ImageLoaderListener() {
			@Override
			public void onImageLoadError(String error) {
				BitmapListener.this.onImageLoadError(error);
			}

			@Override
			public void onImageAvailable(ImageView imageView, Bitmap bitmap, ImageReturnedFrom returnedFrom) {
				BitmapListener.this.onImageAvailable(bitmap, returnedFrom);
			}
		};
	}

	ImageLoaderListener getImageLoaderListener() {
		return mImageLoaderListener;
	}

	public abstract void onImageAvailable(Bitmap bitmap, ImageReturnedFrom returnedFrom);

	public abstract void onImageLoadError(String error);
}
