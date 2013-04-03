package com.xtremelabs.imageutils;

public interface PrecacheListener {
	public void onPrecacheComplete();

	public void onPrecacheFailed(String message);
}
