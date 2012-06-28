package com.xtremelabs.imageutils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class DisplayUtility {
	@SuppressWarnings("deprecation")
	public static Point getDisplaySize(Context applicationContext) {
		Display display = ((WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point displaySize = new Point();
		
		if (Build.VERSION.SDK_INT < 13) {
			// These method calls are used before API level 13.
			displaySize.x = display.getWidth();
			displaySize.y = display.getHeight();
		} else {
			display.getSize(displaySize);
		}
		
		return displaySize;
	}
}
