package com.xtremelabs.imageutils;

public abstract class Prioritizable implements Runnable {
	private boolean mIsCancelled = false;

	public abstract int getTargetPriorityAccessorIndex();

	public abstract Request<?> getRequest();

	public abstract void execute();

	@Override
	public synchronized final void run() {
		if (!isCancelled()) {
			execute();
		}
	}

	public synchronized boolean isCancelled() {
		return mIsCancelled;
	}

	public synchronized void cancel() {
		mIsCancelled = true;
	}
}
