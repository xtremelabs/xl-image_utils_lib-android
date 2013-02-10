package com.xtremelabs.imageutils;

public abstract class Prioritizable implements Runnable {
	public boolean mIsCancelled = false;

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
		mIsCancelled = true;
	}
}
