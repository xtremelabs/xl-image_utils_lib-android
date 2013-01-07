package com.xtremelabs.imageutils;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.testactivity.MainActivity;

public class HashedQueueTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private HashedQueue<String> queue;
	
	public HashedQueueTests() {
		super(MainActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		queue = new HashedQueue<String>();
	}
	
	public void testEnqueueDequeue() {
		String url1 = "url1";
		String url2 = "url2";
		String url3 = "url3";
		
		queue.add(url1);
		queue.add(url2);
		queue.add(url3);
		
		assertEquals(3, queue.size());
		
		assertTrue(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertTrue(queue.contains(url3));
		assertFalse(queue.contains("aeoirhflaiuhwrgliahvipaw"));
		
		String retval;
		
		retval = queue.remove();
		assertEquals(url1, retval);
		
		retval = queue.remove();
		assertEquals(url2, retval);
		
		retval = queue.remove();
		assertEquals(url3, retval);
		
		assertEquals(0, queue.size());
	}
	
	public void testBump() {
		String url1 = "url1";
		String url2 = "url2";
		String url3 = "url3";
		
		queue.add(url1);
		queue.add(url2);
		queue.add(url3);
		
		queue.bump(url1);
		assertEquals(url2, queue.remove());
		queue.bump(url3);
		assertEquals(url1, queue.remove());
		assertEquals(url3, queue.remove());
		assertEquals(0, queue.size());
	}
	
	public void testRemove() {
		String url1 = "url1";
		String url2 = "url2";
		String url3 = "url3";
		String url4 = "url4";
		String url5 = "url5";
		String url6 = "url6";
		
		queue.add(url1);
		queue.add(url2);
		queue.add(url3);
		queue.add(url4);
		queue.add(url5);
		queue.add(url6);
		
		queue.remove(url1);
		
		assertFalse(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertTrue(queue.contains(url3));
		assertTrue(queue.contains(url4));
		assertTrue(queue.contains(url5));
		assertTrue(queue.contains(url6));
		
		queue.remove(url6);
		
		assertFalse(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertTrue(queue.contains(url3));
		assertTrue(queue.contains(url4));
		assertTrue(queue.contains(url5));
		assertFalse(queue.contains(url6));
		
		queue.remove(url3);
		
		assertFalse(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertFalse(queue.contains(url3));
		assertTrue(queue.contains(url4));
		assertTrue(queue.contains(url5));
		assertFalse(queue.contains(url6));
		
		queue.remove();
		
		assertFalse(queue.contains(url1));
		assertFalse(queue.contains(url2));
		assertFalse(queue.contains(url3));
		assertTrue(queue.contains(url4));
		assertTrue(queue.contains(url5));
		assertFalse(queue.contains(url6));
	}
	
	public void testPoll() {
		String url1 = "url1";
		String url2 = "url2";
		String url3 = "url3";
		String url4 = "url4";
		String url5 = "url5";
		String url6 = "url6";
		
		queue.add(url1);
		queue.add(url2);
		queue.add(url3);
		queue.add(url4);
		queue.add(url5);
		queue.add(url6);
		
		assertEquals(url1, queue.poll());
		
		assertFalse(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertTrue(queue.contains(url3));
		assertTrue(queue.contains(url4));
		assertTrue(queue.contains(url5));
		assertTrue(queue.contains(url6));
		
		queue.remove(url2);
		assertEquals(url3, queue.poll());
		
		assertFalse(queue.contains(url1));
		assertFalse(queue.contains(url2));
		assertFalse(queue.contains(url3));
		assertTrue(queue.contains(url4));
		assertTrue(queue.contains(url5));
		assertTrue(queue.contains(url6));
		
		assertEquals(url4, queue.poll());
		assertEquals(url5, queue.poll());
		assertEquals(url6, queue.poll());
		
		assertFalse(queue.contains(url1));
		assertFalse(queue.contains(url2));
		assertFalse(queue.contains(url3));
		assertFalse(queue.contains(url4));
		assertFalse(queue.contains(url5));
		assertFalse(queue.contains(url6));
	}
	
	public void testPeek() {
		String url1 = "url1";
		String url2 = "url2";
		String url3 = "url3";
		
		queue.add(url1);
		queue.add(url2);
		queue.add(url3);
		
		assertEquals(url1, queue.peek());
		
		assertTrue(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertTrue(queue.contains(url3));
		
		queue.poll();
		assertEquals(url2, queue.peek());
		assertFalse(queue.contains(url1));
		assertTrue(queue.contains(url2));
		assertTrue(queue.contains(url3));
		
		queue.poll();
		assertEquals(url3, queue.peek());
		assertFalse(queue.contains(url1));
		assertFalse(queue.contains(url2));
		assertTrue(queue.contains(url3));
	}
	
	public void testEnqueueWithDuplicates() {
		String url1 = "url1";
		String url2 = "url2";
		String url3 = "url3";
		
		queue.add(url1);
		queue.add(url2);
		queue.add(url3);
		
		queue.add(url1);
		assertEquals(url2, queue.poll());
		assertEquals(url3, queue.poll());
		assertEquals(url1, queue.poll());
	}
}
