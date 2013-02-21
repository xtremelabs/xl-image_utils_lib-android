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

class QueuePriorityAccessor implements PriorityAccessor {
	private final HashedQueue<Prioritizable> mQueue = new HashedQueue<Prioritizable>();

	@Override
	public void attach(Prioritizable prioritizable) {
		mQueue.add(prioritizable);
	}

	@Override
	public Prioritizable detachHighestPriorityItem() {
		return mQueue.poll();
	}

	@Override
	public int size() {
		return mQueue.size();
	}

	@Override
	public Prioritizable peek() {
		return mQueue.peek();
	}

	@Override
	public void clear() {
		mQueue.clear();
	}

	@Override
	public void swap(CacheKey cacheKey, PriorityAccessor priorityAccessor, PriorityAccessor priorityAccessor2) {
	}
}
