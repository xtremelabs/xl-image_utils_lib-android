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

	public void testBounds() {
		mMappedQueue = new MappedQueue<String, String>(3);
		mMappedQueue.addOrBump("k1", "v1");
		mMappedQueue.addOrBump("k2", "v2");
		mMappedQueue.addOrBump("k3", "v3");

		assertNotNull(mMappedQueue.getValue("k1"));
		assertNotNull(mMappedQueue.getValue("k2"));
		assertNotNull(mMappedQueue.getValue("k3"));

		mMappedQueue.addOrBump("k4", "v4");

		assertNull(mMappedQueue.getValue("k1"));

		// Should now have 4 -> 3 -> 2

		assertNotNull(mMappedQueue.getValue("k2"));

		// Should now have 2 -> 4 -> 3

		mMappedQueue.getValue("k4");

		// Should now have 4 -> 2 -> 3

		assertNotNull(mMappedQueue.getValue("k3"));
		assertNotNull(mMappedQueue.getValue("k2"));
		assertNotNull(mMappedQueue.getValue("k4"));
		assertNotNull(mMappedQueue.getValue("k4"));
		assertNotNull(mMappedQueue.getValue("k3"));
		assertNotNull(mMappedQueue.getValue("k3"));
		assertNotNull(mMappedQueue.getValue("k2"));
		assertNotNull(mMappedQueue.getValue("k4"));
	}

	public void testDuplicateInitialEntry() {
		mMappedQueue.addOrBump("k1", "v1");
		mMappedQueue.addOrBump("k1", "v1");
	}

	public void testDuplicateKeyButNotValue() {
		mMappedQueue.addOrBump("k1", "v1");
		mMappedQueue.addOrBump("k1", "v2");
	}
}
