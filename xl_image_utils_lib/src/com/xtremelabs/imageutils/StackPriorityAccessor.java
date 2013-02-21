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

class StackPriorityAccessor implements PriorityAccessor {
	private final HashedStack<Prioritizable> mStack = new HashedStack<Prioritizable>();

	@Override
	public synchronized Prioritizable detachHighestPriorityItem() {
		return mStack.pop();
	}

	@Override
	public synchronized void attach(Prioritizable prioritizable) {
		mStack.push(prioritizable);
	}

	@Override
	public synchronized int size() {
		return mStack.size();
	}

	@Override
	public Prioritizable peek() {
		return mStack.peek();
	}

	@Override
	public void clear() {
		mStack.clear();
	}

	@Override
	public void swap(CacheKey cacheKey, PriorityAccessor priorityAccessor, PriorityAccessor priorityAccessor2) {
	}
}
