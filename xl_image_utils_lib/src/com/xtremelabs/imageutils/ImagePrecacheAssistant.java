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

	private int mLowIndex = 0;
	private int mHighIndex = 0;
	private Direction mCurrentDirection = Direction.UP;

	public ImagePrecacheAssistant(AbstractImageLoader imageLoader, PrecacheInformationProvider precacheInformationProvider) {
		mImageLoader = imageLoader;
		mPrecacheInformationProvider = precacheInformationProvider;
	}

	public void onPositionVisited(int position) {
		calculateDirection(position);
		RangesToCache ranges = calculateRanges(position);

		for (int i = ranges.memCacheLowerIndex; i < ranges.memCacheUpperIndex; i++) {
			List<PrecacheRequest> precacheRequests = mPrecacheInformationProvider.onRowPrecacheRequestsRequired(position);
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

	private RangesToCache calculateRanges(int position) {
		RangesToCache indices = new RangesToCache();

		switch (mCurrentDirection) {
		case UP:
			indices.memCacheUpperIndex = Math.max(-1, position - 1);
			indices.memCacheLowerIndex = Math.max(-1, position - 1 - mMemCacheRange);

			indices.diskCacheUpperIndex = Math.max(-1, indices.memCacheLowerIndex);
			indices.diskCacheLowerIndex = Math.max(-1, position - 1 - mDiskCacheRange);
			break;
		case DOWN:
			int count = mPrecacheInformationProvider.getCount();

			indices.memCacheLowerIndex = Math.min(count, position + 1);
			indices.memCacheUpperIndex = Math.min(count, position + 1 + mMemCacheRange);

			indices.diskCacheLowerIndex = Math.min(count, indices.memCacheUpperIndex);
			indices.diskCacheUpperIndex = Math.min(count, position + 1 + mDiskCacheRange);
			break;
		}

		return indices;
	}

	private void calculateDirection(int position) {
		if (directionSwitchedToDown(position)) {
			mCurrentDirection = Direction.DOWN;
		} else if (directionSwitchedToUp(position)) {
			mCurrentDirection = Direction.UP;
		}

		switch (mCurrentDirection) {
		case DOWN:
			mHighIndex = position;
			break;
		case UP:
			mLowIndex = position;
			break;
		}
	}

	private boolean directionSwitchedToDown(int position) {
		return mCurrentDirection == Direction.UP && position >= mLowIndex;
	}

	private boolean directionSwitchedToUp(int position) {
		return mCurrentDirection == Direction.DOWN && position <= mHighIndex;
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
