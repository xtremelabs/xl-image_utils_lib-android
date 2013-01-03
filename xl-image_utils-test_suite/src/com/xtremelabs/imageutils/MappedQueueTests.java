package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class MappedQueueTests extends AndroidTestCase {
	private MappedQueue<String, String> mMappedQueue;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mMappedQueue = new MappedQueue<String, String>(4);
	}

	public void testBasics() {
		mMappedQueue.addOrBump("1", "one");
		assertEquals("one", mMappedQueue.getValue("1"));
	}

	public void testMappedQueue() {
		mMappedQueue.addOrBump("1", "one");
		mMappedQueue.addOrBump("2", "two");
		mMappedQueue.addOrBump("3", "three");
		mMappedQueue.addOrBump("4", "four");

		assertEquals("one", mMappedQueue.getValue("1"));
		assertEquals("two", mMappedQueue.getValue("2"));
		assertEquals("three", mMappedQueue.getValue("3"));
		assertEquals("four", mMappedQueue.getValue("4"));

		mMappedQueue.addOrBump("5", "five");

		assertEquals("two", mMappedQueue.getValue("2"));
		assertEquals("three", mMappedQueue.getValue("3"));
		assertEquals("four", mMappedQueue.getValue("4"));
		assertEquals("five", mMappedQueue.getValue("5"));
		assertNull(mMappedQueue.getValue("1"));

		mMappedQueue.getValue("2");
		mMappedQueue.addOrBump("6", "six");
		assertNull(mMappedQueue.getValue("3"));

		mMappedQueue.addOrBump("3", "three");
		mMappedQueue.addOrBump("7", "seven");
		assertNull(mMappedQueue.getValue("4"));
	}

	public void testContains() {
		assertFalse(mMappedQueue.contains("1"));
		mMappedQueue.addOrBump("1", "one");
		assertTrue(mMappedQueue.contains("1"));
	}
}
