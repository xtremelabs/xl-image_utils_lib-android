package com.xtremelabs.imageutils;

import java.util.List;

public class ImagePrecacheAssistant {
	private enum Direction {
		DOWN, UP
	}

	private static final int DEFAULT_MEM_CACHE_RANGE = 4;
	private static final int DEFAULT_DISK_CACHE_RANGE = 10;

	private int mMemCacheRange = DEFAULT_MEM_CACHE_RANGE;
	private int mDiskCacheRange = DEFAULT_DISK_CACHE_RANGE;

	private final AbstractImageLoader mImageLoader;
	private final PrecacheInformationProvider mPrecacheInformationProvider;

	private int mCurrentPosition = 0;
	private Direction mCurrentDirection = Direction.UP;

	public ImagePrecacheAssistant(AbstractImageLoader imageLoader, PrecacheInformationProvider precacheInformationProvider) {
		mImageLoader = imageLoader;
		mPrecacheInformationProvider = precacheInformationProvider;
	}

	public void onPositionVisited(int position) {
		boolean didDirectionSwap = calculateDirection(position);
		RangesToCache ranges = calculateRanges(position, didDirectionSwap);

		for (int i = ranges.memCacheLowerIndex; i < ranges.memCacheUpperIndex; i++) {
			List<PrecacheRequest> precacheRequests = mPrecacheInformationProvider.onRowPrecacheRequestsRequired(i);
			precacheListToMemory(precacheRequests);
		}

		for (int i = ranges.diskCacheLowerIndex; i < ranges.diskCacheUpperIndex; i++) {
			List<PrecacheRequest> precacheRequests = mPrecacheInformationProvider.onRowPrecacheRequestsRequired(i);
			precacheListToDisk(precacheRequests);
		}
	}

	public void setMemCacheRange(int range) {
		mMemCacheRange = range;
	}

	public void setDiskCacheRange(int range) {
		mDiskCacheRange = range;
	}

	private void precacheListToMemory(List<PrecacheRequest> precacheRequests) {
		for (PrecacheRequest precacheRequest : precacheRequests) {
			mImageLoader.precacheImageToDiskAndMemory(precacheRequest.mUri, precacheRequest.mBounds.width, precacheRequest.mBounds.height);
		}
	}

	private void precacheListToDisk(List<PrecacheRequest> precacheRequests) {
		for (PrecacheRequest precacheRequest : precacheRequests) {
			mImageLoader.precacheImageToDisk(precacheRequest.mUri);
		}
	}

	private RangesToCache calculateRanges(int position, boolean didDirectionSwap) {
		RangesToCache indices = new RangesToCache();

		switch (mCurrentDirection) {
		case UP:
			calculateRangesForUp(position, didDirectionSwap, indices);
			break;
		case DOWN:
			calculateRangesForDown(position, didDirectionSwap, indices);
			break;
		}

		return indices;
	}

	private boolean calculateDirection(int position) {
		boolean didDirectionSwap = false;
		if (directionSwitchedToDown(position)) {
			mCurrentDirection = Direction.DOWN;
			didDirectionSwap = true;
		} else if (directionSwitchedToUp(position)) {
			mCurrentDirection = Direction.UP;
			didDirectionSwap = true;
		}

		switch (mCurrentDirection) {
		case DOWN:
			mCurrentPosition = position;
			break;
		case UP:
			mCurrentPosition = position;
			break;
		}

		return didDirectionSwap;
	}

	private void calculateRangesForUp(int position, boolean didDirectionSwap, RangesToCache indices) {
		if (didDirectionSwap) {
			indices.memCacheUpperIndex = Math.max(0, position);
		} else {
			indices.memCacheUpperIndex = Math.max(0, position - mMemCacheRange + 1);
		}
		indices.memCacheLowerIndex = Math.max(0, position - mMemCacheRange);

		if (didDirectionSwap) {
			indices.diskCacheUpperIndex = Math.max(0, indices.memCacheLowerIndex);
		} else {
			indices.diskCacheUpperIndex = Math.max(0, indices.memCacheLowerIndex - mDiskCacheRange + 1);
		}
		indices.diskCacheLowerIndex = Math.max(0, indices.memCacheLowerIndex - mDiskCacheRange);
	}

	private void calculateRangesForDown(int position, boolean didDirectionSwap, RangesToCache indices) {
		int count = mPrecacheInformationProvider.getCount();

		if (didDirectionSwap) {
			indices.memCacheLowerIndex = Math.min(count, position + 1);
		} else {
			indices.memCacheLowerIndex = Math.min(count, position + mMemCacheRange);
		}
		indices.memCacheUpperIndex = Math.min(count, position + 1 + mMemCacheRange);

		if (didDirectionSwap) {
			indices.diskCacheLowerIndex = Math.min(count, indices.memCacheUpperIndex);
		} else {
			indices.diskCacheLowerIndex = Math.min(count, indices.memCacheUpperIndex + mDiskCacheRange - 1);
		}
		indices.diskCacheUpperIndex = Math.min(count, indices.memCacheUpperIndex + mDiskCacheRange);
	}

	private boolean directionSwitchedToDown(int position) {
		return mCurrentDirection == Direction.UP && position >= mCurrentPosition;
	}

	private boolean directionSwitchedToUp(int position) {
		return mCurrentDirection == Direction.DOWN && position <= mCurrentPosition;
	}

	public static interface PrecacheInformationProvider {
		public int getCount();

		public List<PrecacheRequest> onRowPrecacheRequestsRequired(int position);
	}

	public static class PrecacheRequest {
		private final String mUri;
		private final Dimensions mBounds;

		public PrecacheRequest(String uri, Dimensions bounds) {
			mUri = uri;
			mBounds = bounds;
		}
	}

	private static class RangesToCache {
		int memCacheLowerIndex = 0, memCacheUpperIndex = 0;
		int diskCacheLowerIndex = 0, diskCacheUpperIndex = 0;
	}
}
