package com.xtremelabs.imageutils;

import java.util.HashMap;
import java.util.Map;

class MappedQueue<KEY, VALUE> {
	private final int mMaxSize;
	private Node mHead;
	private Node mTail;
	private Map<KEY, Node> mMap = new HashMap<KEY, Node>();
	private int mSize = 0;

	public MappedQueue(int maxSize) {
		mMaxSize = maxSize;
	}

	public synchronized void addOrBump(KEY key, VALUE value) {
		if (mMap.containsKey(key)) {
			bump(key);
		} else {
			if (mSize == mMaxSize) {
				removeTail();
			} else {
				mSize++;
			}
			addToHead(new Node(key, value));
		}
	}
	
	public synchronized VALUE getValue(KEY key) {
		VALUE value = mMap.get(key).mValue;
		if (value != null) {
			bump(key);
		}
		return value;
	}
	
	private void bump(KEY key) {
		Node temp = mMap.get(key);
		removeFromList(temp);
		addToHead(temp);
	}
	
	private void removeTail() {
		mMap.remove(mTail.mKey);
		if (mTail == mHead) {
			mHead = null;
		}
		mTail = mTail.mPrevious;
	}

	private void addToHead(Node node) {
		if (mHead == null) {
			mHead = node;
			mTail = node;
		} else {
			node.mNext = mHead;
			mHead.mPrevious = node;
			mHead = node;
		}
	}

	private void removeFromList(Node node) {
		Node next = node.mNext;
		Node previous = node.mPrevious;

		if (previous != null) {
			previous.mNext = next;
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
		private KEY mKey;
		private VALUE mValue;

		Node(KEY key, VALUE value) {
			mKey = key;
			mValue = value;
		}
	}

}
