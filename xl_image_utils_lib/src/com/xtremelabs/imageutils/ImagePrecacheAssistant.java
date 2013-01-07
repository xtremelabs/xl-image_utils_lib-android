package com.xtremelabs.imageutils;

import java.util.List;

import android.widget.BaseAdapter;

/**
 * This utility simplifies the process of implementing precaching in adapters for use in widgets such as ListViews and ViewPagers.<br>
 * <br>
 * <b>Instructions:</b><br>
 * Create a new instance of this class from within adapter.<br>
 * Implement the methods {@link PrecacheInformationProvider#onRowPrecacheRequestsRequired(int)} and {@link PrecacheInformationProvider#getCount()}.<br>
 * In the "getView" method of the adapter, call {@link #onPositionVisited(int)}, and pass in the current position.
 */
/*
 * TODO This class still has a couple of inefficiencies. Namely, there are some duplicate calls being made. Find the duplicate calls and code them away.
 */
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

	/**
	 * This method must be called in the getView method of your adapter.
	 * 
	 * @param position
	 *            The current position within the adapter.
	 */
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

	/**
	 * Adjust the number of positions ahead that become cached in both the disk and memory caches.
	 * 
	 * @param range
	 */
	public void setMemCacheRange(int range) {
		mMemCacheRange = range;
	}

	/**
	 * Adjust the number of positions ahead of those that become cached in memory that will be cached on disk.
	 * 
	 * @param range
	 */
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

	/**
	 * This interface must be implemented in order for the {@link ImagePrecacheAssistant} to function.
	 */
	public static interface PrecacheInformationProvider {
		/**
		 * This method must return the number of elements in the adapter. In most cases, it should return the same value as the {@link BaseAdapter#getCount()} method.
		 * 
		 * @return
		 */
		public int getCount();

		/**
		 * This method returns a list of the URIs that are required for a particular position in the adapter.<br>
		 * <br>
		 * If there are no image URIs available for the provided position, an empty list should be returned.
		 * 
		 * @param position
		 *            The position for which images will be precached.
		 * @return A list of {@link PrecacheRequest}s. Each PrecacheRequest should contain a URI for a particular image and the bounds of the view the image will be loaded into. The bounds should be provided in pixels,
		 *         or be given as null.
		 */
		public List<PrecacheRequest> onRowPrecacheRequestsRequired(int position);
	}

	public static class PrecacheRequest {
		private final String mUri;
		private final Dimensions mBounds;

		/**
		 * @param uri
		 * @param bounds
		 *            The dimensions of the image view the URI will be loaded into. If one or more dimensions are unknown, simply specify the dimensions as null.
		 */
		public PrecacheRequest(String uri, Dimensions bounds) {
			mUri = uri;
			if (bounds == null) {
				mBounds = new Dimensions(null, null);
			} else {
				mBounds = bounds;
			}
		}
	}

	private static class RangesToCache {
		int memCacheLowerIndex = 0, memCacheUpperIndex = 0;
		int diskCacheLowerIndex = 0, diskCacheUpperIndex = 0;
	}
}
