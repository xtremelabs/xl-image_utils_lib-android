package com.xtremelabs.imageutils;

import android.graphics.Point;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

class ViewDimensionsUtil {
	public static Point getImageViewDimensions(ImageView imageView) {
		Point dimensions = new Point();
		dimensions.x = getDimensions(imageView, true);
		dimensions.y = getDimensions(imageView, false);
		if (dimensions.x <= 0) {
			dimensions.x = -1;
		}
		if (dimensions.y <= 0) {
			dimensions.y = -1;
		}
		return dimensions;
	}

	private static int getDimensions(ImageView imageView, boolean isWidth) {
		LayoutParams params = imageView.getLayoutParams();
		if (params == null) {
			return -1;
		}
		int length = isWidth ? params.width : params.height;
		if (length == LayoutParams.WRAP_CONTENT) {
			return -1;
		} else if (length == LayoutParams.MATCH_PARENT) {
			try {
				return getParentDimensions((ViewGroup) imageView.getParent(), isWidth);
			} catch (ClassCastException e) {
				return -1;
			}
		} else {
			return length;
		}
	}

	private static int getParentDimensions(ViewGroup parent, boolean isWidth) {
		LayoutParams params;
		if (parent == null || (params = parent.getLayoutParams()) == null) {
			return -1;
		}
		int length = isWidth ? params.width : params.height;
		if (length == LayoutParams.WRAP_CONTENT) {
			return -1;
		} else if (length == LayoutParams.MATCH_PARENT) {
			try {
				return getParentDimensions((ViewGroup) parent.getParent(), isWidth);
			} catch (ClassCastException e) {
				return -1;
			}
		} else {
			return length;
		}
	}
}
