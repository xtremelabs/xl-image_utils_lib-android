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

import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

class ViewDimensionsUtil {
	public static Point getImageViewDimensions(ImageView imageView) {
		Point dimensions = new Point();
		dimensions.x = getDimension(imageView, true);
		dimensions.y = getDimension(imageView, false);
		if (dimensions.x <= 0) {
			dimensions.x = -1;
		}
		if (dimensions.y <= 0) {
			dimensions.y = -1;
		}
		return dimensions;
	}

	private static int getDimension(View view, boolean isWidth) {
		LayoutParams params;
		while (view != null && (params = view.getLayoutParams()) != null) {
			int length = isWidth ? params.width : params.height;
			if (length == LayoutParams.WRAP_CONTENT) {
				return -1;

			} else if (length == LayoutParams.MATCH_PARENT) {
				try {
					view = (View) view.getParent();
				} catch (ClassCastException e) {
					return -1;
				}

			} else {
				return length;
			}
		}

		return -1;
	}
}
