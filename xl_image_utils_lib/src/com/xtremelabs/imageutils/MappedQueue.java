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

import java.util.HashMap;
import java.util.Map;

class MappedQueue<KEY, VALUE> {
	private final int mMaxSize;
	private Node mHead;
	private Node mTail;
	private final Map<KEY, Node> mMap = new HashMap<KEY, Node>();
	private int mSize = 0;

	public MappedQueue(final int maxSize) {
		mMaxSize = maxSize;
	}

	public synchronized void addOrBump(final KEY key, final VALUE value) {
		if (mMap.containsKey(key)) {
			bump(key);
		} else {
			if (mSize == mMaxSize) {
				removeTail();
			} else {
				mSize++;
			}
			final Node node = new Node(key, value);
			addToHead(node);
			mMap.put(key, node);
		}
	}

	public synchronized VALUE getValue(final KEY key) {
		final Node node = mMap.get(key);
		if (node == null) {
			return null;
		}

		final VALUE value = node.mValue;
		if (value != null) {
			bump(key);
		}
		return value;
	}

	public synchronized boolean contains(KEY key) {
		return mMap.containsKey(key);
	}

	public synchronized void remove(KEY key) {
		Node node = mMap.remove(key);
		removeFromList(node);
	}

	private void bump(final KEY key) {
		final Node temp = mMap.get(key);
		removeFromList(temp);
		addToHead(temp);
	}

	private void removeTail() {
		mMap.remove(mTail.mKey);
		if (mTail == mHead) {
			mHead = null;
			mTail = null;
		} else {
			mTail = mTail.mPrevious;
			mTail.mNext = null;
		}
	}

	private void addToHead(final Node node) {
		if (mHead == null) {
			mHead = node;
			mTail = node;
		} else {
			node.mNext = mHead;
			mHead.mPrevious = node;
			mHead = node;
		}
	}

	private void removeFromList(final Node node) {
		final Node next = node.mNext;
		final Node previous = node.mPrevious;

		if (previous != null) {
			previous.mNext = next;
		} else {
			mHead = next;
		}

		if (next != null) {
			next.mPrevious = previous;
		} else {
			mTail = previous;
		}

		node.mNext = null;
		node.mPrevious = null;
	}

	private class Node {
		private Node mPrevious;
		private Node mNext;
		private final KEY mKey;
		private final VALUE mValue;

		Node(final KEY key, final VALUE value) {
			mKey = key;
			mValue = value;
		}
	}
}