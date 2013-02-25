/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
		return QueueIndexTranslator.translateToIndex(mCacheRequest.getImageRequestType());
	}

	@Override
	public final Request<?> getRequest() {
		return mRequest;
	}

	public final CacheRequest getCacheRequest() {
		return mCacheRequest;
	}
}
