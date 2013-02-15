package com.xtremelabs.imageutils;

abstract class DefaultPrioritizable extends Prioritizable {

	protected final CacheRequest mCacheRequest;
	private final Request<?> mRequest;

	public DefaultPrioritizable(CacheRequest cacheRequest, Request<?> request) {
		mCacheRequest = cacheRequest;
		mRequest = request;
	}

	@Override
	public final int getTargetPriorityAccessorIndex() {
		return QueueIndexTranslator.translateToIndex(mCacheRequest.getRequestType());
	}

	@Override
	public final Request<?> getRequest() {
		return mRequest;
	}

	public final CacheRequest getCacheRequest() {
		return mCacheRequest;
	}

	public final void forceAdapterCacheToMemory() {
		mCacheRequest.setRequestType(ImageRequestType.PRECACHE_TO_MEMORY_FOR_ADAPTER);
	}

	public final void forceAdapterCacheToDisk() {
		mCacheRequest.setRequestType(ImageRequestType.PRECACHE_TO_DISK_FOR_ADAPTER);
	}
}
