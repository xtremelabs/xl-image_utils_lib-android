package com.xtremelabs.imageutils;

import android.util.Log;

public abstract class Prioritizable implements Runnable {
	private boolean mIsCancelled = false;

	public abstract int getTargetPriorityAccessorIndex();

	public abstract Request<?> getRequest();

	public abstract void execute();

	@Override
	public final void run() {
		if (!isCancelled()) {
			execute();
		}
	}

	public synchronized boolean isCancelled() {
		return mIsCancelled;
	}

	public synchronized void cancel() {
		Log.d("JAMIE", "JAMIE - Managed to cancel the prioritizable!");
		mIsCancelled = true;
	}
}
