package com.xtremelabs.imageutils;

public interface AsyncOperationsObserver {
	public void onImageDecodeRequired(DecodeSignature decodeSignature);

	public int getSampleSize(ImageRequest imageRequest);

	public boolean isNetworkRequestPending(String uri);

	public boolean isDecodeRequestPending(DecodeSignature decodeSignature);

	public void onImageDetailsRequired(String uri);
}
