package com.xtremelabs.imageutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class DisplayUtility {
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static Dimensions getDisplaySize(Context applicationContext) {
		Display display = ((WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Dimensions displaySize;

		if (Build.VERSION.SDK_INT < 13) {
			// These method calls are used before API level 13.
			displaySize = new Dimensions(display.getWidth(), display.getHeight());
		} else {
			Point size = new Point();
			display.getSize(size);
			displaySize = new Dimensions(size.x, size.y);
		}

		return displaySize;
	}
}
