package com.xtremelabs.imageutils;

import android.os.Handler;
import android.os.HandlerThread;

class ImageUtilsHandler {

	private static final class Holder {
		static final ImageUtilsHandler INSTANCE = new ImageUtilsHandler();
	}

	public static ImageUtilsHandler getInstance() {
		return Holder.INSTANCE;
	}

	private final Handler mBackgroundThreadHandler;

	private ImageUtilsHandler() {
		HandlerThread handlerThread = new HandlerThread("xl_image_utils_handler");
		handlerThread.start();
		mBackgroundThreadHandler = new Handler(handlerThread.getLooper());
	}

	public void post(Runnable runnable) {
		mBackgroundThreadHandler.post(runnable);
	}
}
