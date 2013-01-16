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

import android.widget.ImageView;

/**
 * This class is responsible for mapping responses from the LifecycleReferenceManager back to the ImageViews that were originally passed in.
 */
class ImageViewReferenceMapper {
	private final TwoWayHashMap<ImageView, ImageManagerListener> map = new TwoWayHashMap<ImageView, ImageManagerListener>();

	public synchronized void registerRequest(ImageView view, ImageManagerListener listener) {
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
